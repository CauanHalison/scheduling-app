package com.github.cauanHalison.scheduling_api.bookings.scheduler;

import com.github.cauanHalison.scheduling_api.bookings.repository.BookingRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingCompletionSchedulerTest {

    @Mock
    private BookingRepository bookingRepository;

    @InjectMocks
    private BookingCompletionScheduler scheduler;

    @Test
    @DisplayName("Should call markExpiredBookingsAsCompleted with current timestamp when expired bookings exist")
    void shouldMarkExpiredBookingsAsCompleted() {
        Instant before = Instant.now();
        when(bookingRepository.markExpiredBookingsAsCompleted(any(Instant.class))).thenReturn(2);

        scheduler.completeExpiredBookings();

        Instant after = Instant.now();
        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(bookingRepository).markExpiredBookingsAsCompleted(cutoffCaptor.capture());

        assertThat(cutoffCaptor.getValue()).isBetween(before, after);
    }

    @Test
    @DisplayName("Should complete without errors when no expired confirmed bookings exist")
    void shouldHandleZeroUpdatedBookings() {
        when(bookingRepository.markExpiredBookingsAsCompleted(any(Instant.class))).thenReturn(0);

        scheduler.completeExpiredBookings();

        verify(bookingRepository).markExpiredBookingsAsCompleted(any(Instant.class));
    }
}
