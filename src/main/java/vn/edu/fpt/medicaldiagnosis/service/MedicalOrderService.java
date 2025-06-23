package vn.edu.fpt.medicaldiagnosis.service;

import vn.edu.fpt.medicaldiagnosis.dto.request.UpdateMedicalOrderStatusRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.MedicalOrderStatusResponse;

public interface MedicalOrderService {
    MedicalOrderStatusResponse updateMedicalOrderStatus(UpdateMedicalOrderStatusRequest request);
}
