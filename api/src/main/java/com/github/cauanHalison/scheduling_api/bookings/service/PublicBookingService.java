package com.github.cauanHalison.scheduling_api.bookings.service;

import com.github.cauanHalison.scheduling_api.bookings.domain.BookingEntity;
import com.github.cauanHalison.scheduling_api.bookings.domain.enums.BookingStatus;
import com.github.cauanHalison.scheduling_api.bookings.dto.BookingConfirmationResponse;
import com.github.cauanHalison.scheduling_api.bookings.dto.CreateBookingRequest;
import com.github.cauanHalison.scheduling_api.bookings.dto.PublicEventDetailsResponse;
import com.github.cauanHalison.scheduling_api.bookings.mapper.BookingMapper;
import com.github.cauanHalison.scheduling_api.bookings.repository.BookingRepository;
import com.github.cauanHalison.scheduling_api.config.cache.CacheConfiguration;
import com.github.cauanHalison.scheduling_api.eventtype.domain.EventTypeEntity;
import com.github.cauanHalison.scheduling_api.eventtype.dto.EventTypeResponse;
import com.github.cauanHalison.scheduling_api.eventtype.mapper.EventTypeMapper;
import com.github.cauanHalison.scheduling_api.eventtype.repository.EventTypeRepository;
import com.github.cauanHalison.scheduling_api.exception.exceptions.ResourceConflictException;
import com.github.cauanHalison.scheduling_api.exception.exceptions.ResourceNotFoundException;
import com.github.cauanHalison.scheduling_api.notification.event.BookingNotificationEvents.*;
import com.github.cauanHalison.scheduling_api.notification.event.BookingSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PublicBookingService {

    private final EventTypeRepository eventTypeRepository;
    private final BookingRepository bookingRepository;
    private final SlotCalculationService slotCalculationService;
    private final BookingMapper bookingMapper;
    private final EventTypeMapper eventTypeMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public List<EventTypeResponse> getProviderActiveEvents(String userSlug) {
        log.debug("Fetching all active public events for provider: userSlug={}", userSlug);
        List<EventTypeEntity> events = eventTypeRepository.findAllByProviderSlugAndIsActiveTrueOrderByCreatedAtDesc(userSlug);
        log.info("Found {} active public events for userSlug={}", events.size(), userSlug);
        return events.stream().map(eventTypeMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PublicEventDetailsResponse getPublicEventDetails(String userSlug, String eventSlug) {
        log.debug("Fetching public event details: userSlug={}, eventSlug={}", userSlug, eventSlug);

        EventTypeEntity eventType = eventTypeRepository.findByProviderSlugAndSlugAndIsActiveTrue(userSlug, eventSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Evento não encontrado para este prestador."));

        log.info("Public event details retrieved: eventTypeId={}, providerId={}, eventSlug={}",
                eventType.getId(), eventType.getProvider().getId(), eventSlug);

        return bookingMapper.toPublicEventDetails(eventType);
    }

    @Transactional
    @CacheEvict(value = {CacheConfiguration.AVAILABLE_SLOTS_CACHE, CacheConfiguration.AVAILABLE_DAYS_CACHE}, allEntries = true)
    public BookingConfirmationResponse createBooking(String userSlug, String eventSlug, CreateBookingRequest request) {
        log.info("Attempting to create booking: userSlug={}, eventSlug={}, clientEmail={}, startTime={}",
                userSlug, eventSlug, request.clientEmail(), request.startTime());

        EventTypeEntity eventType = eventTypeRepository.findByProviderSlugAndSlugAndIsActiveTrue(userSlug, eventSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de evento não encontrado para este prestador ou inativo."));

        Instant startTime = request.startTime();
        boolean isAvailable = slotCalculationService.isSlotAvailable(eventType, startTime);
        if (!isAvailable) {
            log.warn("Booking creation rejected - slot unavailable: userSlug={}, eventSlug={}, startTime={}",
                    userSlug, eventSlug, startTime);
            throw new ResourceConflictException("O horário solicitado não está disponível para agendamento.");
        }

        Instant endTime = startTime.plus(eventType.getDurationMinutes(), ChronoUnit.MINUTES);
        Instant blockedUntil = endTime.plus(eventType.getBufferAfterMinutes(), ChronoUnit.MINUTES);

        BookingEntity booking = new BookingEntity();
        booking.setEventType(eventType);
        booking.setProvider(eventType.getProvider());
        booking.setClientName(request.clientName().trim());
        booking.setClientEmail(request.clientEmail().trim().toLowerCase());
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setBlockedUntil(blockedUntil);
        booking.setStatus(BookingStatus.CONFIRMED);

        BookingEntity savedBooking = bookingRepository.save(booking);
        eventPublisher.publishEvent(new BookingCreatedEvent(BookingSnapshot.from(savedBooking)));

        log.info("Booking created successfully: id={}, providerId={}, eventTypeId={}, cancelToken={}, startTime={}",
                savedBooking.getId(),
                savedBooking.getProvider().getId(),
                savedBooking.getEventType().getId(),
                savedBooking.getCancelToken(),
                savedBooking.getStartTime());

        return bookingMapper.toConfirmationResponse(savedBooking);
    }
}
