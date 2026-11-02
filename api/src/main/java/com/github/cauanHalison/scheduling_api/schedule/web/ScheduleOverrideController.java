package com.github.cauanHalison.scheduling_api.schedule.web;

import com.github.cauanHalison.scheduling_api.config.security.ProviderPrincipal;
import com.github.cauanHalison.scheduling_api.schedule.dto.ScheduleOverrideRequest;
import com.github.cauanHalison.scheduling_api.schedule.dto.ScheduleOverrideResponse;
import com.github.cauanHalison.scheduling_api.schedule.service.ScheduleOverrideService;
import com.github.cauanHalison.scheduling_api.schedule.web.docs.ScheduleOverrideControllerDocs;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@AllArgsConstructor
@RequestMapping("/schedules/overrides")
public class ScheduleOverrideController implements ScheduleOverrideControllerDocs {
    private final ScheduleOverrideService scheduleOverrideService;

    @Override
    @GetMapping
    public ResponseEntity<List<ScheduleOverrideResponse>> findAllFuture(
            @AuthenticationPrincipal ProviderPrincipal principal
    ) {
        List<ScheduleOverrideResponse> response = scheduleOverrideService.findAllFuture(principal.id());
        return ResponseEntity.ok(response);
    }

    @Override
    @PostMapping
    public ResponseEntity<ScheduleOverrideResponse> create(
            @RequestBody @Valid ScheduleOverrideRequest request,
            @AuthenticationPrincipal ProviderPrincipal principal) {
        ScheduleOverrideResponse response = scheduleOverrideService.create(request, principal.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    @DeleteMapping("/{overrideId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID overrideId,
            @AuthenticationPrincipal ProviderPrincipal principal) {
        scheduleOverrideService.delete(overrideId, principal.id());
        return ResponseEntity.noContent().build();
    }
}
