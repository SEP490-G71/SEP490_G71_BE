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
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
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
    private static final double warnUpPct = 30;

    private static final double criticalUpPct = 50;

    private static final double warnDownPct = 30;

    private static final double criticalDownPct = 50;

    private static final ZoneId ZONE = ZoneId.of("Asia/Bangkok");
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
    public String generateInvoiceQr(String invoiceId) {
        Invoice invoice = invoiceRepository.findByIdAndDeletedAtIsNull(invoiceId)
                .orElseThrow(() -> new AppException(ErrorCode.INVOICE_NOT_FOUND));

        SettingResponse setting = settingService.getSetting();
        if (setting.getBankCode() == null || setting.getBankAccountNumber() == null) {
            throw new AppException(ErrorCode.INVALID_INPUT, "Chưa cấu hình tài khoản ngân hàng");
        }

        long amount = invoice.getTotal().longValue();
        String invoiceCode = invoice.getInvoiceCode();

        String addInfoRaw = "Thanh toan " + invoiceCode;
        String addInfo = URLEncoder.encode(normalizeAddInfo(addInfoRaw, 60), StandardCharsets.UTF_8);
        String accountName = URLEncoder.encode(setting.getHospitalName(), StandardCharsets.UTF_8);

        String template = "compact"; // hoặc lấy từ setting

        return String.format(
                "https://img.vietqr.io/image/%s-%s-%s.png?amount=%d&addInfo=%s&accountName=%s&fixedAmount=true",
                setting.getBankCode(),
                setting.getBankAccountNumber(),
                template,
                amount,
                addInfo,
                accountName
        );
    }

    private String normalizeAddInfo(String s, int maxLen) {
        if (s == null) return "";
        String noDiacritic = Normalizer.normalize(s, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        String cleaned = noDiacritic.replaceAll("[^A-Za-z0-9 _\\-]", " ").trim();
        return cleaned.length() > maxLen ? cleaned.substring(0, maxLen) : cleaned;
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

    @Override
    public DailyRevenueSeriesResponse getDailySeries(YearMonth ym) {
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.plusMonths(1).atDay(1);
        LocalDate today = LocalDate.now(ZONE);

        // 1) Query doanh thu theo ngày
        List<Object[]> rows = invoiceRepository.sumDailyBetween(start.atStartOfDay(), end.atStartOfDay());
        Map<LocalDate, BigDecimal> revenueByDate = new HashMap<>();
        for (Object[] r : rows) {
            LocalDate d = (r[0] instanceof java.sql.Date) ? ((java.sql.Date) r[0]).toLocalDate() : (LocalDate) r[0];
            Object val = r[1];
            BigDecimal rev = (val instanceof BigDecimal) ? (BigDecimal) val
                    : (val instanceof Number) ? BigDecimal.valueOf(((Number) val).doubleValue())
                    : new BigDecimal(val.toString());
            revenueByDate.put(d, rev);
        }

        // 2) Target tháng
        BigDecimal monthlyTarget = BigDecimal.ZERO;
        SettingResponse setting = settingService.getSetting();
        if (setting != null && setting.getMonthlyTargetRevenue() != null) {
            monthlyTarget = setting.getMonthlyTargetRevenue();
        }

        int daysInMonth = ym.lengthOfMonth();
        BigDecimal dailyExpected = daysInMonth == 0 ? BigDecimal.ZERO
                : monthlyTarget.divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);

        // 3) Chỉ tới hôm nay (hoặc rỗng nếu tháng tương lai)
        YearMonth currentYm = YearMonth.from(today);
        LocalDate lastDay;
        if (ym.isAfter(currentYm)) {
            lastDay = start.minusDays(1); // future month → empty
        } else if (ym.equals(currentYm)) {
            lastDay = today;
        } else {
            lastDay = ym.atEndOfMonth();
        }

        List<DailyRevenuePoint> points = new ArrayList<>();
        BigDecimal totalToDate = BigDecimal.ZERO;

        for (LocalDate d = start; !d.isAfter(lastDay); d = d.plusDays(1)) {
            BigDecimal revenue = revenueByDate.getOrDefault(d, BigDecimal.ZERO);
            BigDecimal expected = dailyExpected;

            BigDecimal diffPct = expected.signum() == 0 ? BigDecimal.ZERO
                    : revenue.subtract(expected).divide(expected, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            String level = levelOf(diffPct.doubleValue());
            String direction = diffPct.signum() > 0 ? "UP" : (diffPct.signum() < 0 ? "DOWN" : "FLAT");
            String reason = diffPct.signum() >= 0 ? "Above expected" : "Below expected";

            points.add(DailyRevenuePoint.builder()
                    .date(d)
                    .revenue(revenue)
                    .expected(expected)
                    .diffPct(diffPct.setScale(2, RoundingMode.HALF_UP))
                    .level(level)
                    .direction(direction)
                    .reason(reason)
                    .build());

            totalToDate = totalToDate.add(revenue);
        }

        // expectedToDate tính theo tỷ lệ để tránh drift do làm tròn
        int elapsedDays = points.size();
        BigDecimal expectedToDate = daysInMonth == 0 ? BigDecimal.ZERO
                : monthlyTarget.multiply(BigDecimal.valueOf(elapsedDays))
                .divide(BigDecimal.valueOf(daysInMonth), 2, RoundingMode.HALF_UP);

        BigDecimal diffPctToDate = expectedToDate.signum() == 0 ? BigDecimal.ZERO
                : totalToDate.subtract(expectedToDate).divide(expectedToDate, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        String levelToDate = levelOf(diffPctToDate.doubleValue());

        return DailyRevenueSeriesResponse.builder()
                .month(ym)
                .monthlyTarget(monthlyTarget)
                .data(points)
                .totalToDate(totalToDate)
                .expectedToDate(expectedToDate)
                .diffPctToDate(diffPctToDate.setScale(2, RoundingMode.HALF_UP))
                .levelToDate(levelToDate)
                .build();
    }


    public DailyRevenueSeriesResponse getDailySeries(String month) {
        return getDailySeries(YearMonth.parse(month)); // month = "2025-08"
    }

    private String levelOf(double diffPct) {
        // Giảm đột biến (dưới kỳ vọng)
        if (diffPct <= criticalDownPct) return "CRITICAL";
        if (diffPct <= warnDownPct)     return "WARN";

        // Tăng đột biến (trên kỳ vọng)
        if (diffPct >= criticalUpPct)   return "CRITICAL";
        if (diffPct >= warnUpPct)       return "WARN";

        return "OK";
    }


    private String formatCurrency(BigDecimal number) {
        return NumberFormat.getInstance(new Locale("vi", "VN")).format(number);
    }
}
