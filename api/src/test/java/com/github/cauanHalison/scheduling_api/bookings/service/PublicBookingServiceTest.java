package com.github.cauanHalison.scheduling_api.bookings.service;

import com.github.cauanHalison.scheduling_api.bookings.domain.BookingEntity;
import com.github.cauanHalison.scheduling_api.bookings.domain.enums.BookingStatus;
import com.github.cauanHalison.scheduling_api.bookings.dto.BookingConfirmationResponse;
import com.github.cauanHalison.scheduling_api.bookings.dto.CreateBookingRequest;
import com.github.cauanHalison.scheduling_api.bookings.dto.PublicEventDetailsResponse;
import com.github.cauanHalison.scheduling_api.bookings.mapper.BookingMapper;
import com.github.cauanHalison.scheduling_api.bookings.repository.BookingRepository;
import com.github.cauanHalison.scheduling_api.eventtype.domain.EventTypeEntity;
import com.github.cauanHalison.scheduling_api.eventtype.dto.PublicEventTypeResponse;
import com.github.cauanHalison.scheduling_api.eventtype.mapper.EventTypeMapper;
import com.github.cauanHalison.scheduling_api.eventtype.repository.EventTypeRepository;
import com.github.cauanHalison.scheduling_api.exception.exceptions.ResourceConflictException;
import com.github.cauanHalison.scheduling_api.identity.domain.ProviderEntity;
import com.github.cauanHalison.scheduling_api.notification.event.BookingNotificationEvents.*;
import com.github.cauanHalison.scheduling_api.notification.event.BookingSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PublicBookingServiceTest {

    @Mock
    private EventTypeRepository eventTypeRepository;

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private SlotCalculationService slotCalculationService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Spy
    private BookingMapper bookingMapper = new BookingMapper();

    @Spy
    private EventTypeMapper eventTypeMapper = new EventTypeMapper();

    @InjectMocks
    private PublicBookingService publicBookingService;

    private ProviderEntity provider;
    private EventTypeEntity eventType;

    @BeforeEach
    void setUp() {
        provider = new ProviderEntity();
        provider.setId(UUID.randomUUID());
        provider.setName("Provider Test");
        provider.setEmail("provider@test.com");
        provider.setSlug("provider-slug");
        provider.setTimeZone("America/Sao_Paulo");

        eventType = new EventTypeEntity();
        eventType.setId(UUID.randomUUID());
        eventType.setProvider(provider);
        eventType.setTitle("Mentoria");
        eventType.setSlug("mentoria");
        eventType.setEventLink("https://meet.google.com/mentoria-room");
        eventType.setDurationMinutes(45);
        eventType.setBufferAfterMinutes(15);
        eventType.setIsActive(true);
    }

    @Test
    @DisplayName("Should return provider active events showcase without exposing eventLink")
    void shouldReturnProviderActiveEvents() {
        when(eventTypeRepository.findAllByProviderSlugAndIsActiveTrueOrderByCreatedAtDesc("provider-slug"))
                .thenReturn(List.of(eventType));

        List<PublicEventTypeResponse> responses = publicBookingService.getProviderActiveEvents("provider-slug");

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().title()).isEqualTo("Mentoria");
        assertThat(responses.getFirst().slug()).isEqualTo("mentoria");
    }

    @Test
    @DisplayName("Should return public event details by user and event slugs")
    void shouldReturnPublicEventDetails() {
        when(eventTypeRepository.findByProviderSlugAndSlugAndIsActiveTrue("provider-slug", "mentoria"))
                .thenReturn(Optional.of(eventType));

        PublicEventDetailsResponse response = publicBookingService.getPublicEventDetails("provider-slug", "mentoria");

        assertThat(response).isNotNull();
        assertThat(response.providerName()).isEqualTo("Provider Test");
        assertThat(response.eventTitle()).isEqualTo("Mentoria");
        assertThat(response.durationMinutes()).isEqualTo(45);
    }

    @Test
    @DisplayName("Should create booking when requested slot is available")
    void shouldCreateBookingWhenSlotAvailable() {
        Instant startTime = Instant.now().plus(2, ChronoUnit.DAYS).truncatedTo(ChronoUnit.SECONDS);
        CreateBookingRequest request = new CreateBookingRequest("Client Name", "client@test.com", startTime);

        when(eventTypeRepository.findByProviderSlugAndSlugAndIsActiveTrue("provider-slug", "mentoria"))
                .thenReturn(Optional.of(eventType));
        when(slotCalculationService.isSlotAvailable(eventType, startTime)).thenReturn(true);
        when(bookingRepository.save(any(BookingEntity.class))).thenAnswer(invocation -> {
            BookingEntity b = invocation.getArgument(0);
            b.setId(UUID.randomUUID());
            b.setCancelToken(UUID.randomUUID());
            b.setCreatedAt(Instant.now());
            return b;
        });

        BookingConfirmationResponse response = publicBookingService.createBooking("provider-slug", "mentoria", request);

        ArgumentCaptor<BookingEntity> captor = ArgumentCaptor.forClass(BookingEntity.class);
        verify(bookingRepository).save(captor.capture());

        BookingEntity saved = captor.getValue();
        assertThat(saved.getClientName()).isEqualTo("Client Name");
        assertThat(saved.getClientEmail()).isEqualTo("client@test.com");
        assertThat(saved.getStartTime()).isEqualTo(startTime);
        assertThat(saved.getEndTime()).isEqualTo(startTime.plus(45, ChronoUnit.MINUTES));
        assertThat(saved.getBlockedUntil()).isEqualTo(startTime.plus(60, ChronoUnit.MINUTES)); // 45 + 15 buffer
        assertThat(saved.getStatus()).isEqualTo(BookingStatus.CONFIRMED);

        assertThat(response).isNotNull();
        assertThat(response.clientName()).isEqualTo("Client Name");
        assertThat(response.eventLink()).isEqualTo("https://meet.google.com/mentoria-room");
        assertThat(response.cancelToken()).isNotNull();

        ArgumentCaptor<BookingCreatedEvent> eventCaptor = ArgumentCaptor.forClass(BookingCreatedEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());

        BookingSnapshot snapshot = eventCaptor.getValue().booking();
        assertThat(snapshot.eventTitle()).isEqualTo("Mentoria");
        assertThat(snapshot.eventLink()).isEqualTo("https://meet.google.com/mentoria-room");
        assertThat(snapshot.clientName()).isEqualTo("Client Name");
        assertThat(snapshot.providerEmail()).isEqualTo("provider@test.com");
        assertThat(snapshot.cancelToken()).isEqualTo(response.cancelToken());
    }

    @Test
    @DisplayName("Should throw ResourceConflictException when requested slot is unavailable")
    void shouldThrowConflictWhenSlotUnavailable() {
        Instant startTime = Instant.now().plus(2, ChronoUnit.DAYS);
        CreateBookingRequest request = new CreateBookingRequest("Client", "client@test.com", startTime);

        when(eventTypeRepository.findByProviderSlugAndSlugAndIsActiveTrue("provider-slug", "mentoria"))
                .thenReturn(Optional.of(eventType));
        when(slotCalculationService.isSlotAvailable(eventType, startTime)).thenReturn(false);

        assertThatThrownBy(() -> publicBookingService.createBooking("provider-slug", "mentoria", request))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("O horário solicitado não está disponível para agendamento.");

        verify(bookingRepository, never()).save(any());
        verifyNoInteractions(eventPublisher);
    }
}
