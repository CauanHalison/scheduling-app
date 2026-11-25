package com.github.cauanHalison.scheduling_api.eventtype.dto;

import java.time.Instant;
import java.util.UUID;

public record EventTypeResponse(
        UUID id,
        String title,
        String slug,
        String eventLink,
        String description,
        Integer durationMinutes,
        Integer bufferAfterMinutes,
        Boolean isActive,
        Instant createdAt
) {
}
