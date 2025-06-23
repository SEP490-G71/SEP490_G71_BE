package vn.edu.fpt.medicaldiagnosis.service.impl;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.dto.request.MedicalRequestDTO;
import vn.edu.fpt.medicaldiagnosis.dto.response.MedicalOrderResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.MedicalRecordResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.MedicalResponseDTO;
import vn.edu.fpt.medicaldiagnosis.dto.response.MedicalResultResponse;
import vn.edu.fpt.medicaldiagnosis.entity.*;
import vn.edu.fpt.medicaldiagnosis.enums.InvoiceStatus;
import vn.edu.fpt.medicaldiagnosis.enums.MedicalOrderStatus;
import vn.edu.fpt.medicaldiagnosis.enums.MedicalRecordStatus;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.repository.*;
import vn.edu.fpt.medicaldiagnosis.service.MedicalRecordService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class MedicalRecordServiceImpl implements MedicalRecordService {
    MedicalRecordRepository medicalRecordRepository;
    MedicalOrderRepository medicalOrderRepository;
    MedicalServiceRepository medicalServiceRepository;
    InvoiceRepository invoiceRepository;
    InvoiceItemRepository invoiceItemRepository;
    PatientRepository patientRepository;
    StaffRepository staffRepository;
    CodeGeneratorService codeGeneratorService;
    MedicalResultRepository medicalResultRepository;

    @Override
    public MedicalResponseDTO createMedicalRecord(MedicalRequestDTO request) {
        log.info("Service: create medical record");
        log.info("Request: {}", request);

        // 1. Validate and fetch patient & staff
        Patient patient = patientRepository.findByIdAndDeletedAtIsNull(request.getPatientId())
                .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND));
        Staff staff = staffRepository.findByIdAndDeletedAtIsNull(request.getStaffId())
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        // 2. Generate medical record code
        String medicalRecordCode = codeGeneratorService.generateCode("MEDICAL_RECORD", "MR-", 6);

        // 3. Create MedicalRecord
        MedicalRecord record = MedicalRecord.builder()
                .medicalRecordCode(medicalRecordCode)
                .patient(patient)
                .createdBy(staff)
                .diagnosisText(request.getDiagnosisText())
                .summary("")
                .status(MedicalRecordStatus.WAITING_FOR_PAYMENT)
                .build();
        record = medicalRecordRepository.save(record);

        // 4. Generate invoice code
        String invoiceCode = codeGeneratorService.generateCode("INVOICE", "INV-", 6);

        // 5. Create Invoice
        Invoice invoice = Invoice.builder()
                .invoiceCode(invoiceCode)
                .patient(patient)
                .status(InvoiceStatus.UNPAID)
                .amount(BigDecimal.ZERO)
                .build();
        invoice = invoiceRepository.save(invoice);

        List<String> medicalOrderIds = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        // 6. Loop through each service
        for (MedicalRequestDTO.ServiceRequest s : request.getServices()) {
            MedicalService service = medicalServiceRepository.findByIdAndDeletedAtIsNull(s.getServiceId())
                    .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_SERVICE_NOT_FOUND));

            int quantity = s.getQuantity();
            BigDecimal price = service.getPrice();
            BigDecimal discount = service.getDiscount() != null ? service.getDiscount() : BigDecimal.ZERO;
            BigDecimal vat = service.getVat() != null ? service.getVat() : BigDecimal.ZERO;

            BigDecimal discounted = price.subtract(discount);
            BigDecimal subtotal = discounted.multiply(BigDecimal.valueOf(quantity));
            BigDecimal total = subtotal.add(subtotal.multiply(vat).divide(BigDecimal.valueOf(100)));

            totalAmount = totalAmount.add(total);

            // 7. Create InvoiceItem (1 dòng, tổng quantity)
            InvoiceItem item = InvoiceItem.builder()
                    .invoice(invoice)
                    .service(service)
                    .name(service.getName())
                    .quantity(quantity)
                    .price(price)
                    .discount(discount)
                    .vat(vat)
                    .total(total)
                    .build();
            item = invoiceItemRepository.save(item);

            // 8. Tách MedicalOrder cho từng lượt chỉ định (quantity > 1 → nhiều order)
            for (int i = 0; i < quantity; i++) {
                MedicalOrder order = MedicalOrder.builder()
                        .medicalRecord(record)
                        .service(service)
                        .invoiceItem(item)
                        .createdBy(staff)
                        .status(MedicalOrderStatus.PENDING)
                        .build();
                order = medicalOrderRepository.save(order);
                medicalOrderIds.add(order.getId());
            }
        }

        // 9. Update invoice total
        invoice.setAmount(totalAmount);
        invoiceRepository.save(invoice);

        // 10. Return response
        return MedicalResponseDTO.builder()
                .medicalRecordId(record.getId())
                .invoiceId(invoice.getId())
                .totalAmount(totalAmount)
                .medicalOrderIds(medicalOrderIds)
                .build();
    }

    @Override
    public MedicalRecordResponse getMedicalRecordDetail(String recordId) {
        MedicalRecord record = medicalRecordRepository.findByIdAndDeletedAtIsNull(recordId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));

        List<MedicalOrder> orders = medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull(recordId);

        List<MedicalOrderResponse> orderDTOs = orders.stream().map(order -> {
            List<MedicalResult> results = medicalResultRepository
                    .findAllByMedicalOrderIdAndDeletedAtIsNull(order.getId());

            List<MedicalResultResponse> resultDTOs = results.stream().map(result -> MedicalResultResponse.builder()
                    .id(result.getId())
                    .imageUrl(result.getResultImageUrl())
                    .note(result.getResultNote())
                    .completedBy(result.getCompletedBy() != null ? result.getCompletedBy().getFullName() : null)
                    .build()).toList();

            return MedicalOrderResponse.builder()
                    .id(order.getId())
                    .serviceName(order.getService().getName())
                    .status(order.getStatus().name())
                    .results(resultDTOs)
                    .build();
        }).toList();

        return MedicalRecordResponse.builder()
                .id(record.getId())
                .patientName(record.getPatient().getFullName())
                .diagnosisText(record.getDiagnosisText())
                .summary(record.getSummary())
                .status(record.getStatus().name())
                .orders(orderDTOs)
                .build();
    }
}
