package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.Builder;
import lombok.Data;
import vn.edu.fpt.medicaldiagnosis.enums.SatisfactionLevel;

import java.time.LocalDateTime;

@Data
@Builder
public class StaffFeedbackResponse {
    private String id;
    private String doctorId;
    private String doctorName;
    private String patientId;
    private String patientName;
    private String medicalRecordId;
    private SatisfactionLevel satisfactionLevel;
    private String comment;
    private LocalDateTime createdAt;
}
