package com.github.cauanHalison.scheduling_api.schedule.repository;


import com.github.cauanHalison.scheduling_api.schedule.domain.ScheduleOverrideEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleOverrideRepository extends JpaRepository<ScheduleOverrideEntity, UUID> {
    Optional<ScheduleOverrideEntity>  findByIdAndProviderId(UUID id, UUID providerId);

    @Query("""
            SELECT o FROM ScheduleOverrideEntity o
            WHERE o.provider.id = :providerId
            AND o.endTime > :now
            ORDER BY o.startTime ASC
            """)
    List<ScheduleOverrideEntity> findAllNextOverrides(
            @Param("providerId") UUID providerId,
            @Param("now") Instant now
    );

    @Query("""
        SELECT o FROM ScheduleOverrideEntity o
        WHERE :providerId = o.provider.id
        AND :endRange > o.startTime
        AND :startRange < o.endTime
        ORDER BY o.startTime ASC
        """)
    List<ScheduleOverrideEntity> findOverlapping(
            @Param("providerId") UUID providerId,
            @Param("startRange") Instant startRange,
            @Param("endRange") Instant endRange
    );
}
