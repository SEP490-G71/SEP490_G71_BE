package vn.edu.fpt.medicaldiagnosis.service.impl;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.dto.request.MedicalRequestDTO;
import vn.edu.fpt.medicaldiagnosis.dto.response.MedicalResponseDTO;
import vn.edu.fpt.medicaldiagnosis.entity.*;
import vn.edu.fpt.medicaldiagnosis.enums.InvoiceStatus;
import vn.edu.fpt.medicaldiagnosis.enums.MedicalOrderStatus;
import vn.edu.fpt.medicaldiagnosis.enums.PaymentType;
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
    @Override
    public MedicalResponseDTO createMedicalRecord(MedicalRequestDTO request) {
        log.info("Service: create medical record");
        log.info("Request: {}", request);

        // 1. Validate and fetch patient & staff
        Patient patient = patientRepository.findByIdAndDeletedAtIsNull(request.getPatientId())
                .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND));
        Staff staff = staffRepository.findByIdAndDeletedAtIsNull(request.getStaffId())
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        // 2. Create MedicalRecord
        MedicalRecord record = MedicalRecord.builder()
                .patient(patient)
                .createdBy(staff)
                .diagnosisText(request.getDiagnosisText())
                .summary("")
                .build();
        record = medicalRecordRepository.save(record);

        // 3. Create Invoice (chưa cần paymentType)
        Invoice invoice = Invoice.builder()
                .patient(patient)
                .status(InvoiceStatus.UNPAID)
                .amount(BigDecimal.ZERO) // sẽ cập nhật sau
                .build();
        invoice = invoiceRepository.save(invoice);

        List<String> medicalOrderIds = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (MedicalRequestDTO.ServiceRequest s : request.getServices()) {
            MedicalService service = medicalServiceRepository.findByIdAndDeletedAtIsNull(s.getServiceId())
                    .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_SERVICE_NOT_FOUND));

            // Tính toán giá
            BigDecimal quantity = BigDecimal.valueOf(s.getQuantity());
            BigDecimal price = service.getPrice();
            BigDecimal discount = service.getDiscount() != null ? service.getDiscount() : BigDecimal.ZERO;
            BigDecimal vat = service.getVat() != null ? service.getVat() : BigDecimal.ZERO;

            BigDecimal discounted = price.subtract(discount);
            BigDecimal subtotal = discounted.multiply(quantity);
            BigDecimal total = subtotal.add(subtotal.multiply(vat).divide(BigDecimal.valueOf(100)));

            totalAmount = totalAmount.add(total);

            // 4. Create InvoiceItem
            InvoiceItem item = InvoiceItem.builder()
                    .invoice(invoice)
                    .service(service)
                    .name(service.getName())
                    .quantity(s.getQuantity())
                    .price(price)
                    .discount(discount)
                    .vat(vat)
                    .total(total)
                    .build();
            item = invoiceItemRepository.save(item);

            // 5. Create MedicalOrder
            MedicalOrder order = MedicalOrder.builder()
                    .medicalRecord(record)
                    .service(service)
                    .invoiceItem(item)
                    .createdBy(staff)
                    .status(MedicalOrderStatus.ORDERED)
                    .build();
            order = medicalOrderRepository.save(order);
            medicalOrderIds.add(order.getId());
        }

        // 6. Update invoice total
        invoice.setAmount(totalAmount);
        invoiceRepository.save(invoice);

        // 7. Return response
        return MedicalResponseDTO.builder()
                .medicalRecordId(record.getId())
                .invoiceId(invoice.getId())
                .totalAmount(totalAmount)
                .medicalOrderIds(medicalOrderIds)
                .build();
    }
}
