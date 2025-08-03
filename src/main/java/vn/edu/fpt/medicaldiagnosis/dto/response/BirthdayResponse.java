package vn.edu.fpt.medicaldiagnosis.dto.response;


import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class BirthdayResponse {
    private String fullName;
    private String patientCode;
    private String email;
    private LocalDate dob;
    private String phone;
}
