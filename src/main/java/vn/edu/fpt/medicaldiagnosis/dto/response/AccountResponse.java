package vn.edu.fpt.medicaldiagnosis.dto.response;

import java.time.LocalDate;
import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountResponse {
    private String username;
    private String firstName;
    private String lastName;
    private LocalDate dob;
    private Set<RoleResponse> roles;
}
