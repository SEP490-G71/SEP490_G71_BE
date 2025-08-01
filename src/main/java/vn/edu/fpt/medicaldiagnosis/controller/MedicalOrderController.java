package vn.edu.fpt.medicaldiagnosis.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.medicaldiagnosis.dto.request.UpdateMedicalOrderStatusRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.MedicalOrderStatusResponse;
import vn.edu.fpt.medicaldiagnosis.service.MedicalOrderService;

import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/medical-orders")
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class MedicalOrderController {
    MedicalOrderService medicalOrderService;

    @PutMapping("/status")
    ApiResponse<MedicalOrderStatusResponse> updateMedicalOrderStatus(
            @RequestBody @Valid UpdateMedicalOrderStatusRequest request
    ) {
        MedicalOrderStatusResponse response = medicalOrderService.updateMedicalOrderStatus(request);
        return ApiResponse.<MedicalOrderStatusResponse>builder()
//                .message("Update medical order status successfully")
                .result(response)
                .build();
    }
}
