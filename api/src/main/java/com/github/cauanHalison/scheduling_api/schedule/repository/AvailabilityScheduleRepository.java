package com.github.cauanHalison.scheduling_api.schedule.repository;

import com.github.cauanHalison.scheduling_api.schedule.domain.AvailabilityScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AvailabilityScheduleRepository extends JpaRepository<AvailabilityScheduleEntity, UUID> {

    List<AvailabilityScheduleEntity> findAllByProviderId(UUID providerId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM AvailabilityScheduleEntity s WHERE s.provider.id = :providerId")
    void deleteAllByProviderId(@Param("providerId") UUID providerId);
}
