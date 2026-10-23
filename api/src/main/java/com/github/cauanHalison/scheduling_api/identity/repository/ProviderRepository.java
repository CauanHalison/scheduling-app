package com.github.cauanHalison.scheduling_api.identity.repository;

import com.github.cauanHalison.scheduling_api.identity.domain.ProviderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderRepository extends JpaRepository<ProviderEntity, UUID> {
    Optional<ProviderEntity> findById(UUID id);

    Optional<ProviderEntity> findByEmail(String email);

    Optional<ProviderEntity> findBySlug(String slug);

    boolean existsByEmail(String email);

    boolean existsBySlug(String slug);
}
