package vn.edu.fpt.medicaldiagnosis.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import vn.edu.fpt.medicaldiagnosis.dto.response.MedicalRecordResponse;
import vn.edu.fpt.medicaldiagnosis.entity.MedicalRecord;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface MedicalRecordMapper {

    @Mapping(target = "patientName", expression = "java(getPatientName(medicalRecord))")
    @Mapping(target = "doctorName", expression = "java(getDoctorName(medicalRecord))")
    MedicalRecordResponse toMedicalRecordResponse(MedicalRecord medicalRecord);

    default String getPatientName(MedicalRecord record) {
        if (record.getPatient() == null) return null;
        return record.getPatient().getFullName();
    }

    default String getDoctorName(MedicalRecord record) {
        if (record.getCreatedBy() == null) return null;
        return record.getCreatedBy().getFullName();
    }
}
