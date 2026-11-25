package com.github.cauanHalison.scheduling_api.bookings.web.docs;

import com.github.cauanHalison.scheduling_api.bookings.domain.enums.BookingStatus;
import com.github.cauanHalison.scheduling_api.bookings.dto.CancelBookingRequest;
import com.github.cauanHalison.scheduling_api.bookings.dto.ProviderBookingResponse;
import com.github.cauanHalison.scheduling_api.config.security.ProviderPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.UUID;

@Tag(name = "Provider Bookings", description = "Booking management endpoints for the authenticated provider")
@SecurityRequirement(name = "bearerAuth")
public interface ProviderBookingControllerDocs {

    @Operation(summary = "List bookings", description = "Lists all bookings of the authenticated provider with optional status, date range, and pagination filters")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bookings page returned successfully")
    })
    ResponseEntity<Page<ProviderBookingResponse>> listBookings(
            @Parameter(hidden = true) ProviderPrincipal principal,
            @Parameter(description = "Filter by status (CONFIRMED, CANCELED or COMPLETED)") BookingStatus status,
            @Parameter(description = "Minimum start date in UTC") Instant startDate,
            @Parameter(description = "Maximum start date in UTC") Instant endDate,
            @Parameter(description = "Filter by eventType") UUID eventTypeId,
            Pageable pageable
    );

    @Operation(summary = "Get booking details", description = "Returns the details of a specific booking belonging to the authenticated provider")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking found"),
            @ApiResponse(responseCode = "404", description = "Booking not found")
    })
    ResponseEntity<ProviderBookingResponse> getBookingById(
            @Parameter(hidden = true) ProviderPrincipal principal,
            @Parameter(description = "Booking ID") UUID id
    );

    @Operation(summary = "Cancel booking", description = "Cancels a booking with a reason, freeing the time slot immediately")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Booking canceled successfully"),
            @ApiResponse(responseCode = "404", description = "Booking not found"),
            @ApiResponse(responseCode = "409", description = "Booking is already canceled or completed")
    })
    ResponseEntity<ProviderBookingResponse> cancelBooking(
            @Parameter(hidden = true) ProviderPrincipal principal,
            @Parameter(description = "Booking ID") UUID id,
            CancelBookingRequest request
    );
}
