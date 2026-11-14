package com.github.cauanHalison.scheduling_api.bookings.dto;

import com.github.cauanHalison.scheduling_api.bookings.domain.enums.BookingStatus;

import java.time.Instant;
import java.util.UUID;

public record GuestBookingResponse(
        UUID id,
        String eventTitle,
        String eventDescription,
        Integer durationMinutes,
        String providerName,
        String clientName,
        String clientEmail,
        Instant startTime,
        Instant endTime,
        BookingStatus status,
        String cancellationReason
) {
}
