package vn.edu.fpt.medicaldiagnosis.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.common.DataUtil;
import vn.edu.fpt.medicaldiagnosis.context.TenantContext;
import vn.edu.fpt.medicaldiagnosis.dto.request.AccountCreationRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.PatientRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.AccountResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.PatientResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientsResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Patient;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.PatientMapper;
import vn.edu.fpt.medicaldiagnosis.repository.PatientRepository;
import vn.edu.fpt.medicaldiagnosis.service.AccountService;
import vn.edu.fpt.medicaldiagnosis.service.EmailService;
import vn.edu.fpt.medicaldiagnosis.service.PatientService;
import vn.edu.fpt.medicaldiagnosis.service.QueuePatientsService;
import vn.edu.fpt.medicaldiagnosis.specification.PatientSpecification;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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

        // 1. Lấy queue bệnh nhân check-in hôm nay
        List<QueuePatientsResponse> queuePatientsResponses = queuePatientsService.getAllQueuePatients();

        // 2. Lọc theo ngày hôm nay
        List<String> patientIds = queuePatientsResponses.stream()
                .map(QueuePatientsResponse::getPatientId)
                .distinct()
                .toList();

        log.info("Patient ids size: {}", patientIds.size());

        if (patientIds.isEmpty()) {
            return Page.empty();
        }

        // 3. Build sort & pageable
        String sortColumn = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortColumn).ascending() : Sort.by(sortColumn).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        // 4. Build base spec from filters
        Specification<Patient> spec = PatientSpecification.buildSpecification(filters);

        // 5. Kết hợp thêm điều kiện id IN (...) và deletedAt IS NULL
        Specification<Patient> combinedSpec = spec
                .and((root, query, cb) -> root.get("id").in(patientIds))
                .and((root, query, cb) -> cb.isNull(root.get("deletedAt")));

        // 6. Truy vấn DB và ánh xạ về DTO
        Page<Patient> pageResult = patientRepository.findAll(combinedSpec, pageable);
        return pageResult.map(patientMapper::toPatientResponse);
    }


}
