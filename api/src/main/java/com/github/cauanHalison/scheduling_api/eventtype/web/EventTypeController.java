package com.github.cauanHalison.scheduling_api.eventtype.web;

import com.github.cauanHalison.scheduling_api.config.security.ProviderPrincipal;
import com.github.cauanHalison.scheduling_api.eventtype.dto.EventTypeRequest;
import com.github.cauanHalison.scheduling_api.eventtype.dto.EventTypeResponse;
import com.github.cauanHalison.scheduling_api.eventtype.service.EventTypeService;
import com.github.cauanHalison.scheduling_api.eventtype.web.docs.EventTypeControllerDocs;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/event-types")
@RequiredArgsConstructor
public class EventTypeController implements EventTypeControllerDocs {
    private final EventTypeService eventTypeService;

    @Override
    @GetMapping
    public ResponseEntity<List<EventTypeResponse>> findAll(
            @AuthenticationPrincipal ProviderPrincipal principal
    ) {
        return ResponseEntity.ok(eventTypeService.findAll(principal.id()));
    }

    @Override
    @PostMapping
    public ResponseEntity<EventTypeResponse> create(
            @AuthenticationPrincipal ProviderPrincipal principal,
            @RequestBody @Valid EventTypeRequest request
    ) {

        return ResponseEntity.status(HttpStatus.CREATED).body(eventTypeService.create(request, principal.id()));
    }

    @Override
    @PutMapping("/{eventTypeId}")
    public ResponseEntity<EventTypeResponse> update(
            @AuthenticationPrincipal ProviderPrincipal principal,
            @RequestBody @Valid EventTypeRequest request,
            @PathVariable(value = "eventTypeId") UUID eventTypeId
    ) {
        return ResponseEntity.ok(eventTypeService.update(eventTypeId, request, principal.id()));
    }

    @Override
    @PatchMapping("/{eventTypeId}")
    public ResponseEntity<EventTypeResponse> toggleIsActive(
            @AuthenticationPrincipal ProviderPrincipal principal,
            @PathVariable(value = "eventTypeId") UUID eventTypeId
    ) {
        return ResponseEntity.ok(eventTypeService.toggleIsActive(eventTypeId, principal.id()));
    }

    @Override
    @DeleteMapping("/{eventTypeId}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal ProviderPrincipal principal,
            @PathVariable(value = "eventTypeId") UUID eventTypeId
    ) {
        eventTypeService.delete(eventTypeId, principal.id());
        return ResponseEntity.noContent().build();
    }
}
