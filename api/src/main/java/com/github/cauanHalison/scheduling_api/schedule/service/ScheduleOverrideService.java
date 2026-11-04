package com.github.cauanHalison.scheduling_api.schedule.service;

import com.github.cauanHalison.scheduling_api.exception.exceptions.ResourceNotFoundException;
import com.github.cauanHalison.scheduling_api.identity.domain.ProviderEntity;
import com.github.cauanHalison.scheduling_api.identity.repository.ProviderRepository;
import com.github.cauanHalison.scheduling_api.schedule.domain.ScheduleOverrideEntity;
import com.github.cauanHalison.scheduling_api.schedule.dto.ScheduleOverrideRequest;
import com.github.cauanHalison.scheduling_api.schedule.dto.ScheduleOverrideResponse;
import com.github.cauanHalison.scheduling_api.schedule.mapper.ScheduleOverrideMapper;
import com.github.cauanHalison.scheduling_api.schedule.repository.ScheduleOverrideRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduleOverrideService {
    private final ScheduleOverrideRepository scheduleOverrideRepository;
    private final ScheduleOverrideMapper scheduleOverrideMapper;
    private final ProviderRepository providerRepository;

    @Transactional(readOnly = true)
    public List<ScheduleOverrideResponse> findAllFuture(UUID providerId) {
        List<ScheduleOverrideEntity> nextOverrides = scheduleOverrideRepository.findAllNextOverrides(providerId, Instant.now());

        return nextOverrides.stream().map(scheduleOverrideMapper::toResponse).toList();
    }

    @Transactional
    public ScheduleOverrideResponse create(ScheduleOverrideRequest request, UUID providerId) {
        ProviderEntity provider = providerRepository.getReferenceById(providerId);

        ScheduleOverrideEntity entity = scheduleOverrideMapper.toEntity(request);
        entity.setProvider(provider);

        ScheduleOverrideEntity savedEntity = scheduleOverrideRepository.saveAndFlush(entity);
        log.info("Schedule override created successfully: id: {}, providerId: {}",
                savedEntity.getId(),
                providerId
        );
        return scheduleOverrideMapper.toResponse(savedEntity);
    }

    @Transactional
    public void delete(UUID overrideId, UUID providerId) {
        ScheduleOverrideEntity entity = scheduleOverrideRepository.findByIdAndProviderId(overrideId, providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Exceção não encontrada na base de dados"));

        scheduleOverrideRepository.delete(entity);
        log.info("Schedule override deleted successfully: id={}, providerId={}",
                overrideId,
                providerId
        );
    }
}
