package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomTransferResponsePagination {
    private String id;
    private MedicalRecordResponse medicalRecord;
    private DepartmentBasicInfo fromDepartment;
    private DepartmentBasicInfo toDepartment;
    private StaffBasicResponse transferredBy;
    private LocalDateTime transferTime;
    private String reason;
    private StaffBasicResponse doctor;
    private String conclusionText;
    private Boolean isFinal;
}

