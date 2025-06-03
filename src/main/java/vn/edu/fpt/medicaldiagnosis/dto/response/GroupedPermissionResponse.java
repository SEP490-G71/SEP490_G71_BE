package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GroupedPermissionResponse {
    private String groupName;
    private List<PermissionResponse> permissions;
}
