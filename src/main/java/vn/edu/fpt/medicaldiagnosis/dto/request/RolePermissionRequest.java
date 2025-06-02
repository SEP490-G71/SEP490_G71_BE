package vn.edu.fpt.medicaldiagnosis.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermissionRequest {
    private String roleName;
    private Set<String> permissions;
}
