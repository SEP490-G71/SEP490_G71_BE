package vn.edu.fpt.medicaldiagnosis.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyQueueRequest {
    private LocalDateTime queueDate;
    private String status;
}
