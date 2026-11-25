package com.github.cauanHalison.scheduling_api.bookings.service;

import com.github.cauanHalison.scheduling_api.bookings.domain.BookingEntity;
import com.github.cauanHalison.scheduling_api.bookings.domain.enums.BookingStatus;
import com.github.cauanHalison.scheduling_api.bookings.dto.CancelBookingRequest;
import com.github.cauanHalison.scheduling_api.bookings.dto.GuestBookingResponse;
import com.github.cauanHalison.scheduling_api.bookings.dto.MagicLinkRequest;
import com.github.cauanHalison.scheduling_api.bookings.mapper.BookingMapper;
import com.github.cauanHalison.scheduling_api.bookings.repository.BookingRepository;
import com.github.cauanHalison.scheduling_api.config.cache.CacheConfiguration;
import com.github.cauanHalison.scheduling_api.exception.exceptions.InvalidTokenException;
import com.github.cauanHalison.scheduling_api.exception.exceptions.ResourceConflictException;
import com.github.cauanHalison.scheduling_api.exception.exceptions.ResourceNotFoundException;
import com.github.cauanHalison.scheduling_api.notification.event.BookingNotificationEvents.*;
import com.github.cauanHalison.scheduling_api.notification.event.BookingSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GuestBookingService {

    private static final String MAGIC_LINK_PREFIX = "guest:magic-link:";
    private static final Duration MAGIC_LINK_TTL = Duration.ofMinutes(15);

    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;
    private final StringRedisTemplate redisTemplate;
    private final ApplicationEventPublisher eventPublisher;
    private final MagicLinkRateLimiter magicLinkRateLimiter;

    @Transactional(readOnly = true)
    public GuestBookingResponse getBookingByCancelToken(UUID cancelToken) {
        log.debug("Fetching booking by cancelToken: token={}", cancelToken);

        BookingEntity booking = bookingRepository.findByCancelToken(cancelToken)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado para o token informado."));

        log.info("Booking retrieved via cancelToken: id={}, providerId={}",
                booking.getId(), booking.getProvider().getId());

        return bookingMapper.toGuestResponse(booking);
    }

    @Transactional
    @CacheEvict(value = {CacheConfiguration.AVAILABLE_SLOTS_CACHE, CacheConfiguration.AVAILABLE_DAYS_CACHE}, allEntries = true)
    public GuestBookingResponse cancelBookingByToken(UUID cancelToken, CancelBookingRequest request) {
        log.info("Guest requesting cancellation via token: token={}", cancelToken);

        BookingEntity booking = bookingRepository.findByCancelToken(cancelToken)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado para o token informado."));

        if (booking.getStatus() == BookingStatus.CANCELED) {
            log.warn("Guest cancellation rejected - booking already canceled: id={}", booking.getId());
            throw new ResourceConflictException("Este agendamento já se encontra cancelado.");
        }

        if (booking.getStatus() == BookingStatus.COMPLETED) {
            log.warn("Guest cancellation rejected - booking already completed: id={}", booking.getId());
            throw new ResourceConflictException("Não é possível cancelar um agendamento já concluído.");
        }

        if (booking.getEndTime().isBefore(Instant.now())) {
            log.warn("Guest cancellation rejected - booking end time already passed: bookingId={}", booking.getId());
            throw new ResourceConflictException("não é possivel cancelar um agendamento que já terminou");
        }

        String reason = (request != null && request.reason() != null && !request.reason().isBlank())
                ? request.reason().trim()
                : "Cancelado pelo convidado";

        booking.setStatus(BookingStatus.CANCELED);
        booking.setCancellationReason(reason);

        eventPublisher.publishEvent(new BookingCanceledEvent(BookingSnapshot.from(booking)));

        log.info("Booking canceled by guest: id={}, providerId={}, reason='{}'",
                booking.getId(), booking.getProvider().getId(), reason);

        return bookingMapper.toGuestResponse(booking);
    }

    public void requestMagicLink(MagicLinkRequest request, String clientIp) {
        String normalizedEmail = request.email().trim().toLowerCase();

        magicLinkRateLimiter.check(normalizedEmail, clientIp);

        if (!bookingRepository.existsByClientEmailIgnoreCase(normalizedEmail)) {
            log.info("Magic link requested for email without bookings");
            return;
        }

        String magicToken = UUID.randomUUID().toString();

        redisTemplate.opsForValue().set(MAGIC_LINK_PREFIX + magicToken, normalizedEmail, MAGIC_LINK_TTL);

        eventPublisher.publishEvent(new MagicLinkRequestedEvent(normalizedEmail, magicToken));

        log.info("Magic link requested: ttlMinutes={}",
                MAGIC_LINK_TTL.toMinutes());
    }

    @Transactional(readOnly = true)
    public List<GuestBookingResponse> verifyMagicLink(String token) {
        if (token == null || token.isBlank()) {
            throw new InvalidTokenException("Token de acesso inválido ou ausente.");
        }

        String email = redisTemplate.opsForValue().get(MAGIC_LINK_PREFIX + token);
        if (email == null) {
            log.warn("Magic link verification failed - expired or invalid token");
            throw new InvalidTokenException("Link de acesso expirado ou inválido.");
        }

        log.info("Magic link verified successfully");
        return listBookingsByClientEmail(email);
    }

    @Transactional(readOnly = true)
    public List<GuestBookingResponse> listBookingsByClientEmail(String clientEmail) {
        log.debug("Listing bookings by client email");

        List<BookingEntity> bookings = bookingRepository.findAllByClientEmailIgnoreCaseOrderByStartTimeDesc(clientEmail);

        log.info("Bookings retrieved for guest email: count={}", bookings.size());

        return bookings.stream()
                .map(bookingMapper::toGuestResponse)
                .toList();
    }
}
