package com.github.cauanHalison.scheduling_api.notification;

import com.github.cauanHalison.scheduling_api.bookings.domain.BookingEntity;
import com.github.cauanHalison.scheduling_api.bookings.domain.enums.BookingStatus;
import com.github.cauanHalison.scheduling_api.bookings.repository.BookingRepository;
import com.github.cauanHalison.scheduling_api.eventtype.domain.EventTypeEntity;
import com.github.cauanHalison.scheduling_api.identity.domain.ProviderEntity;
import com.github.cauanHalison.scheduling_api.notification.event.BookingNotificationEvents.BookingReminderEvent;
import com.github.cauanHalison.scheduling_api.notification.event.BookingSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingReminderSchedulerTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private BookingReminderScheduler scheduler;

    private BookingEntity booking;

    @BeforeEach
    void setUp() {
        ProviderEntity provider = new ProviderEntity();
        provider.setId(UUID.randomUUID());
        provider.setName("Dr. Provider");
        provider.setEmail("provider@test.com");

        EventTypeEntity eventType = new EventTypeEntity();
        eventType.setId(UUID.randomUUID());
        eventType.setTitle("Mentoria");
        eventType.setDescription("Descrição");
        eventType.setProvider(provider);

        booking = new BookingEntity();
        booking.setId(UUID.randomUUID());
        booking.setCancelToken(UUID.randomUUID());
        booking.setProvider(provider);
        booking.setEventType(eventType);
        booking.setClientName("Maria");
        booking.setClientEmail("maria@test.com");
        booking.setStartTime(Instant.now().plus(2, ChronoUnit.HOURS));
        booking.setEndTime(Instant.now().plus(2, ChronoUnit.HOURS).plusSeconds(1800));
        booking.setStatus(BookingStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Should do nothing when there are no pending reminders")
    void shouldDoNothingWhenNoPendingReminders() {
        when(bookingRepository.findPendingReminders(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());

        scheduler.sendUpcomingBookingReminders();

        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("Should mark reminderSentAt and publish BookingReminderEvent for pending bookings")
    void shouldMarkReminderSentAndPublishEvent() {
        when(bookingRepository.findPendingReminders(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(booking));

        scheduler.sendUpcomingBookingReminders();

        assertThat(booking.getReminderSentAt()).isNotNull();

        ArgumentCaptor<BookingReminderEvent> captor = ArgumentCaptor.forClass(BookingReminderEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());

        BookingSnapshot snapshot = captor.getValue().booking();
        assertThat(snapshot.bookingId()).isEqualTo(booking.getId());
        assertThat(snapshot.cancelToken()).isEqualTo(booking.getCancelToken());
        assertThat(snapshot.clientEmail()).isEqualTo("maria@test.com");
        assertThat(snapshot.providerEmail()).isEqualTo("provider@test.com");
        assertThat(snapshot.eventTitle()).isEqualTo("Mentoria");
    }
}
