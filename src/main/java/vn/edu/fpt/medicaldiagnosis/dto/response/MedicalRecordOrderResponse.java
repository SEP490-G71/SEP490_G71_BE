package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.Builder;
import lombok.Data;
import vn.edu.fpt.medicaldiagnosis.enums.MedicalOrderStatus;

import java.time.LocalDateTime;

@Data
@Builder
public class MedicalRecordOrderResponse {
    private String orderId;
    private String medicalRecordId;
    private String medicalRecordCode;
    private String patientName;
    private String serviceName;
    private MedicalOrderStatus status;
    private LocalDateTime createdAt;
}

