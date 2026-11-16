package com.github.cauanHalison.scheduling_api.notification.event;

import com.github.cauanHalison.scheduling_api.bookings.domain.BookingEntity;

import java.time.Instant;
import java.util.UUID;

public record BookingSnapshot(
        UUID bookingId,
        UUID cancelToken,
        String eventTitle,
        String eventDescription,
        String providerName,
        String providerEmail,
        String providerTimeZone,
        String clientName,
        String clientEmail,
        Instant startTime,
        Instant endTime,
        String cancellationReason
) {
    public static BookingSnapshot from(BookingEntity booking) {
        return new BookingSnapshot(
                booking.getId(),
                booking.getCancelToken(),
                booking.getEventType().getTitle(),
                booking.getEventType().getDescription(),
                booking.getProvider().getName(),
                booking.getProvider().getEmail(),
                booking.getProvider().getTimeZone(),
                booking.getClientName(),
                booking.getClientEmail(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getCancellationReason()
        );
    }
}
