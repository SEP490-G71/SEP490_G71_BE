package vn.edu.fpt.medicaldiagnosis.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.dto.request.ServicePackageRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ServicePackageResponse;
import vn.edu.fpt.medicaldiagnosis.entity.ServicePackage;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.ServicePackageMapper;
import vn.edu.fpt.medicaldiagnosis.repository.ServicePackageRepository;
import vn.edu.fpt.medicaldiagnosis.service.ServicePackageService;
import vn.edu.fpt.medicaldiagnosis.specification.ServicePackageSpecification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class ServicePackageServiceImpl implements ServicePackageService {

    ServicePackageRepository repo;
    ServicePackageMapper mapper;

    @Override
    public ServicePackageResponse create(ServicePackageRequest req) {
        log.info("Create: {}", req);

        if (repo.packageNameExists(req.getPackageName()) == 1) {
            throw new AppException(ErrorCode.DUPLICATE_SERVICE_PACKAGE_NAME);
        }

        ServicePackage entity = mapper.toEntity(req);
        return mapper.toResponse(repo.save(entity));
    }

    @Override
    public ServicePackageResponse update(String id, ServicePackageRequest req) {
        log.info("Update: {}", id);
        ServicePackage existing = repo.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.SERVICE_PACKAGE_NOT_FOUND));

        boolean nameChanged = !existing.getPackageName().equalsIgnoreCase(req.getPackageName());
        if (nameChanged && repo.packageNameExists(req.getPackageName()) == 1) {
            throw new AppException(ErrorCode.DUPLICATE_SERVICE_PACKAGE_NAME);
        }

        mapper.updateEntityFromRequest(req, existing);
        return mapper.toResponse(repo.save(existing));
    }

    @Override
    public void delete(String id) {
        log.info("Delete: {}", id);
        ServicePackage entity = repo.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.SERVICE_PACKAGE_NOT_FOUND));
        entity.setDeletedAt(LocalDateTime.now());
        repo.save(entity);
    }

    @Override
    public ServicePackageResponse getById(String id) {
        return repo.findByIdAndDeletedAtIsNull(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new AppException(ErrorCode.SERVICE_PACKAGE_NOT_FOUND));
    }

    @Override
    public List<ServicePackageResponse> getAll() {
        return repo.findAll().stream()
                .filter(pkg -> pkg.getDeletedAt() == null)
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public Page<ServicePackageResponse> getServicePackagesPaged(
            Map<String, String> filters, int page, int size, String sortBy, String sortDir) {

        Sort sort = Sort.by(
                sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC,
                (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy
        );
        Pageable pageable = PageRequest.of(page, size, sort);
        Specification<ServicePackage> spec = ServicePackageSpecification.buildSpecification(filters);

        return repo.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Override
    public List<ServicePackageResponse> getByTenantId(String tenantId) {
        return repo.findByTenantId(tenantId).stream()
                .filter(pkg -> pkg.getDeletedAt() == null)
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public List<ServicePackageResponse> getByTenantIdAndStatus(String tenantId, String status) {
        return repo.findByTenantIdAndStatus(tenantId, status).stream()
                .filter(pkg -> pkg.getDeletedAt() == null)
                .map(mapper::toResponse)
                .toList();
    }
}
