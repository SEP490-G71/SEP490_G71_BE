package vn.edu.fpt.medicaldiagnosis.service.impl;

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
import vn.edu.fpt.medicaldiagnosis.dto.request.MedicalServiceRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.MedicalServiceResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Department;
import vn.edu.fpt.medicaldiagnosis.entity.MedicalService;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.MedicalServiceMapper;
import vn.edu.fpt.medicaldiagnosis.repository.DepartmentRepository;
import vn.edu.fpt.medicaldiagnosis.repository.MedicalServiceRepository;
import vn.edu.fpt.medicaldiagnosis.service.DepartmentService;
import vn.edu.fpt.medicaldiagnosis.service.MedicalServiceService;
import vn.edu.fpt.medicaldiagnosis.specification.MedicalServiceSpecification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MedicalServiceServiceImpl implements MedicalServiceService {
    MedicalServiceRepository medicalServiceRepository;
    MedicalServiceMapper medicalServiceMapper;
    DepartmentRepository departmentRepository;
    CodeGeneratorService codeGeneratorService;

    @Override
    public MedicalServiceResponse createMedicalService(MedicalServiceRequest medicalServiceRequest) {
        log.info("Service: create medical service");
        log.info("Creating medical service: {}", medicalServiceRequest);

        Department department = departmentRepository.findByIdAndDeletedAtIsNull(medicalServiceRequest.getDepartmentId())
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));

        MedicalService medicalService = medicalServiceMapper.toMedicalService(medicalServiceRequest);
        String serviceCode = codeGeneratorService.generateCode("MEDICAL_SERVICE", "MS", 6);
        medicalService.setServiceCode(serviceCode);
        medicalService.setDepartment(department);
        medicalService = medicalServiceRepository.save(medicalService);
        log.info("Medical service created: {}", medicalService);

        return medicalServiceMapper.toMedicalServiceResponse(medicalService);
    }

    @Override
    public List<MedicalServiceResponse> getAllMedicalServices() {
        log.info("Service: get all medical services");
        List<MedicalService> services = medicalServiceRepository.findAllByDeletedAtIsNull();
        return services.stream().map(medicalServiceMapper::toMedicalServiceResponse).collect(Collectors.toList());
    }

    @Override
    public MedicalServiceResponse getMedicalServiceById(String id) {
        log.info("Service: get medical service by id: {}", id);
        MedicalService medicalService = medicalServiceRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_SERVICE_NOT_FOUND));
        return medicalServiceMapper.toMedicalServiceResponse(medicalService);
    }

    @Override
    public void deleteMedicalService(String id) {
        log.info("Service: delete medical service by id: {}", id);
        MedicalService medicalService = medicalServiceRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_SERVICE_NOT_FOUND));

        // ✅ Nếu là dịch vụ mặc định → không cho xóa
        if (Boolean.TRUE.equals(medicalService.isDefault())) {
            throw new AppException(ErrorCode.CANNOT_DELETE_DEFAULT_SERVICE);
        }

        medicalService.setDeletedAt(LocalDateTime.now());
        medicalServiceRepository.save(medicalService);
    }

    @Override
    public MedicalServiceResponse updateMedicalService(String id, MedicalServiceRequest medicalServiceRequest) {
        log.info("Service: update medical service {}", id);
        MedicalService medicalService = medicalServiceRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_SERVICE_NOT_FOUND));
        departmentRepository.findByIdAndDeletedAtIsNull(medicalServiceRequest.getDepartmentId())
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND));
        medicalServiceMapper.updateMedicalService(medicalService, medicalServiceRequest);
        return medicalServiceMapper.toMedicalServiceResponse(medicalServiceRepository.save(medicalService));
    }

    @Override
    public Page<MedicalServiceResponse> getMedicalServicesPaged(Map<String, String> filters, int page, int size, String sortBy, String sortDir) {
        log.info("Service: get medical services paged");
        String sortColumn = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortColumn).descending();

        Specification<MedicalService> spec = MedicalServiceSpecification.buildSpecification(filters);

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<MedicalService> pageResult = medicalServiceRepository.findAll(spec, pageable);

        return pageResult.map(medicalServiceMapper::toMedicalServiceResponse);
    }

    @Override
    public MedicalServiceResponse getDefaultServiceByDepartmentId(String departmentId) {
        MedicalService service = medicalServiceRepository
                .findFirstByDepartmentIdAndIsDefaultTrueAndDeletedAtIsNull(departmentId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_SERVICE_NOT_FOUND));

        return medicalServiceMapper.toMedicalServiceResponse(service);
    }

}
