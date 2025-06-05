package vn.edu.fpt.medicaldiagnosis.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import vn.edu.fpt.medicaldiagnosis.dto.request.MedicalServiceRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.MedicalServiceResponse;
import vn.edu.fpt.medicaldiagnosis.entity.MedicalService;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MedicalServiceMapper {
    MedicalService toMedicalService(MedicalServiceRequest request);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "description", expression = "java(department.getDescription() == null ? \"\" : department.getDescription())")
    @Mapping(target = "department", source = "department")
    MedicalServiceResponse toMedicalServiceResponse(MedicalService department);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateMedicalService(@MappingTarget MedicalService medicalService, MedicalServiceRequest request);
}
