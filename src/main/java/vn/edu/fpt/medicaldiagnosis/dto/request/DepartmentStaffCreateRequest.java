package vn.edu.fpt.medicaldiagnosis.dto.request;


import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.fpt.medicaldiagnosis.enums.Position;

import java.util.List;
import java.util.UUID;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentStaffCreateRequest {
    @NotNull(message = "departmentId is required")
    private String departmentId;

    private List<StaffPosition> staffPositions;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StaffPosition {
        @NotNull(message = "staffId is required")
        private String staffId;

        @NotNull(message = "position is required")
        private Position position;
    }
}
