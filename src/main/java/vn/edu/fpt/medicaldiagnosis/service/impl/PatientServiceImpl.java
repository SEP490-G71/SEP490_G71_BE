package vn.edu.fpt.medicaldiagnosis.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.common.DataUtil;
import vn.edu.fpt.medicaldiagnosis.context.TenantContext;
import vn.edu.fpt.medicaldiagnosis.dto.request.AccountCreationRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.PatientRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.AccountResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.PatientResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientsResponse;
import vn.edu.fpt.medicaldiagnosis.entity.EmailTask;
import vn.edu.fpt.medicaldiagnosis.entity.Patient;
import vn.edu.fpt.medicaldiagnosis.enums.DepartmentType;
import vn.edu.fpt.medicaldiagnosis.enums.Status;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.PatientMapper;
import vn.edu.fpt.medicaldiagnosis.repository.EmailTaskRepository;
import vn.edu.fpt.medicaldiagnosis.repository.PatientRepository;
import vn.edu.fpt.medicaldiagnosis.service.AccountService;
import vn.edu.fpt.medicaldiagnosis.service.EmailService;
import vn.edu.fpt.medicaldiagnosis.service.PatientService;
import vn.edu.fpt.medicaldiagnosis.service.QueuePatientsService;
import vn.edu.fpt.medicaldiagnosis.specification.PatientSpecification;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PRIVATE;
import static vn.edu.fpt.medicaldiagnosis.enums.Role.PATIENT;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class PatientServiceImpl implements PatientService {

    PatientRepository patientRepository;
    PatientMapper patientMapper;
    AccountService accountService;
    EmailService emailService;
    CodeGeneratorService codeGeneratorService;
    QueuePatientsService queuePatientsService;
    EmailTaskRepository emailTaskRepository;
    @Override
    @Transactional
    public PatientResponse createPatient(PatientRequest request) {
        if (patientRepository.existsByEmailAndDeletedAtIsNull(request.getEmail())) {
            throw new AppException(ErrorCode.PATIENT_EMAIL_EXISTED);
        }

        if (patientRepository.existsByPhoneAndDeletedAtIsNull(request.getPhone())) {
            throw new AppException(ErrorCode.PATIENT_PHONE_EXISTED);
        }

        // Step 1: tạo account từ họ tên
        String username = accountService.generateUniqueUsername(
                request.getFirstName(),
                request.getMiddleName(),
                request.getLastName()
        );

        String password = DataUtil.generateRandomPassword(10);

        AccountCreationRequest accountRequest = AccountCreationRequest.builder()
                .username(username)
                .password(password)
                .role(PATIENT.name())
                .build();

        // Tạo account trước
        AccountResponse accountResponse = accountService.createAccount(accountRequest);

        // Step 2: tạo patient với accountId
        Patient patient = patientMapper.toPatient(request);
        patient.setAccountId(accountResponse.getId());

        String fullName = (patient.getFirstName() + " " +
                (patient.getMiddleName() != null ? patient.getMiddleName().trim() + " " : "") +
                patient.getLastName()).replaceAll("\\s+", " ").trim();
        patient.setFullName(fullName);

        String patientCode = codeGeneratorService.generateCode("PATIENT", "BN", 6);
        patient.setPatientCode(patientCode);

        log.info("Patient created: {}", patient);

        String url = "https://" + TenantContext.getTenantId() + ".datnd.id.vn" + "/home";
        emailService.sendAccountMail(patient.getEmail(), fullName, accountRequest.getUsername(), accountRequest.getPassword(), url);
        // Lưu và trả về
        return patientMapper.toPatientResponse(patientRepository.save(patient));
    }


    @Override
    public List<PatientResponse> getAllPatients() {
        return patientRepository.findAllByDeletedAtIsNull()
                .stream()
                .map(patientMapper::toPatientResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PatientResponse getPatientById(String id) {
        Patient patient = patientRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND));
        return patientMapper.toPatientResponse(patient);
    }

    @Override
    public void deletePatient(String id) {
        Patient patient = patientRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND));
        patient.setDeletedAt(LocalDateTime.now());
        patientRepository.save(patient);
    }

    @Override
    public PatientResponse updatePatient(String id, PatientRequest request) {
        Patient patient = patientRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.PATIENT_NOT_FOUND));
        if (patientRepository.existsByEmailAndDeletedAtIsNullAndIdNot(request.getEmail(), id)) {
            throw new AppException(ErrorCode.PATIENT_EMAIL_EXISTED);
        }

        if (patientRepository.existsByPhoneAndDeletedAtIsNullAndIdNot(request.getPhone(), id)) {
            throw new AppException(ErrorCode.PATIENT_PHONE_EXISTED);
        }
        patientMapper.updatePatient(patient, request);

        String fullName = (patient.getFirstName() + " " +
                (patient.getMiddleName() != null ? patient.getMiddleName().trim() + " " : "") +
                patient.getLastName()).replaceAll("\\s+", " ").trim();
        patient.setFullName(fullName);
        log.info("Patient updated: {}", patient);
        return patientMapper.toPatientResponse(patientRepository.save(patient));
    }

    @Override
    public Page<PatientResponse> getPatientsPaged(Map<String, String> filters, int page, int size, String sortBy, String sortDir) {
        String sortColumn = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortColumn).ascending() : Sort.by(sortColumn).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<Patient> spec = PatientSpecification.buildSpecification(filters);

        Page<Patient> pageResult = patientRepository.findAll(spec, pageable);
        return pageResult.map(patientMapper::toPatientResponse);
    }

    @Override
    public Page<PatientResponse> getPatientsRegisteredTodayPaged(Map<String, String> filters, int page, int size, String sortBy, String sortDir) {
        // 1. Lọc theo type nếu có
        String typeFilter = filters.get("type");

        List<QueuePatientsResponse> queuePatientsResponses = queuePatientsService.getAllQueuePatients().stream()
                .filter(qp -> typeFilter == null || (qp.getType() != null && qp.getType().name().equalsIgnoreCase(typeFilter)))
                .toList();

        // 2. Xoá type khỏi filters (vì không phải field của Patient entity)
        filters.remove("type");

        if (queuePatientsResponses.isEmpty()) {
            return Page.empty();
        }

        // 3. Lấy full danh sách patientIds (có thể trùng)
        List<String> patientIds = queuePatientsResponses.stream()
                .map(QueuePatientsResponse::getPatientId)
                .toList();

        // 4. Build spec
        Specification<Patient> spec = PatientSpecification.buildSpecification(filters)
                .and((root, query, cb) -> root.get("id").in(patientIds))
                .and((root, query, cb) -> cb.isNull(root.get("deletedAt")));

        // 5. Build sort & pageable
        String sortColumn = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortColumn).ascending() : Sort.by(sortColumn).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // 6. Truy vấn bệnh nhân (sẽ không duplicate)
        Map<String, Patient> patientMap = patientRepository.findAll(spec, Pageable.unpaged()).stream()
                .collect(Collectors.toMap(Patient::getId, Function.identity()));

        // 7. Enrich bản ghi từ queuePatientsResponses
        List<PatientResponse> enrichedList = queuePatientsResponses.stream()
                .filter(qp -> patientMap.containsKey(qp.getPatientId()))
                .map(qp -> {
                    Patient patient = patientMap.get(qp.getPatientId());
                    PatientResponse response = patientMapper.toPatientResponse(patient);
                    response.setType(qp.getType());
                    response.setRegisteredTime(qp.getCreatedAt());
                    return response;
                })
                .sorted((a, b) -> {
                    if (sort.isSorted()) {
                        Sort.Order order = sort.stream().findFirst().orElse(null);
                        if (order != null && "createdAt".equals(order.getProperty())) {
                            return order.isAscending()
                                    ? a.getRegisteredTime().compareTo(b.getRegisteredTime())
                                    : b.getRegisteredTime().compareTo(a.getRegisteredTime());
                        }
                    }
                    return 0;
                })
                .toList();

        // 8. Manual pagination
        int total = enrichedList.size();
        int fromIndex = Math.min(page * size, total);
        int toIndex = Math.min(fromIndex + size, total);
        List<PatientResponse> pageContent = enrichedList.subList(fromIndex, toIndex);

        return new PageImpl<>(pageContent, pageable, total);
    }

    @Override
    public Page<PatientResponse> getPatientsWithBirthdaysInMonth(int month, Map<String, String> filters, int page, int size, String sortBy, String sortDir) {
        Pageable pageable = PageRequest.of(page, size,
                sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending());
        log.info("Service: get patients with birthday in month={}, filters={}, page={}, size={}", month, filters, page, size);
        Specification<Patient> spec = PatientSpecification.buildSpecification(filters)
                .and((root, query, cb) ->
                        cb.equal(cb.function("MONTH", Integer.class, root.get("dob")), month)
                );

        Page<Patient> patients = patientRepository.findAll(spec, pageable);

        return patients.map(patientMapper::toPatientResponse);
    }

    @Override
    public List<PatientResponse> getAllPatientBirthdays(int month, Map<String, String> filters, String sortBy, String sortDir) {
        log.info("Service: get patients with birthday in month={}, filters={}", month, filters);
        Specification<Patient> spec = PatientSpecification.buildSpecification(filters)
                .and((root, query, cb) ->
                        cb.equal(cb.function("MONTH", Integer.class, root.get("dob")), month)
                );

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();

        return patientRepository.findAll(spec, sort)
                .stream()
                .map(patientMapper::toPatientResponse)
                .collect(Collectors.toList());
    }



    @Override
    public List<PatientResponse> searchByNameOrCode(String keyword) {
        List<Patient> patients = patientRepository.findByFullNameContainingIgnoreCaseOrPatientCodeContainingIgnoreCase(keyword, keyword);
        return patients.stream()
                .map(patientMapper::toPatientResponse)
                .collect(Collectors.toList());
    }

    @Override
    public int generateBirthdayEmailsForCurrentTenant(int month) {
        String tenantId = TenantContext.getTenantId();
        log.info("[{}] Gửi email sinh nhật thủ công cho tháng {}", tenantId, month);

        List<Patient> birthdayPatients = patientRepository.findAll((root, query, cb) ->
                cb.equal(cb.function("MONTH", Integer.class, root.get("dob")), month)
        );

        if (birthdayPatients.isEmpty()) return 0;

        List<EmailTask> tasks = birthdayPatients.stream()
                .filter(p -> p.getEmail() != null && !p.getEmail().isBlank())
                .map(patient -> buildEmailTask(patient, tenantId))
                .toList();

        emailTaskRepository.saveAll(tasks);
        log.info("[{}] Đã tạo {} email sinh nhật", tenantId, tasks.size());

        return tasks.size();
    }

    private EmailTask buildEmailTask(Patient patient, String tenantId) {
        String url = "https://" + tenantId + ".datnd.id.vn/home";
        String content;

        try {
            ClassPathResource resource = new ClassPathResource("templates/birthday-email.html");
            String template = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            content = template
                    .replace("{{name}}", patient.getFullName())
                    .replace("{{url}}", url);
        } catch (Exception e) {
            content = String.format("Xin chào %s,\n\nTruy cập hệ thống tại: %s\n\nTrân trọng.",
                    patient.getFullName(), url);
            log.warn("[{}] Không thể load template birthday-email.html: {}", tenantId, e.getMessage());
        }

        return EmailTask.builder()
                .id(UUID.randomUUID().toString())
                .emailTo(patient.getEmail())
                .subject("🎂 Chúc mừng sinh nhật, " + patient.getFullName() + "!")
                .content(content)
                .retryCount(0)
                .status(Status.PENDING)
                .build();
    }

}
