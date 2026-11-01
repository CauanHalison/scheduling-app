package com.github.cauanHalison.scheduling_api.identity.repository;

import com.github.cauanHalison.scheduling_api.identity.domain.ProviderEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProviderRepository extends JpaRepository<ProviderEntity, UUID> {
    Optional<ProviderEntity> findById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProviderEntity p WHERE p.id = :id")
    Optional<ProviderEntity> findByIdForUpdate(@Param("id") UUID id);

    Optional<ProviderEntity> findByEmail(String email);

    Optional<ProviderEntity> findBySlug(String slug);

    boolean existsByEmail(String email);

    boolean existsBySlug(String slug);
}
