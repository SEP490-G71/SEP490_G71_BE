package vn.edu.fpt.medicaldiagnosis.dto.request;

import lombok.Data;

import java.util.List;

@Data
public class AssignRolesRequest {
    private List<String> roleNames;
}
