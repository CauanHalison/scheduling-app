package com.github.cauanHalison.scheduling_api.schedule.service;

import com.github.cauanHalison.scheduling_api.exception.exceptions.ResourceNotFoundException;
import com.github.cauanHalison.scheduling_api.identity.domain.ProviderEntity;
import com.github.cauanHalison.scheduling_api.identity.repository.ProviderRepository;
import com.github.cauanHalison.scheduling_api.schedule.domain.AvailabilityScheduleEntity;
import com.github.cauanHalison.scheduling_api.schedule.dto.WeeklyScheduleRequest;
import com.github.cauanHalison.scheduling_api.schedule.dto.WeeklyScheduleResponse;
import com.github.cauanHalison.scheduling_api.schedule.mapper.AvailabilityScheduleMapper;
import com.github.cauanHalison.scheduling_api.schedule.repository.AvailabilityScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvailabilityScheduleService {
    private final AvailabilityScheduleRepository availabilityScheduleRepository;
    private final AvailabilityScheduleMapper availabilityScheduleMapper;
    private final ProviderRepository providerRepository;

    @Transactional(readOnly = true)
    public WeeklyScheduleResponse find(UUID providerId) {
        ProviderEntity provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Prestador não encontrado na base de dados"));

        List<AvailabilityScheduleEntity> entities = availabilityScheduleRepository.findAllByProviderId(providerId);

        return availabilityScheduleMapper.toResponse(provider.getTimeZone(), entities);
    }

    @Transactional
    public WeeklyScheduleResponse replace(WeeklyScheduleRequest request, UUID providerId) {
        ProviderEntity provider = providerRepository.findByIdForUpdate(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Prestador não encontrado na base de dados"));
        String timeZone = provider.getTimeZone();

        availabilityScheduleRepository.deleteAllByProviderId(providerId);

        List<AvailabilityScheduleEntity> saved = availabilityScheduleRepository.saveAllAndFlush(
                availabilityScheduleMapper.toEntities(request, provider)
        );

        log.info("Weekly availability schedule replaced successfully: providerId={}, intervals={}",
                providerId, saved.size());
        return availabilityScheduleMapper.toResponse(timeZone, saved);
    }
}
