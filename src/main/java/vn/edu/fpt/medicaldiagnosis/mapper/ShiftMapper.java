package vn.edu.fpt.medicaldiagnosis.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import vn.edu.fpt.medicaldiagnosis.dto.request.ShiftRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ShiftResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Shift;

import java.util.List;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface ShiftMapper {

    ShiftResponse toResponse(Shift shift);

    List<ShiftResponse> toResponseList(List<Shift> shifts);

    Shift toEntity(ShiftRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromRequest(ShiftRequest request, @MappingTarget Shift shift);
}