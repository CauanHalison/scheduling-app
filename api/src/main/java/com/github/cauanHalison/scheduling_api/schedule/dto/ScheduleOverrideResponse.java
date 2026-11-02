package com.github.cauanHalison.scheduling_api.schedule.dto;

import java.time.Instant;
import java.util.UUID;

public record ScheduleOverrideResponse(
        UUID id,
        Instant startTime,
        Instant endTime,
        String reason,
        Instant createdAt
) {
}
