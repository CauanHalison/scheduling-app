package com.github.cauanHalison.scheduling_api.bookings.web;

import com.github.cauanHalison.scheduling_api.bookings.domain.enums.BookingStatus;
import com.github.cauanHalison.scheduling_api.bookings.dto.CancelBookingRequest;
import com.github.cauanHalison.scheduling_api.bookings.dto.ProviderBookingResponse;
import com.github.cauanHalison.scheduling_api.bookings.service.ProviderBookingService;
import com.github.cauanHalison.scheduling_api.bookings.web.docs.ProviderBookingControllerDocs;
import com.github.cauanHalison.scheduling_api.config.security.ProviderPrincipal;
import com.github.cauanHalison.scheduling_api.exception.exceptions.ResourceNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/provider/bookings")
@RequiredArgsConstructor
public class ProviderBookingController implements ProviderBookingControllerDocs {

    private final ProviderBookingService providerBookingService;

    @Override
    @GetMapping
    public ResponseEntity<Page<ProviderBookingResponse>> listBookings(
            @AuthenticationPrincipal ProviderPrincipal principal,
            @RequestParam(required = false) BookingStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) UUID eventTypeId,
            @PageableDefault(sort = "startTime", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        UUID providerId = getProviderId(principal);
        return ResponseEntity.ok(providerBookingService.listBookings(providerId, status, startDate, endDate, eventTypeId, pageable));
    }

    @Override
    @GetMapping("/{id}")
    public ResponseEntity<ProviderBookingResponse> getBookingById(
            @AuthenticationPrincipal ProviderPrincipal principal,
            @PathVariable UUID id
    ) {
        UUID providerId = getProviderId(principal);
        return ResponseEntity.ok(providerBookingService.getBookingById(providerId, id));
    }

    @Override
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ProviderBookingResponse> cancelBooking(
            @AuthenticationPrincipal ProviderPrincipal principal,
            @PathVariable UUID id,
            @RequestBody(required = false) @Valid CancelBookingRequest request
    ) {
        UUID providerId = getProviderId(principal);
        return ResponseEntity.ok(providerBookingService.cancelBooking(providerId, id, request));
    }

    private UUID getProviderId(ProviderPrincipal principal) {
        if (principal == null) {
            throw new ResourceNotFoundException("Prestador não autenticado.");
        }
        return principal.id();
    }
}
