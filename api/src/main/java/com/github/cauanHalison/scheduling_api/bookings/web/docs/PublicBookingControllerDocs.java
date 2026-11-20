package com.github.cauanHalison.scheduling_api.bookings.web.docs;

import com.github.cauanHalison.scheduling_api.bookings.dto.AvailableDaysResponse;
import com.github.cauanHalison.scheduling_api.bookings.dto.AvailableSlotsResponse;
import com.github.cauanHalison.scheduling_api.bookings.dto.BookingConfirmationResponse;
import com.github.cauanHalison.scheduling_api.bookings.dto.CreateBookingRequest;
import com.github.cauanHalison.scheduling_api.bookings.dto.PublicEventDetailsResponse;
import com.github.cauanHalison.scheduling_api.eventtype.dto.EventTypeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Public Bookings", description = "Public endpoints for checking availability and creating bookings")
public interface PublicBookingControllerDocs {

    @Operation(summary = "Get provider event catalog", description = "Lists all active event types configured by the provider")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Active event types returned successfully")
    })
    ResponseEntity<List<EventTypeResponse>> getProviderEvents(
            @Parameter(description = "Service provider slug", example = "cauan-halison") String userSlug
    );

    @Operation(summary = "Get public event details", description = "Returns the event type and provider metadata for the public booking page")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event metadata retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Event or provider not found")
    })
    ResponseEntity<PublicEventDetailsResponse> getPublicEventDetails(
            @Parameter(description = "Service provider slug", example = "cauan-halison") String userSlug,
            @Parameter(description = "Event type slug", example = "consultoria-30min") String eventSlug
    );

    @Operation(summary = "Get available days in month", description = "Returns the list of dates with free slots for the given month/year in the guest's time zone")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Available dates returned successfully"),
            @ApiResponse(responseCode = "404", description = "Event type or provider not found")
    })
    ResponseEntity<AvailableDaysResponse> getAvailableDays(
            @Parameter(description = "Service provider slug", example = "cauan-halison") String userSlug,
            @Parameter(description = "Event type slug", example = "consultoria-30min") String eventSlug,
            @Parameter(description = "Target month (1-12)", example = "11") Integer month,
            @Parameter(description = "Target year", example = "2026") Integer year,
            @Parameter(description = "Guest IANA time zone", example = "America/Sao_Paulo") String timeZone
    );

    @Operation(summary = "Get available slots for a date", description = "Calculates and returns the free time slots for a specific day")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Available slots returned successfully"),
            @ApiResponse(responseCode = "404", description = "Event type or provider not found")
    })
    ResponseEntity<AvailableSlotsResponse> getAvailableSlots(
            @Parameter(description = "Service provider slug", example = "cauan-halison") String userSlug,
            @Parameter(description = "Event type slug", example = "consultoria-30min") String eventSlug,
            @Parameter(description = "Date in YYYY-MM-DD format", example = "2026-11-10") LocalDate date,
            @Parameter(description = "Guest IANA time zone", example = "America/Sao_Paulo") String timeZone
    );

    @Operation(summary = "Create public booking", description = "Books a free time slot with anti-double-booking protection")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Booking confirmed successfully"),
            @ApiResponse(responseCode = "404", description = "Event type or provider not found"),
            @ApiResponse(responseCode = "409", description = "Time slot unavailable or concurrent booking conflict")
    })
    ResponseEntity<BookingConfirmationResponse> book(
            @Parameter(description = "Service provider slug", example = "cauan-halison") String userSlug,
            @Parameter(description = "Event type slug", example = "consultoria-30min") String eventSlug,
            CreateBookingRequest request
    );
}
