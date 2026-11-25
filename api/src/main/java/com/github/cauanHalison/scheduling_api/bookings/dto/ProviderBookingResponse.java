package com.github.cauanHalison.scheduling_api.bookings.dto;

import com.github.cauanHalison.scheduling_api.bookings.domain.enums.BookingStatus;

import java.time.Instant;
import java.util.UUID;

public record ProviderBookingResponse(
        UUID id,
        UUID eventTypeId,
        String eventTitle,
        String eventLink,
        Integer durationMinutes,
        String clientName,
        String clientEmail,
        Instant startTime,
        Instant endTime,
        BookingStatus status,
        String cancellationReason,
        Instant createdAt
) {
}
