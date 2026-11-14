package com.github.cauanHalison.scheduling_api.bookings.service;

import com.github.cauanHalison.scheduling_api.bookings.domain.BookingEntity;
import com.github.cauanHalison.scheduling_api.bookings.dto.AvailableDaysResponse;
import com.github.cauanHalison.scheduling_api.bookings.dto.AvailableSlotsResponse;
import com.github.cauanHalison.scheduling_api.bookings.dto.SlotResponse;
import com.github.cauanHalison.scheduling_api.bookings.repository.BookingRepository;
import com.github.cauanHalison.scheduling_api.config.cache.CacheConfiguration;
import com.github.cauanHalison.scheduling_api.eventtype.domain.EventTypeEntity;
import com.github.cauanHalison.scheduling_api.eventtype.repository.EventTypeRepository;
import com.github.cauanHalison.scheduling_api.exception.exceptions.ResourceNotFoundException;
import com.github.cauanHalison.scheduling_api.schedule.domain.AvailabilityScheduleEntity;
import com.github.cauanHalison.scheduling_api.schedule.domain.ScheduleOverrideEntity;
import com.github.cauanHalison.scheduling_api.schedule.repository.AvailabilityScheduleRepository;
import com.github.cauanHalison.scheduling_api.schedule.repository.ScheduleOverrideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SlotCalculationService {

    private final EventTypeRepository eventTypeRepository;
    private final AvailabilityScheduleRepository availabilityScheduleRepository;
    private final ScheduleOverrideRepository scheduleOverrideRepository;
    private final BookingRepository bookingRepository;

    public record TimeWindow(Instant start, Instant end) {
        public TimeWindow {
            if (!end.isAfter(start)) {
                throw new IllegalArgumentException("O horário final deve ser posterior ao horário inicial.");
            }
        }

        public boolean overlaps(TimeWindow other) {
            return this.start.isBefore(other.end) && other.start.isBefore(this.end);
        }
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfiguration.AVAILABLE_DAYS_CACHE, key = "#userSlug + ':' + #eventSlug + ':' + #yearMonth.toString() + ':' + #clientZoneId.id")
    public AvailableDaysResponse getAvailableDays(String userSlug, String eventSlug, YearMonth yearMonth, ZoneId clientZoneId) {
        log.debug("Calculating available days: userSlug={}, eventSlug={}, yearMonth={}, clientZoneId={}",
                userSlug, eventSlug, yearMonth, clientZoneId);

        EventTypeEntity eventType = findActiveEventType(userSlug, eventSlug);

        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();
        LocalDate todayInClientZone = LocalDate.now(clientZoneId);

        if (endDate.isBefore(todayInClientZone)) {
            log.debug("Requested month is completely in the past: yearMonth={}", yearMonth);
            return new AvailableDaysResponse(List.of());
        }

        if (startDate.isBefore(todayInClientZone)) {
            startDate = todayInClientZone;
        }

        List<LocalDate> availableDays = new ArrayList<>();
        LocalDate current = startDate;

        while (!current.isAfter(endDate)) {
            List<SlotResponse> slots = calculateDaySlots(eventType, current, clientZoneId);
            if (!slots.isEmpty()) {
                availableDays.add(current);
            }
            current = current.plusDays(1);
        }

        log.info("Available days calculated successfully: userSlug={}, eventSlug={}, count={}, yearMonth={}",
                userSlug, eventSlug, availableDays.size(), yearMonth);

        return new AvailableDaysResponse(availableDays);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfiguration.AVAILABLE_SLOTS_CACHE, key = "#userSlug + ':' + #eventSlug + ':' + #date.toString() + ':' + #clientZoneId.id")
    public AvailableSlotsResponse getAvailableSlots(String userSlug, String eventSlug, LocalDate date, ZoneId clientZoneId) {
        log.debug("Calculating available slots: userSlug={}, eventSlug={}, date={}, clientZoneId={}",
                userSlug, eventSlug, date, clientZoneId);

        EventTypeEntity eventType = findActiveEventType(userSlug, eventSlug);
        List<SlotResponse> slots = calculateDaySlots(eventType, date, clientZoneId);

        log.info("Available slots calculated successfully: userSlug={}, eventSlug={}, date={}, clientZoneId={}, count={}",
                userSlug, eventSlug, date, clientZoneId, slots.size());

        return new AvailableSlotsResponse(date, clientZoneId.getId(), slots);
    }

    @Transactional(readOnly = true)
    public boolean isSlotAvailable(EventTypeEntity eventType, Instant requestedStartTime) {
        if (!requestedStartTime.isAfter(Instant.now())) {
            log.warn("Requested slot start time is in the past: startTime={}", requestedStartTime);
            return false;
        }

        ZoneId providerZone = ZoneId.of(eventType.getProvider().getTimeZone());
        LocalDate clientDate = requestedStartTime.atZone(providerZone).toLocalDate();

        List<SlotResponse> slots = calculateDaySlots(eventType, clientDate, providerZone);
        boolean available = slots.stream().anyMatch(slot -> slot.startTime().equals(requestedStartTime));

        log.debug("Slot availability check: providerSlug={}, eventSlug={}, startTime={}, available={}",
                eventType.getProvider().getSlug(), eventType.getSlug(), requestedStartTime, available);

        return available;
    }

    public List<SlotResponse> calculateDaySlots(EventTypeEntity eventType, LocalDate clientDate, ZoneId clientZoneId) {
        Instant dayStartUtc = clientDate.atStartOfDay(clientZoneId).toInstant();
        Instant dayEndUtc = clientDate.plusDays(1).atStartOfDay(clientZoneId).toInstant();

        UUID providerId = eventType.getProvider().getId();
        ZoneId providerZoneId = ZoneId.of(eventType.getProvider().getTimeZone());

        List<TimeWindow> baseWindows = buildOperationalWindows(providerId, providerZoneId, dayStartUtc, dayEndUtc);
        if (baseWindows.isEmpty()) {
            return List.of();
        }

        List<ScheduleOverrideEntity> overrides = scheduleOverrideRepository.findOverlapping(providerId, dayStartUtc, dayEndUtc);
        List<TimeWindow> blockedOverrideWindows = overrides.stream()
                .map(o -> new TimeWindow(o.getStartTime(), o.getEndTime()))
                .toList();

        List<TimeWindow> windowsAfterOverrides = subtractWindows(baseWindows, blockedOverrideWindows);
        if (windowsAfterOverrides.isEmpty()) {
            return List.of();
        }

        List<BookingEntity> bookings = bookingRepository.findConfirmedOverlapping(providerId, dayStartUtc, dayEndUtc);
        List<TimeWindow> blockedBookingWindows = bookings.stream()
                .map(b -> new TimeWindow(b.getStartTime(), b.getBlockedUntil()))
                .toList();

        List<TimeWindow> freeWindows = subtractWindows(windowsAfterOverrides, blockedBookingWindows);
        if (freeWindows.isEmpty()) {
            return List.of();
        }

        return sliceIntoSlots(freeWindows, eventType.getDurationMinutes(), dayStartUtc, dayEndUtc);
    }

    private List<TimeWindow> buildOperationalWindows(
            UUID providerId,
            ZoneId providerZoneId,
            Instant rangeStart,
            Instant rangeEnd
    ) {
        List<AvailabilityScheduleEntity> schedules = availabilityScheduleRepository.findAllByProviderId(providerId);
        if (schedules.isEmpty()) {
            return List.of();
        }

        LocalDate startDate = rangeStart.atZone(providerZoneId).toLocalDate().minusDays(1);
        LocalDate endDate = rangeEnd.atZone(providerZoneId).toLocalDate().plusDays(1);

        List<TimeWindow> windows = new ArrayList<>();

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            DayOfWeek dayOfWeek = date.getDayOfWeek();
            for (AvailabilityScheduleEntity schedule : schedules) {
                if (schedule.getDayOfWeek() == dayOfWeek) {
                    Instant windowStart = date.atTime(schedule.getStartTime()).atZone(providerZoneId).toInstant();
                    Instant windowEnd = date.atTime(schedule.getEndTime()).atZone(providerZoneId).toInstant();

                    if (windowEnd.isAfter(rangeStart) && windowStart.isBefore(rangeEnd)) {
                        Instant boundedStart = windowStart.isBefore(rangeStart) ? rangeStart : windowStart;
                        Instant boundedEnd = windowEnd.isAfter(rangeEnd) ? rangeEnd : windowEnd;
                        if (boundedEnd.isAfter(boundedStart)) {
                            windows.add(new TimeWindow(boundedStart, boundedEnd));
                        }
                    }
                }
            }
        }

        windows.sort(Comparator.comparing(TimeWindow::start));
        return mergeConsecutive(windows);
    }

    private List<TimeWindow> subtractWindows(List<TimeWindow> sources, List<TimeWindow> blockages) {
        List<TimeWindow> current = new ArrayList<>(sources);

        for (TimeWindow block : blockages) {
            List<TimeWindow> next = new ArrayList<>();
            for (TimeWindow src : current) {
                if (!src.overlaps(block)) {
                    next.add(src);
                } else {
                    if (src.start().isBefore(block.start())) {
                        next.add(new TimeWindow(src.start(), block.start()));
                    }
                    if (src.end().isAfter(block.end())) {
                        next.add(new TimeWindow(block.end(), src.end()));
                    }
                }
            }
            current = next;
        }

        current.sort(Comparator.comparing(TimeWindow::start));
        return current;
    }

    private List<SlotResponse> sliceIntoSlots(
            List<TimeWindow> freeWindows,
            int durationMinutes,
            Instant dayStartUtc,
            Instant dayEndUtc
    ) {
        List<SlotResponse> slots = new ArrayList<>();
        Instant now = Instant.now();

        for (TimeWindow window : freeWindows) {
            Instant slotStart = window.start();
            while (true) {
                Instant slotEnd = slotStart.plus(durationMinutes, ChronoUnit.MINUTES);
                if (slotEnd.isAfter(window.end())) {
                    break;
                }

                if (slotStart.isAfter(now)
                        && !slotStart.isBefore(dayStartUtc)
                        && slotStart.isBefore(dayEndUtc)) {
                    slots.add(new SlotResponse(slotStart, slotEnd));
                }

                slotStart = slotStart.plus(durationMinutes, ChronoUnit.MINUTES);
            }
        }

        return slots;
    }

    private List<TimeWindow> mergeConsecutive(List<TimeWindow> windows) {
        if (windows.size() <= 1) {
            return windows;
        }

        List<TimeWindow> merged = new ArrayList<>();
        TimeWindow prev = windows.getFirst();

        for (int i = 1; i < windows.size(); i++) {
            TimeWindow curr = windows.get(i);
            if (!curr.start().isAfter(prev.end())) {
                Instant maxEnd = curr.end().isAfter(prev.end()) ? curr.end() : prev.end();
                prev = new TimeWindow(prev.start(), maxEnd);
            } else {
                merged.add(prev);
                prev = curr;
            }
        }
        merged.add(prev);
        return merged;
    }

    private EventTypeEntity findActiveEventType(String userSlug, String eventSlug) {
        return eventTypeRepository.findByProviderSlugAndSlugAndIsActiveTrue(userSlug, eventSlug)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de evento não encontrado para este prestador ou inativo."));
    }
}
