package com.github.cauanHalison.scheduling_api.notification.service;

import com.github.cauanHalison.scheduling_api.notification.event.BookingSnapshot;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CalendarInviteServiceTest {

    private final CalendarInviteService service = new CalendarInviteService();
    private BookingSnapshot snapshot;

    @BeforeEach
    void setUp() {
        snapshot = new BookingSnapshot(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Mentoria",
                "Descrição da mentoria",
                "https://meet.google.com/mentoria-123",
                "Provider Test",
                "provider@test.com",
                "America/Sao_Paulo",
                "Client",
                "client@test.com",
                Instant.parse("2026-12-01T13:00:00Z"),
                Instant.parse("2026-12-01T13:30:00Z"),
                null
        );
    }

    private String generate(boolean canceled) {
        return new String(service.generateInviteIcs(snapshot, canceled), StandardCharsets.UTF_8)
                .replace("\r\n ", "");
    }

    @Test
    @DisplayName("Should generate a REQUEST invite for a confirmed booking")
    void shouldGenerateRequestInvite() {
        assertThat(generate(false)).contains(
                "BEGIN:VCALENDAR",
                "METHOD:REQUEST",
                "STATUS:CONFIRMED",
                "SEQUENCE:0",
                "UID:" + snapshot.bookingId() + "@scheduling-api.com",
                "SUMMARY:Mentoria",
                "LOCATION:https://meet.google.com/mentoria-123",
                "mailto:provider@test.com",
                "mailto:client@test.com"
        );
    }

    @Test
    @DisplayName("Should generate a CANCEL invite for a canceled booking")
    void shouldGenerateCancelInvite() {
        assertThat(generate(true))
                .contains(
                        "METHOD:CANCEL",
                        "STATUS:CANCELLED",
                        "SEQUENCE:1",
                        "UID:" + snapshot.bookingId() + "@scheduling-api.com"
                )
                .doesNotContain("LOCATION:");
    }

    @Test
    @DisplayName("Should generate invite even when event description is null")
    void shouldHandleNullDescription() {
        snapshot = new BookingSnapshot(
                snapshot.bookingId(), snapshot.cancelToken(), "Mentoria", null, null,
                "Provider Test", "provider@test.com", "America/Sao_Paulo", "Client", "client@test.com",
                snapshot.startTime(), snapshot.endTime(), null
        );

        assertThat(generate(false)).contains("SUMMARY:Mentoria");
    }
}
