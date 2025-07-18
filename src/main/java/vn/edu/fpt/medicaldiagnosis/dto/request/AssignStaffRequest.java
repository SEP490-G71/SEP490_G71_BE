package vn.edu.fpt.medicaldiagnosis.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AssignStaffRequest {
    @NotEmpty
    List<String> staffIds;
}
