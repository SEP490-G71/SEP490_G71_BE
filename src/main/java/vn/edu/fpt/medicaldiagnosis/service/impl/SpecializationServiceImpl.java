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
import vn.edu.fpt.medicaldiagnosis.dto.request.SpecializationRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.SpecializationResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Specialization;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.repository.SpecializationRepository;
import vn.edu.fpt.medicaldiagnosis.service.SpecializationService;
import vn.edu.fpt.medicaldiagnosis.specification.SpecializationSpecification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class SpecializationServiceImpl implements SpecializationService {

    SpecializationRepository specializationRepository;

    @Override
    public SpecializationResponse createSpecialization(SpecializationRequest request) {
        log.info("Service: create specialization");
        if (specializationRepository.existsByNameIgnoreCase(request.getName())) {
            throw new AppException(ErrorCode.SPECIALIZATION_NAME_EXISTS);
        }

        Specialization specialization = Specialization.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();

        specializationRepository.save(specialization);

        return mapToResponse(specialization);
    }

    @Override
    public SpecializationResponse updateSpecialization(String id, SpecializationRequest request) {
        log.info("Service: update specialization {}", id);

        Specialization specialization = specializationRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SPECIALIZATION_NOT_FOUND));

        boolean nameExists = specializationRepository.existsByNameIgnoreCase(request.getName())
                && !specialization.getName().equalsIgnoreCase(request.getName());

        if (nameExists) {
            throw new AppException(ErrorCode.SPECIALIZATION_NAME_EXISTS);
        }

        specialization.setName(request.getName());
        specialization.setDescription(request.getDescription());
        specialization.setUpdatedAt(LocalDateTime.now());

        specializationRepository.save(specialization);
        return mapToResponse(specialization);
    }


    @Override
    public void deleteSpecialization(String id) {
        log.info("Service: delete specialization {}", id);
        Specialization specialization = specializationRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SPECIALIZATION_NOT_FOUND));

        specialization.setDeletedAt(LocalDateTime.now());
        specializationRepository.save(specialization);
    }

    @Override
    public List<SpecializationResponse> getAllSpecializations(String search) {
        log.info("Service: get all specializations with search = {}", search);

        List<Specialization> specializations;

        if (search != null && !search.trim().isEmpty()) {
            specializations = specializationRepository
                    .findByNameContainingIgnoreCaseAndDeletedAtIsNull(search.trim());
        } else {
            specializations = specializationRepository.findAll();
        }

        return specializations.stream()
                .map(this::mapToResponse)
                .toList();
    }


    @Override
    public Page<SpecializationResponse> getSpecializationsPaged(
            Map<String, String> filters,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        log.info("Service: get specializations paged");

        // ✅ Chuẩn hóa sortBy
        String sortColumn = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortColumn).ascending() : Sort.by(sortColumn).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Specification<Specialization> spec = SpecializationSpecification.buildSpecification(filters);

        Page<Specialization> pageResult = specializationRepository.findAll(spec, pageable);
        return pageResult.map(this::mapToResponse);
    }


    @Override
    public SpecializationResponse getSpecializationById(String id) {
        Specialization specialization = specializationRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SPECIALIZATION_NOT_FOUND));

        return mapToResponse(specialization);
    }

    private SpecializationResponse mapToResponse(Specialization specialization) {
        return SpecializationResponse.builder()
                .id(specialization.getId())
                .name(specialization.getName())
                .description(specialization.getDescription())
                .build();
    }
}
