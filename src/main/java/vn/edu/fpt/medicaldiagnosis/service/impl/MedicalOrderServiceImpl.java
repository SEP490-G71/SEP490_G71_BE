package vn.edu.fpt.medicaldiagnosis.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.dto.request.UpdateMedicalOrderStatusRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.MedicalOrderStatusResponse;
import vn.edu.fpt.medicaldiagnosis.entity.MedicalOrder;
import vn.edu.fpt.medicaldiagnosis.entity.MedicalRecord;
import vn.edu.fpt.medicaldiagnosis.enums.MedicalOrderStatus;
import vn.edu.fpt.medicaldiagnosis.enums.MedicalRecordStatus;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.repository.MedicalOrderRepository;
import vn.edu.fpt.medicaldiagnosis.repository.MedicalRecordRepository;
import vn.edu.fpt.medicaldiagnosis.service.MedicalOrderService;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MedicalOrderServiceImpl implements MedicalOrderService {
    MedicalOrderRepository medicalOrderRepository;
    MedicalRecordRepository medicalRecordRepository;
    @Override
    public MedicalOrderStatusResponse updateMedicalOrderStatus(UpdateMedicalOrderStatusRequest request) {
        log.info("Service: update medical order status");

        MedicalOrder order = medicalOrderRepository.findByIdAndDeletedAtIsNull(request.getMedicalOrderId())
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_ORDER_NOT_FOUND));

        order.setStatus(request.getStatus());
        medicalOrderRepository.save(order);

        MedicalRecord record = order.getMedicalRecord();
        List<MedicalOrder> relatedOrders = medicalOrderRepository
                .findAllByMedicalRecordIdAndDeletedAtIsNull(record.getId());

        boolean allCompleted = relatedOrders.stream()
                .allMatch(o -> o.getStatus() == MedicalOrderStatus.COMPLETED);

        if (allCompleted && record.getStatus() != MedicalRecordStatus.TESTING_COMPLETED) {
            record.setStatus(MedicalRecordStatus.TESTING_COMPLETED);
            medicalRecordRepository.save(record);
        }

        return MedicalOrderStatusResponse.builder()
                .medicalOrderId(order.getId())
                .orderStatus(order.getStatus())
                .medicalRecordId(record.getId())
                .recordStatus(record.getStatus())
                .allOrdersCompleted(allCompleted)
                .build();
    }
}
