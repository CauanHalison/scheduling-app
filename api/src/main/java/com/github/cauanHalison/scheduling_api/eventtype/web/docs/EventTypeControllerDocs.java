package com.github.cauanHalison.scheduling_api.eventtype.web.docs;

import com.github.cauanHalison.scheduling_api.config.security.ProviderPrincipal;
import com.github.cauanHalison.scheduling_api.eventtype.dto.EventTypeRequest;
import com.github.cauanHalison.scheduling_api.eventtype.dto.EventTypeResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

@Tag(name = "Event type", description = "Endpoints for CRUD of event types from authenticated provider")
public interface EventTypeControllerDocs {
    @Operation(summary = "Find all", description = "Search for all event types from the authenticated provider")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All event types listed")
    })
    ResponseEntity<List<EventTypeResponse>> findAll(@Parameter (hidden = true) ProviderPrincipal principal);

    @Operation(summary = "Find by id", description = "Search for an event type by id from the authenticated provider")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event type found")
    })
    ResponseEntity<EventTypeResponse> findById(@Parameter (hidden = true) ProviderPrincipal principal, UUID eventTypeId);

    @Operation(summary = "Create new event type", description = "Create a new event type linked to authenticated provider")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Event type created successfully")
    })
    ResponseEntity<EventTypeResponse> create(@Parameter (hidden = true) ProviderPrincipal principal, EventTypeRequest request);

    @Operation(summary = "Update an event type", description = "Update an event type from authenticated provider")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event type updated successfully")
    })
    ResponseEntity<EventTypeResponse> update(@Parameter (hidden = true) ProviderPrincipal principal, EventTypeRequest request, UUID eventTypeId);

    @Operation(summary = "Toggle is active value", description = "Toggle is active value of an event type from authenticated provider")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Event type updated successfully")
    })
    ResponseEntity<EventTypeResponse> toggleIsActive(@Parameter (hidden = true) ProviderPrincipal principal, UUID eventTypeId);

    @Operation(summary = "Delete an event type", description = "Delete an event type from authenticated provider")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Event type deleted successfully")
    })
    ResponseEntity<Void> delete(@Parameter (hidden = true) ProviderPrincipal principal, UUID eventTypeId);
}
