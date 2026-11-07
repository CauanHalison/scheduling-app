package com.github.cauanHalison.scheduling_api.bookings.dto;

import jakarta.validation.constraints.Size;

public record CancelBookingRequest(
        @Size(max = 500)
        String reason
) {

}
