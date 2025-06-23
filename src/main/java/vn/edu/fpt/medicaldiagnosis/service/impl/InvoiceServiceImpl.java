package vn.edu.fpt.medicaldiagnosis.service.impl;

import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.dto.request.PayInvoiceRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.InvoiceResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Invoice;
import vn.edu.fpt.medicaldiagnosis.entity.MedicalOrder;
import vn.edu.fpt.medicaldiagnosis.entity.MedicalRecord;
import vn.edu.fpt.medicaldiagnosis.entity.Staff;
import vn.edu.fpt.medicaldiagnosis.enums.InvoiceStatus;
import vn.edu.fpt.medicaldiagnosis.enums.MedicalOrderStatus;
import vn.edu.fpt.medicaldiagnosis.enums.MedicalRecordStatus;
import vn.edu.fpt.medicaldiagnosis.enums.PaymentType;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.repository.InvoiceRepository;
import vn.edu.fpt.medicaldiagnosis.repository.MedicalOrderRepository;
import vn.edu.fpt.medicaldiagnosis.repository.MedicalRecordRepository;
import vn.edu.fpt.medicaldiagnosis.repository.StaffRepository;
import vn.edu.fpt.medicaldiagnosis.service.InvoiceService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    InvoiceRepository invoiceRepository;
    MedicalOrderRepository medicalOrderRepository;
    MedicalRecordRepository medicalRecordRepository;
    StaffRepository staffRepository;
    @Override
    public InvoiceResponse payInvoice(PayInvoiceRequest request) {
        log.info("Service: pay invoice");

        // 1. Lấy invoice và kiểm tra trạng thái
        Invoice invoice = invoiceRepository.findByIdAndDeletedAtIsNull(request.getInvoiceId())
                .orElseThrow(() -> new AppException(ErrorCode.INVOICE_NOT_FOUND));

        if (InvoiceStatus.PAID.equals(invoice.getStatus())) {
            throw new AppException(ErrorCode.INVOICE_ALREADY_PAID);
        }

        // 2. Lấy thông tin thu ngân xác nhận
        Staff staff = staffRepository.findByIdAndDeletedAtIsNull(request.getStaffId())
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        // 3. Cập nhật invoice
        invoice.setPaymentType(request.getPaymentType());
        invoice.setStatus(InvoiceStatus.PAID);
        invoice.setConfirmedBy(staff);
        invoice.setConfirmedAt(LocalDateTime.now());
        invoiceRepository.save(invoice);

        // 4. Cập nhật trạng thái các medical order liên quan
        List<MedicalOrder> orders =
                medicalOrderRepository.findAllByInvoiceItemInvoiceId(invoice.getId());

        Set<MedicalRecord> relatedRecords = new HashSet<>();

        for (MedicalOrder order : orders) {
            order.setStatus(MedicalOrderStatus.WAITING);
            relatedRecords.add(order.getMedicalRecord());
        }
        medicalOrderRepository.saveAll(orders);

        // 5. Cập nhật trạng thái của 1 medical record duy nhất
        if (relatedRecords.isEmpty()) {
            throw new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND);
        }

        if (relatedRecords.size() > 1) {
            throw new AppException(ErrorCode.MULTIPLE_MEDICAL_RECORDS_FOUND);
        }

        MedicalRecord record = relatedRecords.iterator().next();
        record.setStatus(MedicalRecordStatus.TESTING);
        medicalRecordRepository.save(record);
        log.info("Invoice {} has been marked as PAID by staff {}", invoice.getId(), staff.getId());

        // 6. Trả về kết quả
        return InvoiceResponse.builder()
                .invoiceId(invoice.getId())
                .amount(invoice.getAmount())
                .paymentType(invoice.getPaymentType().name())
                .status(invoice.getStatus())
                .confirmedAt(invoice.getConfirmedAt())
                .build();
    }
}
