package vn.edu.fpt.medicaldiagnosis.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import vn.edu.fpt.medicaldiagnosis.dto.request.QueuePatientsRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientCompactResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientsResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.StaffBasicResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.StaffResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Patient;
import vn.edu.fpt.medicaldiagnosis.entity.QueuePatients;

import java.util.Collections;
import java.util.UUID;


@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)

public interface QueuePatientsMapper {
    QueuePatients toEntity(QueuePatientsRequest request);

    QueuePatientsResponse toResponse(QueuePatients queuePatients);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void update(@MappingTarget QueuePatients queuePatients, QueuePatientsRequest request);

    default QueuePatientCompactResponse toCompactResponse(QueuePatients qp, Patient p) {
        return QueuePatientCompactResponse.builder()
                .id(qp.getId())
                .patientId(qp.getPatientId())
                .firstName(p.getFirstName())
                .middleName(p.getMiddleName())
                .lastName(p.getLastName())
                .fullName(p.getFullNameSafe())
                .patientCode(p.getPatientCode())
                .dob(p.getDob())
                .gender(p.getGender())
                .phone(p.getPhone())
                .email(p.getEmail())
                .type(qp.getType())
                .registeredTime(qp.getRegisteredTime())
                .roomNumber(qp.getRoomNumber())
                .queueOrder(qp.getQueueOrder())
                .isPriority(qp.getIsPriority())
                .specialization(qp.getSpecialization() != null ? qp.getSpecialization().getName() : null)
                .status(qp.getStatus())
                .checkinTime(qp.getCheckinTime())
                .checkoutTime(qp.getCheckoutTime())
                .calledTime(qp.getCalledTime())
                .awaitingResultTime(qp.getAwaitingResultTime())
                .staff(qp.getReceptionist() != null ?
                StaffBasicResponse.builder()
                        .id(qp.getReceptionist().getId())
                        .fullName(qp.getReceptionist().getFullName())
                        .staffCode(qp.getReceptionist().getStaffCode())
                        .roles(Collections.singletonList(""))
                        .build()
                : null)
                .build();
    }
}
