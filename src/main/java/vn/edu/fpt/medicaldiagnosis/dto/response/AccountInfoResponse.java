package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class AccountInfoResponse {
    private String accountId;
    private String userId;
    private String username;
    private Set<String> roles;
    private Set<String> permissions;
}