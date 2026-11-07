package com.github.cauanHalison.scheduling_api.bookings.dto;

import java.time.LocalDate;
import java.util.List;

public record AvailableDaysResponse(
        List<LocalDate> availableDays
) {
}
