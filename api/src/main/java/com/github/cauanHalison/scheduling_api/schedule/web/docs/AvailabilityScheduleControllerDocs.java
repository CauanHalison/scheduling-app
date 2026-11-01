package com.github.cauanHalison.scheduling_api.schedule.web.docs;

import com.github.cauanHalison.scheduling_api.config.security.ProviderPrincipal;
import com.github.cauanHalison.scheduling_api.schedule.dto.WeeklyScheduleRequest;
import com.github.cauanHalison.scheduling_api.schedule.dto.WeeklyScheduleResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Availability schedule", description = "Endpoints for the weekly availability schedule of a provider")
public interface AvailabilityScheduleControllerDocs {
    @Operation(
            summary = "Get weekly schedule",
            description = "Returns the full weekly schedule (Monday to Sunday) and the time zone of the authenticated provider. " +
                    "Days without availability are returned with an empty list of intervals."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Weekly schedule found")
    })
    ResponseEntity<WeeklyScheduleResponse> find(@Parameter(hidden = true) ProviderPrincipal principal);

    @Operation(
            summary = "Replace weekly schedule",
            description = "Atomically replaces the entire weekly schedule of the authenticated provider. " +
                    "Days omitted or sent with an empty list of intervals are treated as days off. " +
                    "A day cannot be repeated and intervals of the same day cannot overlap."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Weekly schedule replaced successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid schedule (repeated day, overlapping or invalid intervals)")
    })
    ResponseEntity<WeeklyScheduleResponse> replace(@Parameter(hidden = true) ProviderPrincipal principal, WeeklyScheduleRequest request);
}
