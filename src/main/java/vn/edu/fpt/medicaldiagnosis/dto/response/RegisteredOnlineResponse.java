package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class RegisteredOnlineResponse {
    private String id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private LocalDateTime registeredAt;
    private String message;
    private Integer visitCount;
    private LocalDateTime createdAt;
}
