package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.Builder;
import lombok.Data;
import vn.edu.fpt.medicaldiagnosis.enums.MedicalOrderStatus;
import vn.edu.fpt.medicaldiagnosis.enums.MedicalRecordStatus;

@Data
@Builder
public class MedicalOrderStatusResponse {
    private String medicalOrderId;
    private MedicalOrderStatus orderStatus;
    private String medicalRecordId;
    private MedicalRecordStatus recordStatus;
    private boolean allOrdersCompleted;
}
