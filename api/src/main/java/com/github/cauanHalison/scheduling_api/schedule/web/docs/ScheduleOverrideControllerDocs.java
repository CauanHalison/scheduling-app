package com.github.cauanHalison.scheduling_api.schedule.web.docs;

import com.github.cauanHalison.scheduling_api.config.security.ProviderPrincipal;
import com.github.cauanHalison.scheduling_api.schedule.dto.ScheduleOverrideRequest;
import com.github.cauanHalison.scheduling_api.schedule.dto.ScheduleOverrideResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

@Tag(name = "Schedule overrides", description = "Endpoints for manage overrides in the authenticated provider schedule")
public interface ScheduleOverrideControllerDocs {
    @Operation(
            summary = "Find next overrides",
            description = "Find next overrides in the authenticated provider schedule"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Schedule overrides found")
    })
    ResponseEntity<List<ScheduleOverrideResponse>> findAllFuture(@Parameter(hidden = true) ProviderPrincipal principal);

    @Operation(
            summary = "Create a new override",
            description = "Create a new override in the authenticated provider schedule"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "New override created successfully")
    })
    ResponseEntity<ScheduleOverrideResponse> create(
            ScheduleOverrideRequest request,
            @Parameter(hidden = true) ProviderPrincipal principal
    );

    @Operation(
            summary = "Delete an override",
            description = "Delete an override in the authenticated provider schedule"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Override deleted successfully")
    })
    ResponseEntity<Void> delete(
            UUID overrideId,
            @Parameter(hidden = true) ProviderPrincipal principal
    );
}
