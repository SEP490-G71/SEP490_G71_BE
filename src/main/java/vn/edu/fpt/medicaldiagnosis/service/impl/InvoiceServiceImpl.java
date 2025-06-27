package vn.edu.fpt.medicaldiagnosis.service.impl;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.util.StreamUtil;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.dto.request.PayInvoiceRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.UpdateInvoiceRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.InvoiceDetailResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.InvoiceItemResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.InvoiceResponse;
import vn.edu.fpt.medicaldiagnosis.entity.*;
import vn.edu.fpt.medicaldiagnosis.enums.InvoiceStatus;
import vn.edu.fpt.medicaldiagnosis.enums.MedicalOrderStatus;
import vn.edu.fpt.medicaldiagnosis.enums.MedicalRecordStatus;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.InvoiceMapper;
import vn.edu.fpt.medicaldiagnosis.repository.*;
import vn.edu.fpt.medicaldiagnosis.service.InvoiceService;
import vn.edu.fpt.medicaldiagnosis.specification.InvoiceSpecification;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.property.HorizontalAlignment;
import com.itextpdf.layout.property.TextAlignment;
import com.itextpdf.layout.property.UnitValue;
import com.itextpdf.layout.borders.Border;

import java.util.List;
import java.util.Locale;


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
    InvoiceMapper invoiceMapper;
    InvoiceItemRepository invoiceItemRepository;
    MedicalServiceRepository medicalServiceRepository;
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

        return invoiceMapper.toInvoiceResponse(invoice);
    }

    @Override
    public Page<InvoiceResponse> getInvoicesPaged(Map<String, String> filters, int page, int size, String sortBy, String sortDir) {
        String sortColumn = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortColumn).ascending() : Sort.by(sortColumn).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Invoice> spec = InvoiceSpecification.buildSpecification(filters);
        Page<Invoice> pageResult = invoiceRepository.findAll(spec, pageable);

        return pageResult.map(invoiceMapper::toInvoiceResponse);
    }

    @Override
    public InvoiceResponse updateInvoiceItems(UpdateInvoiceRequest request) {
        log.info("Service: update invoice items (smart diff)");

        Invoice invoice = invoiceRepository.findByIdAndDeletedAtIsNull(request.getInvoiceId())
                .orElseThrow(() -> new AppException(ErrorCode.INVOICE_NOT_FOUND));

        if (InvoiceStatus.PAID.equals(invoice.getStatus())) {
            throw new AppException(ErrorCode.INVOICE_ALREADY_PAID);
        }

        Staff staff = staffRepository.findByIdAndDeletedAtIsNull(request.getStaffId())
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        // Load hiện tại
        List<InvoiceItem> currentItems = invoiceItemRepository.findAllByInvoiceId(invoice.getId());
        Map<String, InvoiceItem> currentItemMap = currentItems.stream()
                .collect(Collectors.toMap(item -> item.getService().getId(), Function.identity()));

        List<MedicalOrder> allOrders = medicalOrderRepository.findAllByInvoiceItemIdIn(
                currentItems.stream().map(InvoiceItem::getId).toList()
        );

//        Map<String, List<MedicalOrder>> orderMapByItemId = allOrders.stream()
//                .collect(Collectors.groupingBy(order -> order.getInvoiceItem().getId()));

        List<InvoiceItem> itemsToDelete = new ArrayList<>();
        List<InvoiceItem> itemsToKeep = new ArrayList<>();
//        List<InvoiceItem> itemsToUpdate = new ArrayList<>();

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (UpdateInvoiceRequest.InvoiceItemUpdateRequest newItem : request.getServices()) {
            String serviceId = newItem.getServiceId();
            int newQuantity = newItem.getQuantity();

            MedicalService service = medicalServiceRepository.findByIdAndDeletedAtIsNull(serviceId)
                    .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_SERVICE_NOT_FOUND));

            BigDecimal price = service.getPrice();
            BigDecimal discount = service.getDiscount() != null ? service.getDiscount() : BigDecimal.ZERO;
            BigDecimal vat = service.getVat() != null ? service.getVat() : BigDecimal.ZERO;

            BigDecimal discounted = price.subtract(discount);
            BigDecimal subtotal = discounted.multiply(BigDecimal.valueOf(newQuantity));
            BigDecimal total = subtotal.add(subtotal.multiply(vat).divide(BigDecimal.valueOf(100)));
            totalAmount = totalAmount.add(total);

            if (currentItemMap.containsKey(serviceId)) {
                InvoiceItem oldItem = currentItemMap.get(serviceId);
                if (oldItem.getQuantity() == newQuantity) {
                    // Giữ lại
                    itemsToKeep.add(oldItem);
                    continue;
                } else {
                    // Cần xoá và tạo lại
                    itemsToDelete.add(oldItem);
                }
            }

            // Tạo mới
            InvoiceItem created = invoiceItemRepository.save(InvoiceItem.builder()
                    .invoice(invoice)
                    .service(service)
                    .serviceCode(service.getServiceCode())
                    .name(service.getName())
                    .quantity(newQuantity)
                    .price(price)
                    .discount(discount)
                    .vat(vat)
                    .total(total)
                    .build());

            for (int i = 0; i < newQuantity; i++) {
                medicalOrderRepository.save(MedicalOrder.builder()
                        .medicalRecord(allOrders.isEmpty() ? null : allOrders.get(0).getMedicalRecord())
                        .service(service)
                        .invoiceItem(created)
                        .createdBy(staff)
                        .status(MedicalOrderStatus.PENDING)
                        .build());
            }
        }

        // Xoá item và order không còn nữa
        for (InvoiceItem item : currentItems) {
            boolean inRequest = request.getServices().stream()
                    .anyMatch(i -> i.getServiceId().equals(item.getService().getId()));
            if (!inRequest) {
                itemsToDelete.add(item);
            }
        }

        // Xoá medical orders trước
        List<String> deleteItemIds = itemsToDelete.stream().map(InvoiceItem::getId).toList();
        List<MedicalOrder> ordersToDelete = allOrders.stream()
                .filter(order -> deleteItemIds.contains(order.getInvoiceItem().getId()))
                .toList();
        medicalOrderRepository.deleteAll(ordersToDelete);
        invoiceItemRepository.deleteAll(itemsToDelete);

        // Cập nhật lại total
        invoice.setAmount(totalAmount);
        invoiceRepository.save(invoice);

        return invoiceMapper.toInvoiceResponse(invoice);
    }

    @Override
    public ByteArrayInputStream generateInvoicePdf(String invoiceId) {
        log.info("Service: generate invoice pdf");
        Invoice invoice = invoiceRepository.findByIdAndDeletedAtIsNull(invoiceId)
                .orElseThrow(() -> {
                    log.error("Invoice not found for ID: {}", invoiceId);
                    return new AppException(ErrorCode.INVOICE_NOT_FOUND);
                });

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document doc = new Document(pdfDoc, PageSize.A4);
            doc.setMargins(20, 20, 40, 20);

            // === 1. Font Unicode hỗ trợ tiếng Việt ===
            InputStream fontStream = getClass().getClassLoader().getResourceAsStream("fonts/DejaVuSans.ttf");

            if (fontStream == null) {
                log.error("Font file not found in classpath!");
                throw new AppException(ErrorCode.INVOICE_PDF_CREATION_FAILED);
            }
            PdfFont font = PdfFontFactory.createFont(
                    StreamUtil.inputStreamToArray(fontStream), PdfEncodings.IDENTITY_H);
            doc.setFont(font);
            // Thông tin invoice
            log.debug("Invoice: code={}, amount={}, patient={}, confirmedAt={}",
                    invoice.getInvoiceCode(), invoice.getAmount(),
                    invoice.getPatient().getFullName(), invoice.getConfirmedAt());
            log.info("Font loaded successfully from classpath");
            // === 2. Tiêu đề ===
            Paragraph title = new Paragraph("HÓA ĐƠN THANH TOÁN")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(18)
                    .setBold();
            doc.add(title);

            doc.add(new Paragraph("BỆNH VIỆN MEDSOFT").setTextAlignment(TextAlignment.CENTER).setFontSize(12));
            doc.add(new Paragraph("Địa chỉ: 123 Đường ABC, Hà Nội").setTextAlignment(TextAlignment.CENTER).setFontSize(10));
            doc.add(new Paragraph("SĐT: 0123.456.789").setTextAlignment(TextAlignment.CENTER).setFontSize(10));
            doc.add(new Paragraph(" "));

            // === 3. Thông tin hóa đơn ===
            doc.add(new Paragraph("Mã hóa đơn: " + invoice.getInvoiceCode()));
            doc.add(new Paragraph("Bệnh nhân: " + invoice.getPatient().getFullName()));

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            String confirmDate = invoice.getConfirmedAt() != null ? invoice.getConfirmedAt().format(formatter) : "-";
            doc.add(new Paragraph("Ngày xác nhận: " + confirmDate));

            String cashier = invoice.getConfirmedBy() != null ? invoice.getConfirmedBy().getFullName() : "-";
            doc.add(new Paragraph("Nhân viên thu ngân: " + cashier));
            doc.add(new Paragraph(" "));

            // === 4. Bảng dịch vụ chi tiết ===
            float[] columnWidths = {30f, 80f, 150f, 40f, 70f, 70f, 70f, 90f};
            Table table = new Table(UnitValue.createPercentArray(columnWidths)).useAllAvailableWidth();

            table.addHeaderCell(new Cell().add(new Paragraph("STT")).setBold());
            table.addHeaderCell(new Cell().add(new Paragraph("Mã DV")).setBold());         // 👈 Cột mã dịch vụ mới
            table.addHeaderCell(new Cell().add(new Paragraph("Dịch vụ")).setBold());
            table.addHeaderCell(new Cell().add(new Paragraph("SL")).setBold());
            table.addHeaderCell(new Cell().add(new Paragraph("Giá gốc")).setBold());
            table.addHeaderCell(new Cell().add(new Paragraph("Giảm giá")).setBold());
            table.addHeaderCell(new Cell().add(new Paragraph("VAT")).setBold());
            table.addHeaderCell(new Cell().add(new Paragraph("Thành tiền")).setBold());

            List<InvoiceItem> items = invoiceItemRepository.findAllByInvoiceId(invoice.getId());
            log.info("Fetched {} invoice items", items.size());

            int index = 1;
            for (InvoiceItem item : items) {
                table.addCell(String.valueOf(index++));
                table.addCell(item.getServiceCode());
                table.addCell(item.getName());
                table.addCell(String.valueOf(item.getQuantity()));
                table.addCell(formatCurrency(item.getPrice()));
                table.addCell(formatCurrency(item.getDiscount()));
                table.addCell(formatCurrency(item.getVat()));
                table.addCell(formatCurrency(item.getTotal()));
            }

            doc.add(table);

            // === 5. Tổng cộng ===
            doc.add(new Paragraph(" "));
            Paragraph total = new Paragraph("TỔNG CỘNG: " + formatCurrency(invoice.getAmount()) + " VND")
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setBold()
                    .setFontSize(12);
            doc.add(total);

            // === 6. Ký tên ===
            doc.add(new Paragraph(" ").setHeight(20));
            Table signature = new Table(2).useAllAvailableWidth();
            signature.addCell(new Cell().add(new Paragraph("Người thu tiền\n(Ký, ghi rõ họ tên)")).setTextAlignment(TextAlignment.CENTER).setBorder(Border.NO_BORDER));
            signature.addCell(new Cell().add(new Paragraph("Người nhận\n(Ký, ghi rõ họ tên)")).setTextAlignment(TextAlignment.CENTER).setBorder(Border.NO_BORDER));
            doc.add(signature);

            doc.close();
            return new ByteArrayInputStream(out.toByteArray());
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVOICE_PDF_CREATION_FAILED);
        }
    }

    @Override
    public InvoiceDetailResponse getInvoiceDetail(String id) {
        Invoice invoice = invoiceRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVOICE_NOT_FOUND));

        List<InvoiceItem> items = invoiceItemRepository.findAllByInvoiceId(invoice.getId());

        List<InvoiceItemResponse> itemResponses = items.stream().map(item -> InvoiceItemResponse.builder()
                .name(item.getName())
                .quantity(item.getQuantity())
                .serviceCode(item.getServiceCode())
                .price(item.getPrice())
                .discount(item.getDiscount())
                .vat(item.getVat())
                .total(item.getTotal())
                .build()
        ).toList();

        return InvoiceDetailResponse.builder()
                .invoiceId(invoice.getId())
                .invoiceCode(invoice.getInvoiceCode())
                .patientName(invoice.getPatient().getFullName())
                .confirmedAt(invoice.getConfirmedAt())
                .confirmedBy(invoice.getConfirmedBy() != null ? invoice.getConfirmedBy().getFullName() : null)
                .paymentType(invoice.getPaymentType())
                .amount(invoice.getAmount())
                .items(itemResponses)
                .build();
    }


    private String formatCurrency(BigDecimal number) {
        return NumberFormat.getInstance(new Locale("vi", "VN")).format(number);
    }
}
