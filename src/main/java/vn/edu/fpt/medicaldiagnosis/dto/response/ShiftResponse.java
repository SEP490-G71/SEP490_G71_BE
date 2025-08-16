package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShiftResponse {
    private String id;
    private String name;
    private LocalTime startTime;
    private LocalTime endTime;
    private String description;
}

