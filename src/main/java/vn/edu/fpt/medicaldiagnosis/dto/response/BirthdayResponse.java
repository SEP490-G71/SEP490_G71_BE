package vn.edu.fpt.medicaldiagnosis.dto.response;


import lombok.Data;

@Data
public class BirthdayResponse {
    private String fullName;
    private String patientCode;
    private String email;
    private String dob;
    private String phone;
}
