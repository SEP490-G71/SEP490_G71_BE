package vn.edu.fpt.medicaldiagnosis.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import vn.edu.fpt.medicaldiagnosis.dto.request.DailyQueueRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.DailyQueueResponse;
import vn.edu.fpt.medicaldiagnosis.entity.DailyQueue;


@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)

public interface DailyQueueMapper {
    DailyQueue toEntity(DailyQueueRequest request);

    DailyQueueResponse toResponse(DailyQueue dailyQueue);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void update(@MappingTarget DailyQueue dailyQueue, DailyQueueRequest request);
}
