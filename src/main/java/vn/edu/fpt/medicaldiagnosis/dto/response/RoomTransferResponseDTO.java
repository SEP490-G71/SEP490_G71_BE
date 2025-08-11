package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomTransferResponseDTO {
    private String id;
    private String medicalRecordId;
    private String fromDepartmentId;
    private String toDepartmentId;
    private String transferredById;
    private LocalDateTime transferTime;
    private String reason;
    private String doctorId;
    private String conclusionText;
    private Boolean isFinal;
}
