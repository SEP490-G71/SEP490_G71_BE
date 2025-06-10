package vn.edu.fpt.medicaldiagnosis.dto.request;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DailyQueueRequest {
    private LocalDateTime queueDate;
    private String status;
}
