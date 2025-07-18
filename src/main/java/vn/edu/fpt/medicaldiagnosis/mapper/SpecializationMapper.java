package vn.edu.fpt.medicaldiagnosis.mapper;

import org.mapstruct.Mapper;
import vn.edu.fpt.medicaldiagnosis.dto.response.SpecializationResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Specialization;

@Mapper(componentModel = "spring")
public interface SpecializationMapper {
    SpecializationResponse toResponse(Specialization specialization);
}
