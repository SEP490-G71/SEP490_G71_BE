package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TenantResponse {
    private String id;
    private String name;
    private String code;
    private String status;
    private String email;
    private String phone;
    private String servicePackageName;
}
