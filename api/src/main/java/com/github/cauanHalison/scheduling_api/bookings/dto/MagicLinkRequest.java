package com.github.cauanHalison.scheduling_api.bookings.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MagicLinkRequest(
        @NotBlank
        @Email
        @Size(max = 255)
        String email
) {
}
