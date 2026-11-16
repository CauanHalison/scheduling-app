package com.github.cauanHalison.scheduling_api.notification.listener;

import com.github.cauanHalison.scheduling_api.notification.event.BookingNotificationEvents.*;
import com.github.cauanHalison.scheduling_api.notification.event.BookingSnapshot;
import com.github.cauanHalison.scheduling_api.notification.service.CalendarInviteService;
import com.github.cauanHalison.scheduling_api.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.HtmlUtils;

import java.time.ZoneId;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingNotificationListener {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy", PT_BR);
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm", PT_BR);

    private final EmailService emailService;
    private final CalendarInviteService calendarInviteService;

    @Value("${app.frontend.url}")
    private String frontendUrl;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingCreated(BookingCreatedEvent event) {
        BookingSnapshot booking = event.booking();

        byte[] icsBytes = calendarInviteService.generateInviteIcs(booking, false);
        String clientName = escape(booking.clientName());
        String clientEmail = escape(booking.clientEmail());
        String eventTitle = escape(booking.eventTitle());
        String providerName = escape(booking.providerName());
        String period = formatPeriod(booking);

        String cancelUrl = frontendUrl + "/guest/bookings/" + booking.cancelToken();

        emailService.sendEmail(
                booking.clientEmail(),
                "Agendamento Confirmado: " + booking.eventTitle(),
                """
                <h2>Olá, %s!</h2>
                <p>Seu agendamento para <strong>%s</strong> com <strong>%s</strong> foi confirmado.</p>
                <p><strong>Quando:</strong> %s</p>
                <p>O convite para adicionar à sua agenda está em anexo (<code>invite.ics</code>).</p>
                <p>Precisa cancelar ou visualizar mais detalhes? <a href="%s">Clique aqui para gerenciar seu agendamento</a>.</p>
                """.formatted(
                        clientName,
                        eventTitle,
                        providerName,
                        period,
                        cancelUrl
                ),
                icsBytes
        );

        emailService.sendEmail(
                booking.providerEmail(),
                "Novo Agendamento: " + booking.eventTitle() + " - " + booking.clientName(),
                """
                <h2>Novo agendamento recebido!</h2>
                <p><strong>Cliente:</strong> %s (%s)</p>
                <p><strong>Evento:</strong> %s</p>
                <p><strong>Quando:</strong> %s</p>
                """.formatted(
                        clientName,
                        clientEmail,
                        eventTitle,
                        period
                ),
                icsBytes
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingCanceled(BookingCanceledEvent event) {
        BookingSnapshot booking = event.booking();
        byte[] icsBytes = calendarInviteService.generateInviteIcs(booking, true);
        String clientName = escape(booking.clientName());
        String clientEmail = escape(booking.clientEmail());
        String eventTitle = escape(booking.eventTitle());
        String providerName = escape(booking.providerName());
        String period = formatPeriod(booking);
        String reason = escape(booking.cancellationReason() != null ? booking.cancellationReason() : "Não informado");

        emailService.sendEmail(
                booking.clientEmail(),
                "Agendamento Cancelado: " + booking.eventTitle(),
                """
                <h2>Agendamento Cancelado</h2>
                <p>O agendamento de <strong>%s</strong> com <strong>%s</strong> (%s) foi cancelado.</p>
                <p><strong>Motivo:</strong> %s</p>
                """.formatted(
                        eventTitle,
                        providerName,
                        period,
                        reason
                ),
                icsBytes
        );

        emailService.sendEmail(
                booking.providerEmail(),
                "Agendamento Cancelado: " + booking.eventTitle() + " - " + booking.clientName(),
                """
                <h2>Um agendamento foi cancelado</h2>
                <p><strong>Cliente:</strong> %s (%s)</p>
                <p><strong>Quando:</strong> %s</p>
                <p><strong>Motivo:</strong> %s</p>
                """.formatted(
                        clientName,
                        clientEmail,
                        period,
                        reason
                ),
                icsBytes
        );
    }

    @Async
    @EventListener
    public void onMagicLinkRequested(MagicLinkRequestedEvent event) {
        String magicLinkUrl = frontendUrl + "/guest/magic-link?token=" + event.token();
        emailService.sendEmail(
                event.email(),
                "Seu link de acesso aos agendamentos",
                """
                <h2>Acesso aos seus agendamentos</h2>
                <p>Clique no link abaixo para visualizar seus agendamentos (válido por 15 minutos):</p>
                <p><a href="%s">Acessar meus agendamentos</a></p>
                """.formatted(magicLinkUrl),
                null
        );
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBookingReminder(BookingReminderEvent event) {
        BookingSnapshot booking = event.booking();
        String cancelUrl = frontendUrl + "/guest/bookings/" + booking.cancelToken();
        String period = formatPeriod(booking);

        emailService.sendEmail(
                booking.clientEmail(),
                "Lembrete de agendamento: " + booking.eventTitle(),
                """
                <h2>Olá, %s!</h2>
                <p>Este é um lembrete do seu agendamento de <strong>%s</strong> com <strong>%s</strong> para <strong>%s</strong>.</p>
                <p>Precisa cancelar ou visualizar mais detalhes? <a href="%s">Clique aqui para gerenciar seu agendamento</a>.</p>
                """.formatted(
                        escape(booking.clientName()),
                        escape(booking.eventTitle()),
                        escape(booking.providerName()),
                        period,
                        cancelUrl
                ),
                null
        );
    }

    private static String formatPeriod(BookingSnapshot booking) {
        ZoneId zone = resolveZone(booking.providerTimeZone());
        ZonedDateTime start = booking.startTime().atZone(zone);
        ZonedDateTime end = booking.endTime().atZone(zone);

        return "%s das %s às %s (%s)".formatted(
                start.format(DATE_FMT),
                start.format(TIME_FMT),
                end.format(TIME_FMT),
                zone.getId()
        );
    }

    private static ZoneId resolveZone(String timeZone) {
        return timeZone != null && ZoneId.getAvailableZoneIds().contains(timeZone)
                ? ZoneId.of(timeZone)
                : ZoneId.of("UTC");
    }

    private static String escape(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value);
    }
}
