package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderRow {
    public String SERVICE_NAME;
    public String CREATED_BY;
    public String COMPLETED_BY;
    public String DESCRIPTION;
    public String NOTE;
}
