package com.github.cauanHalison.scheduling_api.bookings.web;

import com.github.cauanHalison.scheduling_api.bookings.dto.CancelBookingRequest;
import com.github.cauanHalison.scheduling_api.bookings.dto.GuestBookingResponse;
import com.github.cauanHalison.scheduling_api.bookings.dto.MagicLinkRequest;
import com.github.cauanHalison.scheduling_api.bookings.service.GuestBookingService;
import com.github.cauanHalison.scheduling_api.bookings.web.docs.GuestBookingControllerDocs;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/guest")
@RequiredArgsConstructor
public class GuestBookingController implements GuestBookingControllerDocs {

    private final GuestBookingService guestBookingService;

    @Override
    @GetMapping("/bookings/{cancelToken}")
    public ResponseEntity<GuestBookingResponse> getBookingByCancelToken(@PathVariable UUID cancelToken) {
        return ResponseEntity.ok(guestBookingService.getBookingByCancelToken(cancelToken));
    }

    @Override
    @PostMapping("/bookings/{cancelToken}/cancel")
    public ResponseEntity<GuestBookingResponse> cancelBookingByToken(
            @PathVariable UUID cancelToken,
            @RequestBody(required = false) @Valid CancelBookingRequest request
    ) {
        return ResponseEntity.ok(guestBookingService.cancelBookingByToken(cancelToken, request));
    }

    @Override
    @PostMapping("/magic-link/request")
    public ResponseEntity<Void> requestMagicLink(
            @RequestBody @Valid MagicLinkRequest request,
            HttpServletRequest httpRequest
    ) {
        guestBookingService.requestMagicLink(request, httpRequest.getRemoteAddr());
        return ResponseEntity.ok().build();
    }

    @Override
    @GetMapping("/magic-link/verify")
    public ResponseEntity<List<GuestBookingResponse>> verifyMagicLink(@RequestParam String token) {
        return ResponseEntity.ok(guestBookingService.verifyMagicLink(token));
    }
}
