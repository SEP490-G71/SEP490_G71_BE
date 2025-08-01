package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class MedicalResultResponse {
    private String id;
    private String completedBy;
    private List<MedicalResultImageResponse> imageUrls;
    private String note;
    private String description;
}

