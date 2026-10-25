package com.github.cauanHalison.scheduling_api.identity.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "O refresh token não pode ser vazio")
        String refreshToken
) {
}
