package vn.edu.fpt.medicaldiagnosis.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkUpdateWorkScheduleRequest {
    private List<String> idsToDelete;

    private List<UpdateWorkScheduleRequest> newSchedules;
}

