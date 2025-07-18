package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalTime;

@Data
@Builder
public class ShiftResponse {
    private String id;
    private String name;
    private LocalTime startTime;
    private LocalTime endTime;
    private String description;
}

