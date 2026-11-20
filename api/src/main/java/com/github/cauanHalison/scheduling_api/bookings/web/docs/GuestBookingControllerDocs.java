package com.github.cauanHalison.scheduling_api.bookings.web.docs;

import com.github.cauanHalison.scheduling_api.bookings.dto.CancelBookingRequest;
import com.github.cauanHalison.scheduling_api.bookings.dto.GuestBookingResponse;
import com.github.cauanHalison.scheduling_api.bookings.dto.MagicLinkRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

@Tag(name = "Guest Bookings", description = "Self-service endpoints for guests via secure token and Magic Link")
public interface GuestBookingControllerDocs {

    @Operation(summary = "Get booking by cancel token", description = "Retrieves the full booking details using the cancellation token sent via email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking found"),
            @ApiResponse(responseCode = "404", description = "Booking not found for the provided token")
    })
    ResponseEntity<GuestBookingResponse> getBookingByCancelToken(
            @Parameter(description = "Secure UUID token received by the guest") UUID cancelToken
    );

    @Operation(summary = "Cancel booking by token", description = "Cancels the meeting using the secure token, freeing the time slot for new bookings")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking canceled successfully"),
            @ApiResponse(responseCode = "404", description = "Booking not found"),
            @ApiResponse(responseCode = "409", description = "Booking is already canceled")
    })
    ResponseEntity<GuestBookingResponse> cancelBookingByToken(
            @Parameter(description = "Secure UUID token received by the guest") UUID cancelToken,
            CancelBookingRequest request
    );

    @Operation(summary = "Request Magic Link", description = "Generates a temporary token valid for 15 minutes in Redis to list all bookings linked to the provided email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Request processed successfully"),
            @ApiResponse(responseCode = "429", description = "Too many requests. Check the Retry-After header")
    })
    ResponseEntity<Void> requestMagicLink(MagicLinkRequest request, @Parameter(hidden = true) HttpServletRequest httpRequest);

    @Operation(summary = "Verify Magic Link", description = "Validates the temporary Magic Link token and returns the list of bookings associated with the email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bookings listed successfully"),
            @ApiResponse(responseCode = "401", description = "Expired or invalid token")
    })
    ResponseEntity<List<GuestBookingResponse>> verifyMagicLink(
            @Parameter(description = "Temporary Magic Link token received in the link") String token
    );
}
