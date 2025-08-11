package vn.edu.fpt.medicaldiagnosis.dto.request;

import lombok.Data;

@Data
public class RoomTransferUpdateRequestDTO {
    private String reason;         // optional
    private String doctorId;       // optional
    private String conclusionText; // optional
    private Boolean isFinal;       // optional
}
