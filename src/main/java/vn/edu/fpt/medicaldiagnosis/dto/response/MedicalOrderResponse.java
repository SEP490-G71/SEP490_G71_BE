package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MedicalOrderResponse {
    private String id;
    private String serviceName;
    private String status;
    private String createdBy;
    private String completedBy;
    private List<MedicalResultResponse> results;
}
