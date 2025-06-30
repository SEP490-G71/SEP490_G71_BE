package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MedicalResultResponse {
    private String id;
    private String imageUrl;
    private String note;
}

