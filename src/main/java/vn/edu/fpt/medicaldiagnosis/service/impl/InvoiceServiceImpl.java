package vn.edu.fpt.medicaldiagnosis.service.impl;


import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.layout.font.FontProvider;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.dto.request.PayInvoiceRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.UpdateInvoiceRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.*;
import vn.edu.fpt.medicaldiagnosis.entity.*;
import vn.edu.fpt.medicaldiagnosis.enums.InvoiceStatus;
import vn.edu.fpt.medicaldiagnosis.enums.MedicalOrderStatus;
import vn.edu.fpt.medicaldiagnosis.enums.MedicalRecordStatus;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.InvoiceMapper;
import vn.edu.fpt.medicaldiagnosis.repository.*;
import vn.edu.fpt.medicaldiagnosis.service.InvoiceService;
import vn.edu.fpt.medicaldiagnosis.service.SettingService;
import vn.edu.fpt.medicaldiagnosis.service.WorkScheduleService;
import vn.edu.fpt.medicaldiagnosis.specification.InvoiceSpecification;

import java.io.*;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    SettingService settingService;
    WorkScheduleService workScheduleService;

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

        if (!workScheduleService.isStaffOnShiftNow(staff.getId())) {
            throw new AppException(ErrorCode.ACTION_NOT_ALLOWED, "không trong ca làm không thể thao tác");
        }

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
            if (MedicalOrderStatus.PENDING.equals(order.getStatus())) {
                order.setStatus(MedicalOrderStatus.WAITING);
            }
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

        if (!workScheduleService.isStaffOnShiftNow(staff.getId())) {
            throw new AppException(ErrorCode.ACTION_NOT_ALLOWED, "không trong ca làm không thể thao tác");
        }

        // Load hiện tại
        List<InvoiceItem> currentItems = invoiceItemRepository.findAllByInvoiceId(invoice.getId());
        Map<String, InvoiceItem> currentItemMap = currentItems.stream()
                .collect(Collectors.toMap(item -> item.getService().getId(), Function.identity()));

        List<MedicalOrder> allOrders = medicalOrderRepository.findAllByInvoiceItemIdIn(
                currentItems.stream().map(InvoiceItem::getId).toList()
        );

        List<InvoiceItem> itemsToDelete = new ArrayList<>();
        List<InvoiceItem> itemsToKeep = new ArrayList<>();

        // Tổng cộng
        BigDecimal originalTotal = BigDecimal.ZERO;
        BigDecimal discountTotal = BigDecimal.ZERO;
        BigDecimal vatTotal = BigDecimal.ZERO;
        BigDecimal finalTotal = BigDecimal.ZERO;

        for (UpdateInvoiceRequest.InvoiceItemUpdateRequest newItem : request.getServices()) {
            String serviceId = newItem.getServiceId();
            int newQuantity = newItem.getQuantity();

            MedicalService service = medicalServiceRepository.findByIdAndDeletedAtIsNull(serviceId)
                    .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_SERVICE_NOT_FOUND));

            BigDecimal price = service.getPrice();
            BigDecimal discountPercent = service.getDiscount() != null ? service.getDiscount() : BigDecimal.ZERO;
            BigDecimal vat = service.getVat() != null ? service.getVat() : BigDecimal.ZERO;

            BigDecimal itemOriginal = price.multiply(BigDecimal.valueOf(newQuantity));
            BigDecimal discountPerUnit = price.multiply(discountPercent).divide(BigDecimal.valueOf(100));
            BigDecimal itemDiscount = discountPerUnit.multiply(BigDecimal.valueOf(newQuantity));
            BigDecimal discounted = price.subtract(discountPerUnit);

            BigDecimal subtotal = discounted.multiply(BigDecimal.valueOf(newQuantity));
            BigDecimal itemVat = subtotal.multiply(vat).divide(BigDecimal.valueOf(100));
            BigDecimal total = subtotal.add(itemVat);


            originalTotal = originalTotal.add(itemOriginal);
            discountTotal = discountTotal.add(itemDiscount);
            vatTotal = vatTotal.add(itemVat);
            finalTotal = finalTotal.add(total);

            if (currentItemMap.containsKey(serviceId)) {
                InvoiceItem oldItem = currentItemMap.get(serviceId);
                if (oldItem.getQuantity() == newQuantity) {
                    itemsToKeep.add(oldItem);
                    continue;
                } else {
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
                    .discount(discountPercent)
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

        // Xoá item và order không còn trong request
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
        invoice.setOriginalTotal(originalTotal);
        invoice.setDiscountTotal(discountTotal);
        invoice.setVatTotal(vatTotal);
        invoice.setTotal(finalTotal);
        invoiceRepository.save(invoice);

        return invoiceMapper.toInvoiceResponse(invoice);
    }


//    @Override
//    public ByteArrayInputStream generateInvoicePdf(String invoiceId) {
//        log.info("Service: generate invoice pdf");
//        TemplateFileResponse template = templateFileService.getDefaultTemplateByType(TemplateFileType.INVOICE);
//        Invoice invoice = invoiceRepository.findByIdAndDeletedAtIsNull(invoiceId)
//                .orElseThrow(() -> new AppException(ErrorCode.INVOICE_NOT_FOUND));
//
//        List<InvoiceItem> items = invoiceItemRepository.findAllByInvoiceId(invoiceId);
//        SettingResponse setting = settingService.getSetting();
//
//        try {
//            // === 1. Load DOCX từ VPS ===
//            String url = template.getFileUrl();
//            Document doc = new Document();
//            doc.loadFromStream(new URL(url).openStream(), FileFormat.Docx);
//
//            Map<String, Object> invoiceData = new HashMap<>();
//            invoiceData.put("HOSPITAL_NAME", setting.getHospitalName());
//            invoiceData.put("HOSPITAL_ADDRESS", setting.getHospitalAddress());
//            invoiceData.put("HOSPITAL_PHONE", setting.getHospitalPhone());
//            invoiceData.put("INVOICE_CODE", invoice.getInvoiceCode());
//            invoiceData.put("CUSTOMER_NAME", invoice.getPatient().getFullName());
//            invoiceData.put("CUSTOMER_PHONE", invoice.getPatient().getPhone());
//            invoiceData.put("CUSTOMER_CODE", invoice.getPatient().getPatientCode());
//            invoiceData.put("PAYMENT_DATE", invoice.getConfirmedAt() != null
//                    ? invoice.getConfirmedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
//                    : "-");
//            invoiceData.put("CASHIER_NAME", invoice.getConfirmedBy() != null
//                    ? invoice.getConfirmedBy().getFullName()
//                    : "-");
//            invoiceData.put("TOTAL_AMOUNT", formatCurrency(invoice.getTotal()));
//            invoiceData.put("DESCRIPTION", invoice.getDescription() != null ? invoice.getDescription() : "");
//            invoiceData.put("ORIGINAL_TOTAL", formatCurrency(invoice.getOriginalTotal()));
//            invoiceData.put("DISCOUNT_TOTAL", formatCurrency(invoice.getDiscountTotal()));
//            invoiceData.put("VAT_TOTAL", formatCurrency(invoice.getVatTotal()));
//
//            // Thay thế các đoạn văn bản
//            DataUtil.replaceParagraphPlaceholders(doc, invoiceData);
//
//            // === 2. Tìm bảng chứa dòng mẫu dịch vụ ===
//            Section section = doc.getSections().get(0);
//            Table table = null;
//            TableRow templateRow = null;
//
//            outer:
//            for (int t = 0; t < section.getTables().getCount(); t++) {
//                Table currentTable = section.getTables().get(t);
//                for (int r = 0; r < currentTable.getRows().getCount(); r++) {
//                    TableRow row = currentTable.getRows().get(r);
//                    for (int c = 0; c < row.getCells().getCount(); c++) {
//                        String text = DataUtil.getCellText(row.getCells().get(c));
//                        if (text.contains("{SERVICE_NAME}")) {
//                            table = currentTable;
//                            templateRow = row;
//                            break outer;
//                        }
//                    }
//                }
//            }
//
//            if (table == null || templateRow == null) {
//                log.error("Không tìm thấy bảng hoặc dòng mẫu phù hợp.");
//                throw new AppException(ErrorCode.INVOICE_PDF_CREATION_FAILED);
//            }
//
//            // === 3. Sinh dữ liệu dòng dịch vụ ===
//            int index = 1;
//            for (InvoiceItem item : items) {
//                Map<String, Object> rowData = new HashMap<>();
//                rowData.put("INDEX", index++);
//                rowData.put("SERVICE_NAME", item.getName());
//                rowData.put("SERVICE_CODE", item.getServiceCode());
//                rowData.put("QUANTITY", item.getQuantity());
//                rowData.put("PRICE", formatCurrency(item.getPrice()));
//                rowData.put("DISCOUNT", formatCurrency(item.getDiscount()));
//                rowData.put("VAT", formatCurrency(item.getVat()));
//                rowData.put("TOTAL", formatCurrency(item.getTotal()));
//
//                TableRow newRow = (TableRow) templateRow.deepClone();
//                DataUtil.replaceRowPlaceholders(newRow, rowData);
//                int insertIndex = table.getRows().indexOf(templateRow);
//                table.getRows().insert(insertIndex, newRow);
//            }
//
//            table.getRows().remove(templateRow); // Xóa dòng mẫu
//
//            // === 4. QR Code ===
//            String amount = String.valueOf(invoice.getTotal());
//            String addInfo = "Thanh toan " + invoice.getInvoiceCode();
//            String qrUrl = String.format(
//                    "https://img.vietqr.io/image/%s-%s-%s.png?amount=%s&addInfo=%s&accountName=%s&fixedAmount=true",
//                    setting.getBankCode(),
//                    setting.getBankAccountNumber(),
//                    amount,
//                    amount,
//                    URLEncoder.encode(addInfo, StandardCharsets.UTF_8),
//                    URLEncoder.encode(setting.getHospitalName(), StandardCharsets.UTF_8)
//            );
//            log.info("QR URL: {}", qrUrl);
//
//            // Thay thế placeholder hình ảnh QR ở bất cứ đâu
//            DataUtil.replaceImagePlaceholder(doc, "QR_IMAGE", List.of(qrUrl));
//
//            // === 5. Export PDF ===
//            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
//                doc.saveToStream(out, FileFormat.PDF);
//                return new ByteArrayInputStream(out.toByteArray());
//            }
//
//        } catch (Exception e) {
//            log.error("Error generating invoice PDF", e);
//            throw new AppException(ErrorCode.INVOICE_PDF_CREATION_FAILED);
//        }
//    }

    private void addFontFromClasspath(FontProvider fontProvider, String classpathFontPath) throws IOException {
        ClassPathResource resource = new ClassPathResource(classpathFontPath);
        File tempFontFile = File.createTempFile("font-", ".ttf");
        try (InputStream is = resource.getInputStream(); OutputStream os = new FileOutputStream(tempFontFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
        fontProvider.addFont(tempFontFile.getAbsolutePath());
    }

    @Override
    public ByteArrayInputStream generateInvoicePdf(String invoiceId) {
        Invoice invoice = invoiceRepository.findByIdAndDeletedAtIsNull(invoiceId)
                .orElseThrow(() -> new AppException(ErrorCode.INVOICE_NOT_FOUND));

        List<InvoiceItem> items = invoiceItemRepository.findAllByInvoiceId(invoiceId);
        SettingResponse setting = settingService.getSetting();

        try {
            // 1. Tạo bảng dịch vụ
            StringBuilder itemRows = new StringBuilder();
            int i = 1;
            for (InvoiceItem item : items) {
                itemRows.append(String.format("""
                <tr>
                    <td>%d</td>
                    <td>%s</td>
                    <td class="left">%s</td>
                    <td>%d</td>
                    <td class="right">%s</td>
                    <td class="right">%s</td>
                    <td class="right">%s</td>
                    <td class="right">%s</td>
                </tr>
            """, i++,
                        item.getServiceCode(),
                        item.getName(),
                        item.getQuantity(),
                        formatCurrency(item.getPrice()),
                        formatCurrency(item.getDiscount()),
                        formatCurrency(item.getVat()),
                        formatCurrency(item.getTotal())));
            }

            // 2. Tạo link QR
            String qrHtml = "";
            if (setting.getBankCode() != null && setting.getBankAccountNumber() != null) {
                String qrUrl = String.format(
                        "https://img.vietqr.io/image/%s-%s-%s.png?amount=%s&addInfo=%s&accountName=%s&fixedAmount=true",
                        setting.getBankCode(), setting.getBankAccountNumber(), invoice.getTotal(),
                        invoice.getTotal(),
                        URLEncoder.encode("Thanh toan " + invoice.getInvoiceCode(), StandardCharsets.UTF_8),
                        URLEncoder.encode(setting.getHospitalName(), StandardCharsets.UTF_8)
                );

                qrHtml = """
        <img src="%s" width="120" height="120" alt="Mã QR"/>
    """.formatted(qrUrl);
            }


            // 3. Đọc template HTML
            Resource resource = new ClassPathResource("default/invoice.html");
            String htmlTemplate = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

            // 4. Thay thế placeholder trong HTML
            String html = htmlTemplate
                    .replace("{HOSPITAL_NAME}", safe(setting.getHospitalName()))
                    .replace("{HOSPITAL_ADDRESS}", safe(setting.getHospitalAddress()))
                    .replace("{HOSPITAL_PHONE}", safe(setting.getHospitalPhone()))
                    .replace("{INVOICE_CODE}", safe(invoice.getInvoiceCode()))
                    .replace("{CUSTOMER_NAME}", safe(invoice.getPatient().getFullName()))
                    .replace("{CUSTOMER_PHONE}", safe(invoice.getPatient().getPhone()))
                    .replace("{CUSTOMER_CODE}", safe(invoice.getPatient().getPatientCode()))
                    .replace("{PAYMENT_DATE}", invoice.getConfirmedAt() != null
                            ? invoice.getConfirmedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                            : "-")
                    .replace("{CASHIER_NAME}", invoice.getConfirmedBy() != null
                            ? invoice.getConfirmedBy().getFullName() : "-")
                    .replace("{TOTAL_AMOUNT}", formatCurrency(invoice.getTotal()))
                    .replace("{ORIGINAL_TOTAL}", formatCurrency(invoice.getOriginalTotal()))
                    .replace("{DISCOUNT_TOTAL}", formatCurrency(invoice.getDiscountTotal()))
                    .replace("{VAT_TOTAL}", formatCurrency(invoice.getVatTotal()))
                    .replace("{DESCRIPTION}", safe(invoice.getDescription()))
                    .replace("{QR_IMAGE}", qrHtml)
                    .replace("${itemRows}", itemRows.toString());

            // 5. Cấu hình export PDF
            ByteArrayOutputStream pdfOut = new ByteArrayOutputStream();
            ConverterProperties converterProperties = new ConverterProperties();

            // Load font từ classpath
            FontProvider fontProvider = new FontProvider();
            addFontFromClasspath(fontProvider, "fonts/DejaVuSans.ttf");
            addFontFromClasspath(fontProvider, "fonts/DejaVuSans-Bold.ttf");
            converterProperties.setFontProvider(fontProvider);
            converterProperties.setCharset("UTF-8");

            // 6. Convert sang PDF
            HtmlConverter.convertToPdf(html, pdfOut, converterProperties);
            return new ByteArrayInputStream(pdfOut.toByteArray());

        } catch (Exception e) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "PDF creation failed: " + e.getMessage());
        }
    }

    private String safe(String value) {
        return value != null ? value : "-";
    }




    @Override
    public InvoiceDetailResponse getInvoiceDetail(String id) {
        Invoice invoice = invoiceRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.INVOICE_NOT_FOUND));

        List<InvoiceItem> items = invoiceItemRepository.findAllByInvoiceId(invoice.getId());

        List<InvoiceItemResponse> itemResponses = items.stream().map(item -> InvoiceItemResponse.builder()
                .id(item.getId())
                .medicalServiceId(item.getService().getId())
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
                .patientCode(invoice.getPatient().getPatientCode())
                .dateOfBirth(invoice.getPatient().getDob())
                .createdAt(invoice.getCreatedAt())
                .gender(invoice.getPatient().getGender())
                .phone(invoice.getPatient().getPhone())
                .confirmedAt(invoice.getConfirmedAt())
                .confirmedBy(invoice.getConfirmedBy() != null ? invoice.getConfirmedBy().getFullName() : null)
                .paymentType(invoice.getPaymentType())
                .total(invoice.getTotal())
                .originalTotal(invoice.getOriginalTotal())
                .discountTotal(invoice.getDiscountTotal())
                .vatTotal(invoice.getVatTotal())
                .description(invoice.getDescription())
                .items(itemResponses)
                .build();
    }

    @Override
    public BigDecimal sumTotalAmount(Map<String, String> filters) {
        Specification<Invoice> spec = InvoiceSpecification.buildSpecification(filters)
                .and((root, query, cb) -> cb.equal(root.get("status"), InvoiceStatus.PAID));

        List<Invoice> invoices = invoiceRepository.findAll(spec);
        return invoices.stream()
                .map(Invoice::getTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }



    @Override
    public BigDecimal sumMonthlyRevenue() {
        LocalDate firstDayOfMonth = LocalDate.now().withDayOfMonth(1);
        LocalDateTime start = firstDayOfMonth.atStartOfDay();
        LocalDateTime end = LocalDate.now().withDayOfMonth(LocalDate.now().lengthOfMonth()).atTime(23, 59, 59);

        List<Invoice> invoices = invoiceRepository.findAllByCreatedAtBetweenAndStatusAndDeletedAtIsNull(
                start, end, InvoiceStatus.PAID);

        return invoices.stream()
                .map(Invoice::getTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    @Override
    public long countValidInvoices(Map<String, String> filters) {
        Specification<Invoice> spec = InvoiceSpecification.buildSpecification(filters).and(
                (root, query, cb) -> cb.equal(root.get("status"), InvoiceStatus.PAID)
        );
        return invoiceRepository.count(spec);
    }

    @Override
    public List<InvoiceResponse> getAllInvoices(Map<String, String> filters, String sortBy, String sortDir) {
        String sortColumn = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortColumn).ascending() : Sort.by(sortColumn).descending();

        Specification<Invoice> spec = InvoiceSpecification.buildSpecification(filters);

        List<Invoice> invoices = invoiceRepository.findAll(spec, sort);
        return invoices.stream()
                .map(invoiceMapper::toInvoiceResponse)
                .toList();
    }



    private String formatCurrency(BigDecimal number) {
        return NumberFormat.getInstance(new Locale("vi", "VN")).format(number);
    }
}
