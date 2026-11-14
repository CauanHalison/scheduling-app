package com.github.cauanHalison.scheduling_api.bookings.dto;

import java.time.Instant;

public record SlotResponse(
        Instant startTime,
        Instant endTime
) {
}
