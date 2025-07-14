package vn.edu.fpt.medicaldiagnosis.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettingRequest {
    @NotBlank(message = "HOSPITAL_NAME_REQUIRED")
    @Size(min = 3, max = 100, message = "HOSPITAL_NAME_LENGTH")
    private String hospitalName;

    @NotBlank(message = "HOSPITAL_PHONE_REQUIRED")
    @Size(min = 10, max = 10, message = "HOSPITAL_PHONE_LENGTH")
    private String hospitalPhone;

    @NotBlank(message = "HOSPITAL_EMAIL_REQUIRED")
    @Email(message = "HOSPITAL_EMAIL_INVALID")
    private String hospitalEmail;

    private String hospitalAddress;

    private String bankAccountNumber;

    private String bankCode;

    @NotEmpty(message = "PAGING_SIZE_REQUIRED")
    private List<Integer> paginationSizeList;
}

