package com.github.cauanHalison.scheduling_api.notification.listener;

import com.github.cauanHalison.scheduling_api.notification.event.BookingNotificationEvents.*;
import com.github.cauanHalison.scheduling_api.notification.event.BookingSnapshot;
import com.github.cauanHalison.scheduling_api.notification.service.CalendarInviteService;
import com.github.cauanHalison.scheduling_api.notification.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingNotificationListenerTest {
    @Mock
    private EmailService emailService;

    @Mock
    private CalendarInviteService calendarInviteService;

    @InjectMocks
    private BookingNotificationListener listener;

    private BookingSnapshot bookingSnapshot;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(listener, "frontendUrl", "http://localhost:3000");
        bookingSnapshot = new BookingSnapshot(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Event Test",
                "event description test",
                "https://meet.google.com/event-test",
                "Provider Test",
                "provider@test.com",
                "America/Sao_Paulo",
                "CLient test",
                "client@test.com",
                Instant.parse("2026-11-16T12:00:00Z"),
                Instant.parse("2026-11-16T12:30:00Z"),
                "Reason"
        );
    }

    @Test
    void shouldNotifyProviderAndClientOnCreation() {
        byte[] ics = new byte[]{1};

        when(calendarInviteService.generateInviteIcs(bookingSnapshot, false)).thenReturn(ics);

        listener.onBookingCreated(new BookingCreatedEvent(bookingSnapshot));

        verify(emailService).sendEmail(
                eq("client@test.com"),
                contains("Agendamento Confirmado"),
                contains("https://meet.google.com/event-test"),
                eq(ics)
        );
        verify(emailService).sendEmail(
                eq("provider@test.com"),
                contains("Novo Agendamento"),
                contains("https://meet.google.com/event-test"),
                eq(ics)
        );
    }

    @Test
    void shouldNotifyBothOnCancellationWithCancelInvite() {
        byte[] ics = new byte[]{2};
        when(calendarInviteService.generateInviteIcs(bookingSnapshot, true)).thenReturn(ics);

        listener.onBookingCanceled(new BookingCanceledEvent(bookingSnapshot));

        verify(emailService).sendEmail(
                eq("client@test.com"),
                contains("Cancelado"),
                contains("Motivo"), eq(ics));
        verify(emailService).sendEmail(
                eq("provider@test.com"),
                contains("Cancelado"),
                contains("Motivo"),
                eq(ics)
        );
    }

    @Test
    void shouldSendMagicLinkWithoutAttachment() {
        listener.onMagicLinkRequested(new MagicLinkRequestedEvent("user@example.com", "tok"));

        verify(emailService).sendEmail(
                eq("user@example.com"),
                anyString(),
                contains("http://localhost:3000/guest/magic-link?token=tok"),
                isNull());
    }

    @Test
    void shouldEscapeHtmlInUserControlledFields() {
        BookingSnapshot malicious = new BookingSnapshot(
                UUID.randomUUID(), UUID.randomUUID(), "Event Test", "desc", null,
                "Provider Test", "provider@test.com", "America/Sao_Paulo",
                "<script>alert(1)</script>", "client@test.com",
                Instant.now(), Instant.now().plusSeconds(1800),
                "<img src=x onerror=alert(1)>"
        );
        when(calendarInviteService.generateInviteIcs(malicious, true)).thenReturn(new byte[]{3});

        listener.onBookingCanceled(new BookingCanceledEvent(malicious));

        ArgumentCaptor<String> bodies = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce()).sendEmail(anyString(), anyString(), bodies.capture(), any());

        assertThat(bodies.getAllValues()).isNotEmpty().allSatisfy(body -> {
            assertThat(body).doesNotContain("<script>").doesNotContain("<img");
            assertThat(body).contains("&lt;");
        });
    }

    @Test
    void shouldUseFallbackReasonWhenCancellationReasonIsNull() {
        BookingSnapshot noReason = new BookingSnapshot(
                UUID.randomUUID(), UUID.randomUUID(), "Event Test", "desc", null,
                "Provider Test", "provider@test.com", "America/Sao_Paulo", "Client", "client@test.com",
                Instant.now(), Instant.now().plusSeconds(1800), null
        );
        when(calendarInviteService.generateInviteIcs(noReason, true)).thenReturn(new byte[]{4});

        listener.onBookingCanceled(new BookingCanceledEvent(noReason));

        ArgumentCaptor<String> bodies = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce()).sendEmail(anyString(), anyString(), bodies.capture(), any());
        assertThat(bodies.getAllValues()).allSatisfy(body -> assertThat(body).doesNotContain("null"));
    }

    @Test
    void shouldSendReminderEmailToClientWithoutAttachment() {
        listener.onBookingReminder(new BookingReminderEvent(bookingSnapshot));

        verify(emailService).sendEmail(
                eq("client@test.com"),
                contains("Lembrete de agendamento"),
                contains("https://meet.google.com/event-test"),
                isNull()
        );
    }

    @Test
    void shouldFallbackToUtcWhenProviderTimeZoneIsMissingOrInvalid() {
        BookingSnapshot noZone = new BookingSnapshot(
                UUID.randomUUID(), UUID.randomUUID(), "Event Test", "desc", null,
                "Provider Test", "provider@test.com", "Invalid/Zone", "Client", "client@test.com",
                Instant.parse("2026-11-16T12:00:00Z"), Instant.parse("2026-11-16T12:30:00Z"), null
        );

        listener.onBookingReminder(new BookingReminderEvent(noZone));

        verify(emailService).sendEmail(
                eq("client@test.com"),
                contains("Lembrete de agendamento"),
                contains("16/11/2026 das 12:00 às 12:30 (UTC)"),
                isNull()
        );
    }
}
