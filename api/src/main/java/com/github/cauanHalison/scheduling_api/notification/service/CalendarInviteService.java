package com.github.cauanHalison.scheduling_api.notification.service;

import com.github.cauanHalison.scheduling_api.notification.event.BookingSnapshot;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.property.*;
import net.fortuna.ical4j.model.property.immutable.ImmutableCalScale;
import net.fortuna.ical4j.model.property.immutable.ImmutableMethod;
import net.fortuna.ical4j.model.property.immutable.ImmutableStatus;
import net.fortuna.ical4j.model.property.immutable.ImmutableVersion;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.net.URI;

@Service
public class CalendarInviteService {
    public byte[] generateInviteIcs(BookingSnapshot booking, boolean isCanceled) {
        VEvent event = new VEvent(
                booking.startTime(),
                booking.endTime(),
                booking.eventTitle()
        );

        event.add(new Uid(booking.bookingId().toString() + "@scheduling-api.com"));
        event.add(new Description(booking.eventDescription() != null ? booking.eventDescription() : ""));
        if (!isCanceled && booking.eventLink() != null && !booking.eventLink().isBlank()) {
            event.add(new Location(booking.eventLink()));
        }
        event.add(isCanceled ? ImmutableStatus.VEVENT_CANCELLED : ImmutableStatus.VEVENT_CONFIRMED);
        event.add(new Sequence(isCanceled ? 1 : 0));

        Organizer organizer = new Organizer(URI.create("mailto:" + booking.providerEmail()));
        organizer.add(new Cn(booking.providerName()));
        event.add(organizer);

        Attendee attendee = new Attendee(URI.create("mailto:" + booking.clientEmail()));
        attendee.add(new Cn(booking.clientName()));
        attendee.add(Role.REQ_PARTICIPANT);
        event.add(attendee);

        Calendar calendar = new Calendar();
        calendar.add(new ProdId("-//Scheduling API//iCal4j 4.0//PT"));
        calendar.add(ImmutableVersion.VERSION_2_0);
        calendar.add(ImmutableCalScale.GREGORIAN);
        calendar.add(isCanceled ? ImmutableMethod.CANCEL : ImmutableMethod.REQUEST);
        calendar.add(event);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CalendarOutputter outputter = new CalendarOutputter();
            outputter.output(calendar, out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar arquivo .ics", e);
        }
    }
}
