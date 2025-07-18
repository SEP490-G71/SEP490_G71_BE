package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.fpt.medicaldiagnosis.enums.DepartmentType;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentDetailResponse {
    private String id;
    private String name;
    private String description;
    private String roomNumber;
    private DepartmentType type;
    private SpecializationResponse specialization;
    private List<StaffBasicResponse> staffs;
}

