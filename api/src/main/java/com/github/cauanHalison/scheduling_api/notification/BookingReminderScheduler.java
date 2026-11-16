package com.github.cauanHalison.scheduling_api.notification;

import com.github.cauanHalison.scheduling_api.bookings.domain.BookingEntity;
import com.github.cauanHalison.scheduling_api.bookings.repository.BookingRepository;
import com.github.cauanHalison.scheduling_api.notification.event.BookingNotificationEvents.*;
import com.github.cauanHalison.scheduling_api.notification.event.BookingSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingReminderScheduler {
    private static final Duration REMINDER_LOOKAHEAD = Duration.ofHours(24);
    private final BookingRepository bookingRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(initialDelayString = "${app.reminder.initial-delay-ms:60000}", fixedDelayString = "${app.reminder.interval-ms:900000}")
    @Transactional
    public void sendUpcomingBookingReminders() {
        Instant now = Instant.now();
        Instant windowEnd = now.plus(REMINDER_LOOKAHEAD);

        List<BookingEntity> pendingReminders = bookingRepository.findPendingReminders(now, windowEnd);
        if (pendingReminders.isEmpty()) {
            return;
        }

        for (BookingEntity booking : pendingReminders) {
            booking.setReminderSentAt(now);
            eventPublisher.publishEvent(new BookingReminderEvent(BookingSnapshot.from(booking)));
        }
        log.info("Booking reminders scheduled for dispatch: count={}, lookahead={}", pendingReminders.size(), REMINDER_LOOKAHEAD);
    }
}
