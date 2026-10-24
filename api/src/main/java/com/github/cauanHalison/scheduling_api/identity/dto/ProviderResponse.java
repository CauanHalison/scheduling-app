package com.github.cauanHalison.scheduling_api.identity.dto;

import java.util.UUID;

public record ProviderResponse(
        UUID id,
        String name,
        String email,
        String slug,
        String timeZone
) {
}
