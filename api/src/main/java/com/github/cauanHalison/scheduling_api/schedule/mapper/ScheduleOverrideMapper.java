package com.github.cauanHalison.scheduling_api.schedule.mapper;

import com.github.cauanHalison.scheduling_api.schedule.domain.ScheduleOverrideEntity;
import com.github.cauanHalison.scheduling_api.schedule.dto.ScheduleOverrideRequest;
import com.github.cauanHalison.scheduling_api.schedule.dto.ScheduleOverrideResponse;
import org.springframework.stereotype.Component;

@Component
public class ScheduleOverrideMapper {
    public ScheduleOverrideEntity toEntity(ScheduleOverrideRequest request) {
        ScheduleOverrideEntity entity = new ScheduleOverrideEntity();
        entity.setStartTime(request.startTime());
        entity.setEndTime(request.endTime());
        if (request.reason() != null) {
            entity.setReason(request.reason());
        }
        return entity;
    };

    public ScheduleOverrideResponse toResponse(ScheduleOverrideEntity entity) {
        return new ScheduleOverrideResponse(
                entity.getId(),
                entity.getStartTime(),
                entity.getEndTime(),
                entity.getReason(),
                entity.getCreatedAt()
        );
    }
}
