package com.github.cauanHalison.scheduling_api.bookings.web;

import com.github.cauanHalison.scheduling_api.bookings.dto.AvailableDaysResponse;
import com.github.cauanHalison.scheduling_api.bookings.dto.AvailableSlotsResponse;
import com.github.cauanHalison.scheduling_api.bookings.dto.BookingConfirmationResponse;
import com.github.cauanHalison.scheduling_api.bookings.dto.CreateBookingRequest;
import com.github.cauanHalison.scheduling_api.bookings.dto.PublicEventDetailsResponse;
import com.github.cauanHalison.scheduling_api.bookings.service.PublicBookingService;
import com.github.cauanHalison.scheduling_api.bookings.service.SlotCalculationService;
import com.github.cauanHalison.scheduling_api.bookings.web.docs.PublicBookingControllerDocs;
import com.github.cauanHalison.scheduling_api.eventtype.dto.EventTypeResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/public/users/{userSlug}")
@RequiredArgsConstructor
public class PublicBookingController implements PublicBookingControllerDocs {

    private final PublicBookingService publicBookingService;
    private final SlotCalculationService slotCalculationService;

    @Override
    @GetMapping
    public ResponseEntity<List<EventTypeResponse>> getProviderEvents(@PathVariable String userSlug) {
        return ResponseEntity.ok(publicBookingService.getProviderActiveEvents(userSlug));
    }

    @Override
    @GetMapping("/events/{eventSlug}")
    public ResponseEntity<PublicEventDetailsResponse> getPublicEventDetails(
            @PathVariable String userSlug,
            @PathVariable String eventSlug
    ) {
        return ResponseEntity.ok(publicBookingService.getPublicEventDetails(userSlug, eventSlug));
    }

    @Override
    @GetMapping("/events/{eventSlug}/available-days")
    public ResponseEntity<AvailableDaysResponse> getAvailableDays(
            @PathVariable String userSlug,
            @PathVariable String eventSlug,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(defaultValue = "UTC") String timeZone
    ) {
        ZoneId zoneId = ZoneId.of(timeZone);
        LocalDate now = LocalDate.now(zoneId);
        int targetMonth = (month != null) ? month : now.getMonthValue();
        int targetYear = (year != null) ? year : now.getYear();

        YearMonth yearMonth = YearMonth.of(targetYear, targetMonth);
        return ResponseEntity.ok(slotCalculationService.getAvailableDays(userSlug, eventSlug, yearMonth, zoneId));
    }

    @Override
    @GetMapping("/events/{eventSlug}/slots")
    public ResponseEntity<AvailableSlotsResponse> getAvailableSlots(
            @PathVariable String userSlug,
            @PathVariable String eventSlug,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "UTC") String timeZone
    ) {
        ZoneId zoneId = ZoneId.of(timeZone);
        return ResponseEntity.ok(slotCalculationService.getAvailableSlots(userSlug, eventSlug, date, zoneId));
    }

    @Override
    @PostMapping("/events/{eventSlug}/book")
    public ResponseEntity<BookingConfirmationResponse> book(
            @PathVariable String userSlug,
            @PathVariable String eventSlug,
            @RequestBody @Valid CreateBookingRequest request
    ) {
        BookingConfirmationResponse response = publicBookingService.createBooking(userSlug, eventSlug, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
