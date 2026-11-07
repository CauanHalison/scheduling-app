package com.github.cauanHalison.scheduling_api.bookings.dto;

import jakarta.validation.constraints.*;

import java.time.Instant;

public record CreateBookingRequest(
        @NotBlank
        @Size(max = 255)
        String clientName,

        @NotBlank
        @Email
        @Size(max = 255)
        String clientEmail,

        @NotNull
        @Future
        Instant startTime
) {
}
