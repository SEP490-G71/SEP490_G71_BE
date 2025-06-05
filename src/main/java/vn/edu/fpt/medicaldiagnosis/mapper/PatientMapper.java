package vn.edu.fpt.medicaldiagnosis.mapper;

import org.mapstruct.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.PatientRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.PatientResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Patient;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface PatientMapper {
    Patient toPatient(PatientRequest request);

    PatientResponse toPatientResponse(Patient patient);

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updatePatient(@MappingTarget Patient patient, PatientRequest request);
}
