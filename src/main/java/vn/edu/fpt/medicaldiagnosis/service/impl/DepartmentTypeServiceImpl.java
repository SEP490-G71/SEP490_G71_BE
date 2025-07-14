package vn.edu.fpt.medicaldiagnosis.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.dto.request.DepartmentTypeRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.DepartmentTypeResponse;
import vn.edu.fpt.medicaldiagnosis.entity.DepartmentType;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.repository.DepartmentTypeRepository;
import vn.edu.fpt.medicaldiagnosis.service.DepartmentTypeService;
import vn.edu.fpt.medicaldiagnosis.specification.DepartmentTypeSpecification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PRIVATE;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class DepartmentTypeServiceImpl implements DepartmentTypeService {
    DepartmentTypeRepository repository;

    @Override
    public DepartmentTypeResponse create(DepartmentTypeRequest request) {
        log.info("Creating department type: {}", request);

        if (repository.existsByNameIgnoreCaseAndDeletedAtIsNull(request.getName())) {
            throw new AppException(ErrorCode.DEPARTMENT_TYPE_NAME_EXISTED);
        }

        DepartmentType entity = DepartmentType.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        repository.save(entity);
        return mapToResponse(entity);
    }

    @Override
    public DepartmentTypeResponse update(String id, DepartmentTypeRequest request) {
        log.info("Updating department type: {}", request);
        DepartmentType entity = repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_TYPE_NOT_FOUND));

        if (!entity.getName().equalsIgnoreCase(request.getName()) &&
                repository.existsByNameIgnoreCaseAndDeletedAtIsNull(request.getName())) {
            throw new AppException(ErrorCode.DEPARTMENT_TYPE_NAME_EXISTED);
        }


        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        repository.save(entity);
        return mapToResponse(entity);
    }

    @Override
    @Transactional
    public void delete(String id) {
        log.info("Deleting department type: {}", id);
        DepartmentType entity = repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_TYPE_NOT_FOUND));
        entity.setDeletedAt(LocalDateTime.now());
        repository.save(entity);
    }

    @Override
    public DepartmentTypeResponse getById(String id) {
        log.info("Getting department type by id: {}", id);
        DepartmentType entity = repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_TYPE_NOT_FOUND));
        return mapToResponse(entity);
    }

    @Override
    public List<DepartmentTypeResponse> getAll() {
        log.info("Getting all department types");
        return repository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Page<DepartmentTypeResponse> getPaged(Map<String, String> filters, int page, int size, String sortBy, String sortDir) {
        log.info("Getting paged department types");

        String sortColumn = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortColumn).ascending()
                : Sort.by(sortColumn).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Specification<DepartmentType> spec = DepartmentTypeSpecification.buildSpecification(filters);

        Page<DepartmentType> pageResult = repository.findAll(spec, pageable);

        return pageResult.map(this::mapToResponse);
    }


    private DepartmentTypeResponse mapToResponse(DepartmentType entity) {
        DepartmentTypeResponse response = new DepartmentTypeResponse();
        response.setId(entity.getId());
        response.setName(entity.getName());
        response.setDescription(entity.getDescription());
        return response;
    }
}
