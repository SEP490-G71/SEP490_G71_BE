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
import vn.edu.fpt.medicaldiagnosis.enums.InvoiceStatus;
import vn.edu.fpt.medicaldiagnosis.enums.MedicalOrderStatus;
import vn.edu.fpt.medicaldiagnosis.enums.PaymentType;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.repository.InvoiceRepository;
import vn.edu.fpt.medicaldiagnosis.repository.MedicalOrderRepository;
import vn.edu.fpt.medicaldiagnosis.repository.MedicalRecordRepository;
import vn.edu.fpt.medicaldiagnosis.service.InvoiceService;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class InvoiceServiceImpl implements InvoiceService {

    InvoiceRepository invoiceRepository;
    MedicalOrderRepository medicalOrderRepository;
    MedicalRecordRepository medicalRecordRepository;

    @Override
    public InvoiceResponse payInvoice(PayInvoiceRequest request) {
        log.info("Service: pay invoice");
        // 1. Lấy Invoice & kiểm tra trạng thái
        Invoice invoice = invoiceRepository.findByIdAndDeletedAtIsNull(request.getInvoiceId())
                .orElseThrow(() -> new AppException(ErrorCode.INVOICE_NOT_FOUND));

        if (InvoiceStatus.PAID.equals(invoice.getStatus())) {
            throw new AppException(ErrorCode.INVOICE_ALREADY_PAID);
        }

        // 2. Cập nhật Invoice
        invoice.setPaymentType(request.getPaymentType());
        invoice.setStatus(InvoiceStatus.PAID);
//        invoice.set(Instant.now());               // thêm cột nếu cần
        invoiceRepository.save(invoice);

        // 3. Tìm các MedicalOrder liên quan tới Invoice → cập nhật MedicalRecord
        List<MedicalOrder> orders =
                medicalOrderRepository.findAllByInvoiceItemInvoiceId(invoice.getId());

        orders.forEach(o -> o.setStatus(MedicalOrderStatus.PAID)); // optional
        medicalOrderRepository.saveAll(orders);

        // Các MedicalRecord liên quan
//        orders.stream()
//                .map(MedicalOrder::getMedicalRecord)
//                .distinct()
//                .forEach(record -> {
//                    record.setStatus(MedicalRecordStatus.PAID);
//                    medicalRecordRepository.save(record);
//                });

        // 4. Trả về Response
        return InvoiceResponse.builder()
                .invoiceId(invoice.getId())
                .amount(invoice.getAmount())
                .paymentType(invoice.getPaymentType().name())
                .status(invoice.getStatus())
//                .paidAt(invoice.getPaidAt())
                .build();
    }
}
