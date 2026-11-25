package com.github.cauanHalison.scheduling_api.bookings.scheduler;

import com.github.cauanHalison.scheduling_api.bookings.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingCompletionScheduler {

    private final BookingRepository bookingRepository;

    @Scheduled(
            initialDelayString = "${app.booking-completion.initial-delay-ms:60000}",
            fixedDelayString = "${app.booking-completion.interval-ms:900000}"
    )
    @Transactional
    public void completeExpiredBookings() {
        Instant now = Instant.now();
        int completedCount = bookingRepository.markExpiredBookingsAsCompleted(now);

        if (completedCount > 0) {
            log.info("Expired confirmed bookings marked as completed: count={}, cutoff={}", completedCount, now);
        }
    }
}
