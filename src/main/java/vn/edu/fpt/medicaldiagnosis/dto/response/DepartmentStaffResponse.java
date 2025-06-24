package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.fpt.medicaldiagnosis.enums.Position;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentStaffResponse {
    private UUID id;
    private UUID departmentId;
    private UUID staffId;
    private String departmentName; // Tên phòng (lấy từ entity Department)

    private String staffName; // Tên nhân viên (lấy từ entity Staff)

    private Position position;
}
