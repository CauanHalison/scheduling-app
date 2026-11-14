package com.github.cauanHalison.scheduling_api.bookings.dto;

import java.time.LocalDate;
import java.util.List;

public record AvailableSlotsResponse(
        LocalDate date,
        String timeZone,
        List<SlotResponse> slots
) {
}
