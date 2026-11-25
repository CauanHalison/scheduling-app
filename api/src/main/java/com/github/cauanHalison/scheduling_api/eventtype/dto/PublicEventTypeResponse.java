package com.github.cauanHalison.scheduling_api.eventtype.dto;

import java.util.UUID;

public record PublicEventTypeResponse(
        UUID id,
        String title,
        String slug,
        String description,
        Integer durationMinutes,
        Integer bufferAfterMinutes
) {
}
