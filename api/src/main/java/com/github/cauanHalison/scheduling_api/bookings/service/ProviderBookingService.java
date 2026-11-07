package com.github.cauanHalison.scheduling_api.bookings.service;

import com.github.cauanHalison.scheduling_api.bookings.domain.BookingEntity;
import com.github.cauanHalison.scheduling_api.bookings.domain.enums.BookingStatus;
import com.github.cauanHalison.scheduling_api.bookings.dto.CancelBookingRequest;
import com.github.cauanHalison.scheduling_api.bookings.dto.ProviderBookingResponse;
import com.github.cauanHalison.scheduling_api.bookings.mapper.BookingMapper;
import com.github.cauanHalison.scheduling_api.bookings.repository.BookingRepository;
import com.github.cauanHalison.scheduling_api.bookings.repository.BookingSpecifications;
import com.github.cauanHalison.scheduling_api.config.cache.CacheConfiguration;
import com.github.cauanHalison.scheduling_api.exception.exceptions.ResourceConflictException;
import com.github.cauanHalison.scheduling_api.exception.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderBookingService {

    private final BookingRepository bookingRepository;
    private final BookingMapper bookingMapper;

    @Transactional(readOnly = true)
    public Page<ProviderBookingResponse> listBookings(
            UUID providerId,
            BookingStatus status,
            Instant startDate,
            Instant endDate,
            Pageable pageable
    ) {
        log.debug("Listing bookings for provider: providerId={}, status={}, startDate={}, endDate={}, page={}",
                providerId, status, startDate, endDate, pageable.getPageNumber());

        Specification<BookingEntity> spec = BookingSpecifications.withFilters(providerId, status, startDate, endDate);
        Page<BookingEntity> bookingsPage = bookingRepository.findAll(spec, pageable);

        log.info("Bookings listed for provider: providerId={}, totalElements={}, totalPages={}",
                providerId, bookingsPage.getTotalElements(), bookingsPage.getTotalPages());

        return bookingsPage.map(bookingMapper::toProviderResponse);
    }

    @Transactional(readOnly = true)
    public ProviderBookingResponse getBookingById(UUID providerId, UUID bookingId) {
        log.debug("Fetching booking by id: providerId={}, bookingId={}", providerId, bookingId);

        BookingEntity booking = bookingRepository.findByIdAndProviderId(bookingId, providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado."));

        log.info("Booking retrieved successfully: id={}, providerId={}", booking.getId(), providerId);

        return bookingMapper.toProviderResponse(booking);
    }

    @Transactional
    @CacheEvict(value = {CacheConfiguration.AVAILABLE_SLOTS_CACHE, CacheConfiguration.AVAILABLE_DAYS_CACHE}, allEntries = true)
    public ProviderBookingResponse cancelBooking(UUID providerId, UUID bookingId, CancelBookingRequest request) {
        log.info("Provider requesting booking cancellation: providerId={}, bookingId={}", providerId, bookingId);

        BookingEntity booking = bookingRepository.findByIdAndProviderId(bookingId, providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado."));

        if (booking.getStatus() == BookingStatus.CANCELED) {
            log.warn("Cancellation rejected - booking already canceled: bookingId={}, providerId={}",
                    bookingId, providerId);
            throw new ResourceConflictException("Este agendamento já se encontra cancelado.");
        }

        String reason = (request != null && request.reason() != null && !request.reason().isBlank())
                ? request.reason().trim()
                : "Cancelado pelo prestador";

        booking.setStatus(BookingStatus.CANCELED);
        booking.setCancellationReason(reason);

        log.info("Booking canceled by provider: id={}, providerId={}, reason='{}'",
                booking.getId(), providerId, reason);

        return bookingMapper.toProviderResponse(booking);
    }
}
