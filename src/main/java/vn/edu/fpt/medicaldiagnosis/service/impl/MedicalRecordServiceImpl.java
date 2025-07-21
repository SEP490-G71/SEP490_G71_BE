package vn.edu.fpt.medicaldiagnosis.service.impl;

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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.common.DataUtil;
import vn.edu.fpt.medicaldiagnosis.dto.request.MedicalRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.UpdateMedicalRecordRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.*;
import vn.edu.fpt.medicaldiagnosis.entity.*;
import vn.edu.fpt.medicaldiagnosis.enums.InvoiceStatus;
import vn.edu.fpt.medicaldiagnosis.enums.MedicalOrderStatus;
import vn.edu.fpt.medicaldiagnosis.enums.MedicalRecordStatus;
import vn.edu.fpt.medicaldiagnosis.enums.TemplateFileType;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.MedicalRecordMapper;
import vn.edu.fpt.medicaldiagnosis.mapper.QueuePatientsMapper;
import vn.edu.fpt.medicaldiagnosis.repository.*;
import vn.edu.fpt.medicaldiagnosis.service.AccountService;
import vn.edu.fpt.medicaldiagnosis.service.MedicalRecordService;
import vn.edu.fpt.medicaldiagnosis.specification.MedicalRecordSpecification;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.spire.doc.*;

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
    MedicalRecordMapper medicalRecordMapper;
    MedicalResultImageRepository medicalResultImageRepository;
    TemplateFileServiceImpl templateFileService;
    QueuePatientsRepository queuePatientsRepository;
    AccountService accountService;
    QueuePatientsMapper queuePatientsMapper;
    AccountRepository accountRepository;
    DepartmentRepository departmentRepository;
    @Override
    public MedicalResponse createMedicalRecord(MedicalRequest request) {
        log.info("Service: create medical record");
        log.info("Request: {}", request);

        // 1. Validate and fetch patient, staff, and visit
        Patient patient = patientRepository.findByIdAndDeletedAtIsNull(request.getPatientId())
                .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND, "Không tìm thấy thông tin bệnh nhân"));

        Staff staff = staffRepository.findByIdAndDeletedAtIsNull(request.getStaffId())
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND, "Không tìm thấy thông tin nhân viên"));
        AccountResponse staffAccount = accountService.getAccount(staff.getAccountId());

        // 2a. Check staff has role DOCTOR
        boolean isDoctor = staffAccount.getRoles().stream()
                .anyMatch(role -> "DOCTOR".equalsIgnoreCase(role.getName()));
        if (!isDoctor) {
            throw new AppException(ErrorCode.NO_PERMISSION, "Bạn không có quyền thực hiện thao tác này (chỉ dành cho bác sĩ)");
        }

        QueuePatients visit = queuePatientsRepository.findByIdAndDeletedAtIsNull(request.getVisitId())
                .orElseThrow(() -> new AppException(ErrorCode.QUEUE_PATIENT_NOT_FOUND, "Không tìm thấy lượt khám của bệnh nhân"));

        if (!visit.getPatientId().equals(request.getPatientId())) {
            throw new AppException(ErrorCode.INVALID_DATA, "Lượt khám không thuộc về bệnh nhân đã chọn");
        }

        // 2. Generate medical record code
        String medicalRecordCode = codeGeneratorService.generateCode("MEDICAL_RECORD", "MR-", 6);

        // 3. Create MedicalRecord (with vital signs)
        MedicalRecord record = MedicalRecord.builder()
                .medicalRecordCode(medicalRecordCode)
                .patient(patient)
                .visit(visit)
                .createdBy(staff)
                .diagnosisText(request.getDiagnosisText())
                .temperature(request.getTemperature())
                .respiratoryRate(request.getRespiratoryRate())
                .bloodPressure(request.getBloodPressure())
                .heartRate(request.getHeartRate())
                .heightCm(request.getHeightCm())
                .weightKg(request.getWeightKg())
                .bmi(request.getBmi()) // nếu muốn có thể tự tính tại đây
                .spo2(request.getSpo2())
                .notes(request.getNotes())
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
                .originalTotal(BigDecimal.ZERO)
                .discountTotal(BigDecimal.ZERO)
                .vatTotal(BigDecimal.ZERO)
                .total(BigDecimal.ZERO)

                .build();
        invoice = invoiceRepository.save(invoice);

        List<String> medicalOrderIds = new ArrayList<>();
        // Tổng các thành phần
        BigDecimal originalTotal = BigDecimal.ZERO;
        BigDecimal discountTotal = BigDecimal.ZERO;
        BigDecimal vatTotal = BigDecimal.ZERO;
        BigDecimal finalTotal = BigDecimal.ZERO;

        // 6. Loop through each service
        for (MedicalRequest.ServiceRequest s : request.getServices()) {
            MedicalService service = medicalServiceRepository.findByIdAndDeletedAtIsNull(s.getServiceId())
                    .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_SERVICE_NOT_FOUND,
                            "Không tìm thấy dịch vụ y tế với ID: " + s.getServiceId()));

            int quantity = s.getQuantity();
            BigDecimal price = service.getPrice();
            BigDecimal discountPercent = service.getDiscount() != null ? service.getDiscount() : BigDecimal.ZERO;
            BigDecimal vat = service.getVat() != null ? service.getVat() : BigDecimal.ZERO;

            BigDecimal original = price.multiply(BigDecimal.valueOf(quantity));
            BigDecimal discountPerUnit = price.multiply(discountPercent).divide(BigDecimal.valueOf(100));
            BigDecimal discountTotalForItem = discountPerUnit.multiply(BigDecimal.valueOf(quantity));
            BigDecimal discounted = price.subtract(discountPerUnit);

            BigDecimal subtotal = discounted.multiply(BigDecimal.valueOf(quantity));
            BigDecimal vatAmount = subtotal.multiply(vat).divide(BigDecimal.valueOf(100));
            BigDecimal total = subtotal.add(vatAmount);

            originalTotal = originalTotal.add(original);
            discountTotal = discountTotal.add(discountTotalForItem);
            vatTotal = vatTotal.add(vatAmount);
            finalTotal = finalTotal.add(total);


            // 7. Create InvoiceItem (1 dòng, tổng quantity)
            InvoiceItem item = InvoiceItem.builder()
                    .invoice(invoice)
                    .service(service)
                    .serviceCode(service.getServiceCode())
                    .name(service.getName())
                    .quantity(quantity)
                    .price(price)
                    .discount(discountPercent)
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
        // 7. Kiểm tra sinh nhật trong tháng
        boolean isBirthdayMonth = patient.getDob() != null &&
                patient.getDob().getMonthValue() == LocalDate.now().getMonthValue();

        String description = null;
        if (isBirthdayMonth) {
            BigDecimal discountByBirthday = finalTotal.multiply(BigDecimal.valueOf(0.1));
            finalTotal = finalTotal.subtract(discountByBirthday);
            discountTotal = discountTotal.add(discountByBirthday);
            description = "Giảm 10% nhân dịp sinh nhật bệnh nhân";
        }

        // 9. Update invoice total
        invoice.setOriginalTotal(originalTotal);
        invoice.setDiscountTotal(discountTotal);
        invoice.setVatTotal(vatTotal);
        invoice.setTotal(finalTotal);
        invoice.setDescription(description);
        invoiceRepository.save(invoice);

        // 10. Return response
        return MedicalResponse.builder()
                .medicalRecordId(record.getId())
                .invoiceId(invoice.getId())
                .originalTotal(originalTotal)
                .discountTotal(discountTotal)
                .vatTotal(vatTotal)
                .totalAmount(finalTotal)
                .medicalOrderIds(medicalOrderIds)
                .build();
    }

    @Override
    public MedicalRecordDetailResponse getMedicalRecordDetail(String recordId) {
        MedicalRecord record = medicalRecordRepository.findByIdAndDeletedAtIsNull(recordId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND, "Không tìm thấy hồ sơ bệnh án"));

        List<MedicalOrder> orders = medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull(recordId);

        List<MedicalOrderResponse> orderDTOs = orders.stream().map(order -> {
            List<MedicalResult> results = medicalResultRepository
                    .findAllByMedicalOrderIdAndDeletedAtIsNull(order.getId());

            List<MedicalResultResponse> resultDTOs = results.stream().map(result -> {
                List<String> imageUrls = medicalResultImageRepository
                        .findAllByMedicalResultId(result.getId())
                        .stream()
                        .map(MedicalResultImage::getImageUrl)
                        .toList();

                return MedicalResultResponse.builder()
                        .id(result.getId())
                        .completedBy(result.getCompletedBy().getFullName())
                        .imageUrls(imageUrls)
                        .note(result.getResultNote())
                        .build();
            }).toList();


            return MedicalOrderResponse.builder()
                    .id(order.getId())
                    .serviceName(order.getService().getName())
                    .status(order.getStatus().name())
                    .createdBy(order.getCreatedBy().getFullName())
                    .results(resultDTOs)
                    .build();
        }).toList();

        QueuePatientsResponse visitResponse = queuePatientsMapper.toResponse(record.getVisit());


        return MedicalRecordDetailResponse.builder()
                .id(record.getId())
                .patientId(record.getPatient().getId())
                .medicalRecordCode(record.getMedicalRecordCode())
                .patientName(record.getPatient().getFullName())
                .createdBy(record.getCreatedBy().getFullName())
                .diagnosisText(record.getDiagnosisText())
                .summary(record.getSummary())
                .status(record.getStatus().name())
                .createdAt(record.getCreatedAt())

                .visit(visitResponse)

                .temperature(record.getTemperature())
                .respiratoryRate(record.getRespiratoryRate())
                .bloodPressure(record.getBloodPressure())
                .heartRate(record.getHeartRate())
                .heightCm(record.getHeightCm())
                .weightKg(record.getWeightKg())
                .bmi(record.getBmi())
                .spo2(record.getSpo2())
                .notes(record.getNotes())

                .orders(orderDTOs)
                .build();
    }

    @Override
    public List<MedicalRecordResponse> getMedicalRecordHistory(String patientId) {
        // 1. Kiểm tra bệnh nhân có tồn tại
        Patient patient = patientRepository.findByIdAndDeletedAtIsNull(patientId)
                .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND, "Không tìm thấy bệnh nhân"));

        // 2. Lấy danh sách hồ sơ bệnh án của bệnh nhân
        List<MedicalRecord> records = medicalRecordRepository
                .findAllByPatientIdAndDeletedAtIsNullOrderByCreatedAtDesc(patientId);

        // 3. Chuyển sang MedicalRecordResponse
        return records.stream().map(record -> MedicalRecordResponse.builder()
                .id(record.getId())
                .medicalRecordCode(record.getMedicalRecordCode())
                .patientName(patient.getFullName())
                .doctorName(record.getCreatedBy().getFullName())
                .status(record.getStatus().name())
                .createdAt(record.getCreatedAt())
                .build()
        ).toList();
    }


    @Override
    public Page<MedicalRecordResponse> getMedicalRecordsPaged(Map<String, String> filters, int page, int size, String sortBy, String sortDir) {
        String sortColumn = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortColumn).ascending() : Sort.by(sortColumn).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<MedicalRecord> spec = MedicalRecordSpecification.buildSpecification(filters);
        Page<MedicalRecord> pageResult = medicalRecordRepository.findAll(spec, pageable);

        return pageResult.map(medicalRecordMapper::toMedicalRecordResponse);
    }

    @Override
    public ByteArrayInputStream generateMedicalRecordPdf(String recordId) {
        MedicalRecord record = medicalRecordRepository.findByIdAndDeletedAtIsNull(recordId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));

        List<MedicalOrder> orders = medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull(recordId);
        TemplateFileResponse template = templateFileService.getDefaultTemplateByType(TemplateFileType.MEDICAL_RECORD);
        try {
            // === 1. Load template DOCX từ vps ===
            String url = template.getFileUrl();
            Document doc = new Document();
            doc.loadFromStream(new URL(url).openStream(), FileFormat.Docx);

            // === 2. Thay thế các trường cơ bản ===
            Map<String, Object> recordData = Map.of(
                    "medicalRecordCode", record.getMedicalRecordCode(),
                    "patientName", record.getPatient().getFullName(),
                    "createdBy", record.getCreatedBy().getFullName(),
                    "status", record.getStatus().name(),
                    "createdAt", record.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    "diagnosisText", record.getDiagnosisText()
            );
            DataUtil.replaceParagraphPlaceholders(doc, recordData);

            // === 3. Thay thế từng dịch vụ đã thực hiện (MedicalOrder) ===
            int index = 1;
            for (MedicalOrder order : orders) {
                List<MedicalResult> results = medicalResultRepository.findAllByMedicalOrderIdAndDeletedAtIsNull(order.getId());

                MedicalResult result = results.isEmpty() ? null : results.get(0);
                List<String> imageUrls = result != null
                        ? medicalResultImageRepository.findAllByMedicalResultId(result.getId())
                        .stream()
                        .map(MedicalResultImage::getImageUrl)
                        .toList()
                        : List.of();

                Map<String, Object> orderData = new HashMap<>();
                orderData.put("serviceName" + index, order.getService().getName());
                orderData.put("createdBy" + index, order.getCreatedBy().getFullName());
                orderData.put("completedBy" + index, result != null && result.getCompletedBy() != null
                        ? result.getCompletedBy().getFullName() : "-");
                orderData.put("note" + index, result != null ? result.getResultNote() : "-");

                DataUtil.replaceParagraphPlaceholders(doc, orderData);

                DataUtil.replaceImagePlaceholder(doc, "imageUrls" + index, imageUrls);
                index++;
            }

            // === 4. Xuất ra PDF ===
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                doc.saveToStream(out, FileFormat.PDF);
                return new ByteArrayInputStream(out.toByteArray());
            }

        } catch (Exception e) {
            log.error("Error generating medical record PDF", e);
            throw new AppException(ErrorCode.MEDICAL_RECORD_PDF_FAILED);
        }
    }

    @Override
    public MedicalRecordDetailResponse updateMedicalRecord(String recordId, UpdateMedicalRecordRequest request) {
        log.info("Bắt đầu cập nhật hồ sơ bệnh án ID: {}", recordId);

        // 1. Tìm hồ sơ bệnh án
        MedicalRecord record = medicalRecordRepository.findByIdAndDeletedAtIsNull(recordId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND, "Không tìm thấy hồ sơ bệnh án"));

        // 2. Xác định người dùng hiện tại
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("Người dùng hiện tại: {}", username);

        // 3. Lấy tài khoản và thông tin nhân viên
        Account account = accountRepository.findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "Không tìm thấy tài khoản đăng nhập"));

        staffRepository.findByAccountIdAndDeletedAtIsNull(account.getId())
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND, "Không tìm thấy thông tin nhân viên"));

        // 4. Kiểm tra quyền (phải là bác sĩ)
        AccountResponse staffAccount = accountService.getAccount(account.getId());
        Set<String> roleNames = staffAccount.getRoles().stream()
                .map(role -> role.getName().toUpperCase())
                .collect(Collectors.toSet());

        if (!roleNames.contains("DOCTOR")) {
            log.warn("Tài khoản '{}' không có quyền cập nhật hồ sơ bệnh án", username);
            throw new AppException(ErrorCode.NO_PERMISSION, "Bạn không có quyền thực hiện thao tác này (chỉ dành cho bác sĩ)");
        }

        // 5. Cập nhật thông tin nếu có
        if (request.getDiagnosisText() != null) record.setDiagnosisText(request.getDiagnosisText());
        if (request.getSummary() != null) record.setSummary(request.getSummary());
        if (request.getTemperature() != null) record.setTemperature(request.getTemperature());
        if (request.getRespiratoryRate() != null) record.setRespiratoryRate(request.getRespiratoryRate());
        if (request.getBloodPressure() != null) record.setBloodPressure(request.getBloodPressure());
        if (request.getHeartRate() != null) record.setHeartRate(request.getHeartRate());
        if (request.getHeightCm() != null) record.setHeightCm(request.getHeightCm());
        if (request.getWeightKg() != null) record.setWeightKg(request.getWeightKg());
        if (request.getBmi() != null) record.setBmi(request.getBmi());
        if (request.getSpo2() != null) record.setSpo2(request.getSpo2());
        if (request.getNotes() != null) record.setNotes(request.getNotes());

        record.setUpdatedAt(LocalDateTime.now());

        // 6. Lưu thay đổi
        medicalRecordRepository.save(record);
        log.info("✅ Đã cập nhật hồ sơ bệnh án thành công: {}", recordId);
        return getMedicalRecordDetail(recordId);
    }

    @Override
    public List<MedicalRecordOrderResponse> getOrdersByDepartment(String departmentId) {
        // ✅ Kiểm tra phòng ban có tồn tại
        Department department = departmentRepository.findByIdAndDeletedAtIsNull(departmentId)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND, "Không tìm thấy phòng ban"));

        // ✅ Lấy danh sách order thuộc phòng ban
        List<MedicalOrder> orders = medicalOrderRepository
                .findAllByService_Department_IdAndStatusNotAndDeletedAtIsNull(departmentId, MedicalOrderStatus.PENDING);

        // ✅ Mapping kết quả
        return orders.stream().map(order -> {
            MedicalRecord record = order.getMedicalRecord();
            return MedicalRecordOrderResponse.builder()
                    .orderId(order.getId())
                    .medicalRecordId(record.getId())
                    .medicalRecordCode(record.getMedicalRecordCode())
                    .patientName(record.getPatient().getFullName())
                    .serviceName(order.getService().getName())
                    .status(order.getStatus())
                    .createdAt(order.getCreatedAt())
                    .build();
        }).toList();
    }

}
