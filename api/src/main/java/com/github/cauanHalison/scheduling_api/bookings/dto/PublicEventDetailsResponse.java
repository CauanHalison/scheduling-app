package com.github.cauanHalison.scheduling_api.bookings.dto;

public record PublicEventDetailsResponse(
        String providerName,
        String providerSlug,
        String providerTimeZone,
        String eventTitle,
        String eventSlug,
        String description,
        Integer durationMinutes
) {
}
