package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.Builder;
import lombok.Data;
import vn.edu.fpt.medicaldiagnosis.enums.Gender;
import vn.edu.fpt.medicaldiagnosis.enums.Status;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class RegisteredOnlineResponse {
    private String id;

    private String firstName;
    private String middleName;
    private String lastName;
    private LocalDate dob;
    private Gender gender;

    private String fullName;
    private String email;
    private String phoneNumber;
    private LocalDateTime registeredAt;
    private String message;
    private Integer visitCount;
    private Status status;

    private LocalDateTime createdAt;
}
