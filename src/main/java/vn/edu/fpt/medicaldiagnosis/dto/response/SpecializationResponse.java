package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SpecializationResponse {
    private String id;
    private String name;
    private String description;
}