package vn.edu.fpt.medicaldiagnosis.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DepartmentTypeRequest {
    @NotBlank(message = "DEPARTMENT_TYPE_NAME_EMPTY")
    @Size(min = 3, max = 100, message = "DEPARTMENT_TYPE_NAME_LENGTH")
    private String name;

    @Size(min = 3, max = 500, message = "DEPARTMENT_TYPE_DESCRIPTION_LENGTH")
    private String description;
}
