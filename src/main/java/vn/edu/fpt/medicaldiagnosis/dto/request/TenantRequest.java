package vn.edu.fpt.medicaldiagnosis.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantRequest {
    private String name;
    private String code;
    private String email;
    private String phone;
    private String servicePackageId;
}
