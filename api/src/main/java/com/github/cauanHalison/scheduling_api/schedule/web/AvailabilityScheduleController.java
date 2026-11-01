package com.github.cauanHalison.scheduling_api.schedule.web;

import com.github.cauanHalison.scheduling_api.config.security.ProviderPrincipal;
import com.github.cauanHalison.scheduling_api.schedule.dto.WeeklyScheduleRequest;
import com.github.cauanHalison.scheduling_api.schedule.dto.WeeklyScheduleResponse;
import com.github.cauanHalison.scheduling_api.schedule.service.AvailabilityScheduleService;
import com.github.cauanHalison.scheduling_api.schedule.web.docs.AvailabilityScheduleControllerDocs;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/schedules")
public class AvailabilityScheduleController implements AvailabilityScheduleControllerDocs {
    private final AvailabilityScheduleService availabilityScheduleService;

    @Override
    @GetMapping
    public ResponseEntity<WeeklyScheduleResponse> find(
            @AuthenticationPrincipal ProviderPrincipal principal
    ) {
        return ResponseEntity.ok(availabilityScheduleService.find(principal.id()));
    }

    @Override
    @PutMapping
    public ResponseEntity<WeeklyScheduleResponse> replace(
            @AuthenticationPrincipal ProviderPrincipal principal,
            @RequestBody @Valid WeeklyScheduleRequest request
    ) {
        return ResponseEntity.ok(availabilityScheduleService.replace(request, principal.id()));
    }
}
