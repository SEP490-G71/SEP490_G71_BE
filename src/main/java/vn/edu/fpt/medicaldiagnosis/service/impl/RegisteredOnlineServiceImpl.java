package vn.edu.fpt.medicaldiagnosis.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.dto.request.RegisteredOnlineRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.RegisteredOnlineResponse;
import vn.edu.fpt.medicaldiagnosis.entity.RegisteredOnline;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.RegisteredOnlineMapper;
import vn.edu.fpt.medicaldiagnosis.repository.RegisteredOnlineRepository;
import vn.edu.fpt.medicaldiagnosis.service.RegisteredOnlineService;
import vn.edu.fpt.medicaldiagnosis.specification.RegisteredOnlineSpecification;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegisteredOnlineServiceImpl implements RegisteredOnlineService {

    private final RegisteredOnlineRepository repository;
    private final RegisteredOnlineMapper mapper;

    @Override
    @Transactional
    public RegisteredOnlineResponse create(RegisteredOnlineRequest request) {
        // Tìm bản ghi cũ nếu trùng email và sđt
        Optional<RegisteredOnline> existing = repository.findByEmailAndPhoneNumberAndDeletedAtIsNull(
                request.getEmail(), request.getPhoneNumber()
        );

        RegisteredOnline saved;
        if (existing.isPresent()) {
            RegisteredOnline entity = existing.get();
            entity.setRegisteredAt(request.getRegisteredAt());
            entity.setMessage(request.getMessage());
            entity.setVisitCount(entity.getVisitCount() + 1);
            saved = repository.save(entity);
        } else {
            RegisteredOnline entity = mapper.toEntity(request);
            entity.setVisitCount(1);
            saved = repository.save(entity);
        }

        return mapper.toResponse(saved);
    }

    @Override
    public Page<RegisteredOnlineResponse> getPaged(Map<String, String> filters, int page, int size, String sortBy, String sortDir) {
        Specification<RegisteredOnline> spec = RegisteredOnlineSpecification.buildSpecification(filters);
        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        return repository.findAll(spec, pageable)
                .map(mapper::toResponse);
    }

    @Override
    public RegisteredOnlineResponse getById(String id) {
        RegisteredOnline entity = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.REGISTERED_ONLINE_NOT_FOUND));
        return mapper.toResponse(entity);
    }

    @Override
    public void delete(String id) {
        RegisteredOnline entity = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.REGISTERED_ONLINE_NOT_FOUND));
        entity.setDeletedAt(LocalDateTime.now());
        repository.save(entity);
    }
}
