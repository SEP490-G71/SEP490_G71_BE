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
import vn.edu.fpt.medicaldiagnosis.enums.Status;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.RegisteredOnlineMapper;
import vn.edu.fpt.medicaldiagnosis.repository.RegisteredOnlineRepository;
import vn.edu.fpt.medicaldiagnosis.service.RegisteredOnlineService;
import vn.edu.fpt.medicaldiagnosis.specification.RegisteredOnlineSpecification;

import java.time.LocalDate;
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
        // Kiểm tra ngày đăng ký: phải >= ngày hiện tại + 2
        LocalDate minAllowedDate = LocalDate.now().plusDays(2);
        LocalDate requestDate = request.getRegisteredAt().toLocalDate();

        if (requestDate.isBefore(minAllowedDate)) {
            throw new AppException(ErrorCode.REGISTERED_DATE_TOO_SOON);
        }


        // Tìm bản ghi theo email và số điện thoại
        Optional<RegisteredOnline> existingOpt = repository.findByEmailAndPhoneNumberAndDeletedAtIsNull(
                request.getEmail(), request.getPhoneNumber()
        );

        // Nếu tồn tại bản ghi ACTIVE thì không cho phép đăng ký lại
        if (existingOpt.isPresent() && existingOpt.get().getStatus() == Status.ACTIVE) {
            throw new AppException(ErrorCode.REGISTERED_ALREADY_ACTIVE);
        }

        RegisteredOnline saved;
        if (existingOpt.isPresent()) {
            RegisteredOnline entity = existingOpt.get();
            entity.setRegisteredAt(request.getRegisteredAt());
            entity.setMessage(request.getMessage());
            entity.setVisitCount(entity.getVisitCount() + 1);
            saved = repository.save(entity);
        } else {
            RegisteredOnline entity = mapper.toEntity(request);
            entity.setVisitCount(1);
            entity.setStatus(Status.ACTIVE);
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

    @Override
    @Transactional
    public RegisteredOnlineResponse update(String id, RegisteredOnlineRequest request) {
        // Kiểm tra ngày đăng ký: phải >= ngày hiện tại + 2
        LocalDate minAllowedDate = LocalDate.now().plusDays(2);
        LocalDate requestDate = request.getRegisteredAt().toLocalDate();

        if (requestDate.isBefore(minAllowedDate)) {
            throw new AppException(ErrorCode.REGISTERED_DATE_TOO_SOON);
        }

        // Tìm bản ghi theo ID
        RegisteredOnline entity = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.REGISTERED_ONLINE_NOT_FOUND));

        // Cập nhật thông tin
        entity.setFirstName(request.getFirstName());
        entity.setMiddleName(request.getMiddleName());
        entity.setLastName(request.getLastName());
        entity.setDob(request.getDob());
        entity.setGender(request.getGender());
        entity.setEmail(request.getEmail());
        entity.setPhoneNumber(request.getPhoneNumber());
        entity.setRegisteredAt(request.getRegisteredAt());
        entity.setMessage(request.getMessage());
        entity.setStatus(request.getStatus() != null ? request.getStatus() : entity.getStatus());

        return mapper.toResponse(repository.save(entity));
    }

}
