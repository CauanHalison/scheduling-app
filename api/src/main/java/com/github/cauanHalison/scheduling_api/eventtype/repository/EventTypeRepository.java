package com.github.cauanHalison.scheduling_api.eventtype.repository;

import com.github.cauanHalison.scheduling_api.eventtype.domain.EventTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventTypeRepository extends JpaRepository<EventTypeEntity, UUID> {
    List<EventTypeEntity> findAllByProviderIdOrderByCreatedAtDesc(UUID providerId);
    Optional<EventTypeEntity> findByIdAndProviderId(UUID id, UUID providerId);
    boolean existsByProviderIdAndSlug(UUID providerId, String slug);
    boolean existsByProviderIdAndSlugAndIdNot(UUID providerId, String slug, UUID id);
}
