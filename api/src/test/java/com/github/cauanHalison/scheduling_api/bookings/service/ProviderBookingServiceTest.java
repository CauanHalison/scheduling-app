package com.github.cauanHalison.scheduling_api.bookings.service;

import com.github.cauanHalison.scheduling_api.bookings.domain.BookingEntity;
import com.github.cauanHalison.scheduling_api.bookings.domain.enums.BookingStatus;
import com.github.cauanHalison.scheduling_api.bookings.dto.CancelBookingRequest;
import com.github.cauanHalison.scheduling_api.bookings.dto.ProviderBookingResponse;
import com.github.cauanHalison.scheduling_api.bookings.mapper.BookingMapper;
import com.github.cauanHalison.scheduling_api.bookings.repository.BookingRepository;
import com.github.cauanHalison.scheduling_api.eventtype.domain.EventTypeEntity;
import com.github.cauanHalison.scheduling_api.exception.exceptions.ResourceConflictException;
import com.github.cauanHalison.scheduling_api.exception.exceptions.ResourceNotFoundException;
import com.github.cauanHalison.scheduling_api.identity.domain.ProviderEntity;
import com.github.cauanHalison.scheduling_api.notification.event.BookingNotificationEvents;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProviderBookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Spy
    private BookingMapper bookingMapper = new BookingMapper();

    @InjectMocks
    private ProviderBookingService providerBookingService;

    private ProviderEntity provider;
    private EventTypeEntity eventType;
    private BookingEntity booking;

    @BeforeEach
    void setUp() {
        provider = new ProviderEntity();
        provider.setId(UUID.randomUUID());
        provider.setName("Provider Test");
        provider.setEmail("provider@test.com");

        eventType = new EventTypeEntity();
        eventType.setId(UUID.randomUUID());
        eventType.setTitle("Mentoria");
        eventType.setDurationMinutes(30);

        booking = new BookingEntity();
        booking.setId(UUID.randomUUID());
        booking.setProvider(provider);
        booking.setEventType(eventType);
        booking.setClientName("Client");
        booking.setClientEmail("client@test.com");
        booking.setStartTime(Instant.now());
        booking.setEndTime(Instant.now().plusSeconds(1800));
        booking.setStatus(BookingStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Should list bookings for provider with pagination")
    void shouldListBookingsForProvider() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<BookingEntity> page = new PageImpl<>(List.of(booking), pageable, 1);

        when(bookingRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        Page<ProviderBookingResponse> result = providerBookingService.listBookings(
                provider.getId(), null, null, null, pageable
        );

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().clientName()).isEqualTo("Client");
    }

    @Test
    @DisplayName("Should cancel booking with reason when provider requests")
    void shouldCancelBookingWhenRequested() {
        when(bookingRepository.findByIdAndProviderId(booking.getId(), provider.getId()))
                .thenReturn(Optional.of(booking));

        ProviderBookingResponse response = providerBookingService.cancelBooking(
                provider.getId(), booking.getId(), new CancelBookingRequest("Prestador doente")
        );

        ArgumentCaptor<BookingNotificationEvents.BookingCanceledEvent> captor = ArgumentCaptor.forClass(BookingNotificationEvents.BookingCanceledEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        BookingSnapshot snapshot = captor.getValue().booking();

        assertThat(snapshot.providerName()).isEqualTo("Provider Test");
        assertThat(snapshot.providerEmail()).isEqualTo("provider@test.com");
        assertThat(snapshot.cancellationReason()).isEqualTo("Prestador doente");
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.CANCELED);
        assertThat(booking.getCancellationReason()).isEqualTo("Prestador doente");
        assertThat(response.status()).isEqualTo(BookingStatus.CANCELED);

    }

    @Test
    @DisplayName("Should throw ResourceConflictException when canceling an already canceled booking")
    void shouldThrowWhenBookingAlreadyCanceled() {
        booking.setStatus(BookingStatus.CANCELED);

        when(bookingRepository.findByIdAndProviderId(booking.getId(), provider.getId()))
                .thenReturn(Optional.of(booking));

        assertThatThrownBy(() -> providerBookingService.cancelBooking(provider.getId(), booking.getId(), null))
                .isInstanceOf(ResourceConflictException.class)
                .hasMessage("Este agendamento já se encontra cancelado.");
        verifyNoInteractions(eventPublisher);
    }
}
