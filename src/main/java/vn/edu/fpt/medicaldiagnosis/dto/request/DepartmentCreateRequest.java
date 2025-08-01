package vn.edu.fpt.medicaldiagnosis.dto.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.fpt.medicaldiagnosis.enums.DepartmentType;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentCreateRequest {

    @NotBlank(message = "DEPARTMENT_NAME_EMPTY")
    @Size(min = 3, max = 100, message = "DEPARTMENT_NAME_LENGTH")
    private String name;

    @Size(min = 3, max = 500, message = "DEPARTMENT_DESCRIPTION_LENGTH")
    private String description;

    @NotBlank(message = "DEPARTMENT_ROOM_EMPTY")
    @Pattern(regexp = "^[A-Za-z0-9]{2,5}$", message = "DEPARTMENT_ROOM_LENGTH")
    private String roomNumber;

    @NotNull(message = "DEPARTMENT_TYPE_EMPTY")
    private DepartmentType type;

    private String specializationId;

    @DecimalMin(value = "0.0", inclusive = true, message = "DEFAULT_SERVICE_PRICE_MIN")
    @Digits(integer = 15, fraction = 3, message = "DEFAULT_SERVICE_PRICE_INVALID_FORMAT")
    private BigDecimal defaultServicePrice;
}
