package com.github.cauanHalison.scheduling_api.identity.dto;

public record AuthResponse(
        String authToken,
        String refreshToken
) {
}
