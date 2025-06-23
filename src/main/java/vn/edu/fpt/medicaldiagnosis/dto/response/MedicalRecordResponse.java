package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MedicalRecordResponse {
    private String id;
    private String patientName;
    private String diagnosisText;
    private String summary;
    private String status;
    private List<MedicalOrderResponse> orders;
}

