package vn.edu.fpt.medicaldiagnosis.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TransferRoomRequest {

    @NotBlank(message = "fromDeptId là bắt buộc")
    private String fromDeptId;

    @NotBlank(message = "toDeptId là bắt buộc")
    private String toDeptId;

    private String reason;
}
