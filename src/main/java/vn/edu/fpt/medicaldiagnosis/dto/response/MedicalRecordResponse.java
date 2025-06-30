package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MedicalRecordResponse {
    private String id;
    private String medicalRecordCode;
    private String patientName;
    private String doctorName;
    private String status;
    private LocalDateTime createdAt;
}
