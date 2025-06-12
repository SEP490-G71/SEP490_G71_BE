package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class DailyQueueResponse {
    private String id;
    private LocalDateTime queueDate;
    private String status;
}
