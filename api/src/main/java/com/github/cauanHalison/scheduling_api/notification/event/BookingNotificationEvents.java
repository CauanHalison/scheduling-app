package com.github.cauanHalison.scheduling_api.notification.event;

public class BookingNotificationEvents {
    public record BookingCreatedEvent(BookingSnapshot booking) {}
    public record BookingCanceledEvent(BookingSnapshot booking) {}
    public record MagicLinkRequestedEvent(String email, String token) {}
    public record BookingReminderEvent(BookingSnapshot booking) {}
}
