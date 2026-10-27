package com.github.cauanHalison.scheduling_api.eventtype.service;

import com.github.cauanHalison.scheduling_api.config.cache.CacheConfiguration;
import com.github.cauanHalison.scheduling_api.eventtype.domain.EventTypeEntity;
import com.github.cauanHalison.scheduling_api.eventtype.dto.EventTypeRequest;
import com.github.cauanHalison.scheduling_api.eventtype.dto.EventTypeResponse;
import com.github.cauanHalison.scheduling_api.eventtype.mapper.EventTypeMapper;
import com.github.cauanHalison.scheduling_api.eventtype.repository.EventTypeRepository;
import com.github.cauanHalison.scheduling_api.exception.exceptions.ResourceConflictException;
import com.github.cauanHalison.scheduling_api.exception.exceptions.ResourceNotFoundException;
import com.github.cauanHalison.scheduling_api.identity.domain.ProviderEntity;
import com.github.cauanHalison.scheduling_api.identity.repository.ProviderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventTypeService {
    private final EventTypeRepository eventTypeRepository;
    private final ProviderRepository providerRepository;
    private final EventTypeMapper eventTypeMapper;

    @Transactional
    @CacheEvict(value = CacheConfiguration.EVENT_TYPES_CACHE, key = "#providerId")
    public EventTypeResponse create(EventTypeRequest eventTypeRequest, UUID providerId) {
        if (eventTypeRepository.existsByProviderIdAndSlug(providerId, eventTypeRequest.slug())) {
            throw new ResourceConflictException("Este slug já está em uso.");
        }

        ProviderEntity providerEntity = providerRepository.getReferenceById(providerId);
        EventTypeEntity entity = eventTypeMapper.toEntity(eventTypeRequest);

        entity.setProvider(providerEntity);

        EventTypeEntity savedEntity = eventTypeRepository.save(entity);

        log.info("Event type created successfully: id={}, providerId={}, slug={}",
                savedEntity.getId(),
                providerId,
                savedEntity.getSlug()
        );
        return eventTypeMapper.toResponse(savedEntity);
    }

    @Transactional
    @CacheEvict(value = CacheConfiguration.EVENT_TYPES_CACHE, key = "#providerId")
    public EventTypeResponse update(UUID eventTypeId, EventTypeRequest request, UUID providerId) {
        EventTypeEntity entity = eventTypeRepository.findByIdAndProviderId(eventTypeId, providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de evento não encontrado."));

        if (eventTypeRepository.existsByProviderIdAndSlugAndIdNot(providerId, request.slug(), eventTypeId)) {
            throw new ResourceConflictException("Este slug já está em uso em outro evento.");
        }

        eventTypeMapper.updateEntity(entity, request);
        EventTypeEntity updated = eventTypeRepository.save(entity);

        log.info("Event type updated successfully: id={}, providerId={}",
                updated.getId(),
                providerId
        );
        return eventTypeMapper.toResponse(updated);
    }

    @Transactional
    @CacheEvict(value = CacheConfiguration.EVENT_TYPES_CACHE, key = "#providerId")
    public EventTypeResponse toggleIsActive(UUID eventTypeId, UUID providerId) {
        EventTypeEntity entity = eventTypeRepository.findByIdAndProviderId(eventTypeId, providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de evento não encontrado."));

        entity.setIsActive(!entity.getIsActive());
        EventTypeEntity updated = eventTypeRepository.save(entity);

        log.info("Event type active status toggled: id={}, providerId={}, isActive={}",
                updated.getId(),
                providerId,
                updated.getIsActive()
        );

        return eventTypeMapper.toResponse(updated);
    }

    @Transactional
    @CacheEvict(value = CacheConfiguration.EVENT_TYPES_CACHE, key = "#providerId")
    public void delete(UUID eventTypeId, UUID providerId) {
        EventTypeEntity entity = eventTypeRepository.findByIdAndProviderId(eventTypeId, providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de evento não encontrado."));

        log.info("Event type deleted successfully: id={}, providerId={}",
                eventTypeId,
                providerId
        );
        eventTypeRepository.delete(entity);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfiguration.EVENT_TYPES_CACHE, key = "#providerId")
    public List<EventTypeResponse> findAll(UUID providerId) {
        List<EventTypeEntity> events = eventTypeRepository.findAllByProviderIdOrderByCreatedAtDesc(providerId);

        return events.stream().map(
                eventTypeMapper::toResponse
        ).toList();
    }
}
