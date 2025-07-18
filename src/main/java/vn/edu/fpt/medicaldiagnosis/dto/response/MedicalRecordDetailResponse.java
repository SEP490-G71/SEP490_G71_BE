package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MedicalRecordDetailResponse {
    private String id;
    private String medicalRecordCode;
    private String patientName;
    private String createdBy;
    private String diagnosisText;
    private String summary;
    private String status;
    private LocalDateTime createdAt;
    private List<MedicalOrderResponse> orders;
}

