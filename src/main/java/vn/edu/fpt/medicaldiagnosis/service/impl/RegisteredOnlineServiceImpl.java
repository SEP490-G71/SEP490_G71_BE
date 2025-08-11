package vn.edu.fpt.medicaldiagnosis.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.dto.request.RegisteredOnlineRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.RegisteredOnlineStatusRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.RegisteredOnlineResponse;
import vn.edu.fpt.medicaldiagnosis.entity.EmailTask;
import vn.edu.fpt.medicaldiagnosis.entity.RegisteredOnline;
import vn.edu.fpt.medicaldiagnosis.enums.Status;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.RegisteredOnlineMapper;
import vn.edu.fpt.medicaldiagnosis.repository.EmailTaskRepository;
import vn.edu.fpt.medicaldiagnosis.repository.RegisteredOnlineRepository;
import vn.edu.fpt.medicaldiagnosis.service.RegisteredOnlineService;
import vn.edu.fpt.medicaldiagnosis.specification.RegisteredOnlineSpecification;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegisteredOnlineServiceImpl implements RegisteredOnlineService {

    private final RegisteredOnlineRepository repository;
    private final RegisteredOnlineMapper mapper;
    private final EmailTaskRepository emailTaskRepository;

    @Override
    @Transactional
    public RegisteredOnlineResponse create(RegisteredOnlineRequest request) {
        // Kiểm tra ngày đăng ký: phải >= ngày hiện tại + 2
        LocalDate minAllowedDate = LocalDate.now().plusDays(2);
        LocalDate requestDate = request.getRegisteredAt().toLocalDate();

        if (requestDate.isBefore(minAllowedDate)) {
            throw new AppException(ErrorCode.REGISTERED_DATE_TOO_SOON);
        }

        // Tìm bản ghi theo email hoặc số điện thoại
        Optional<RegisteredOnline> existingOpt = repository.findByEmailOrPhoneNumberAndDeletedAtIsNull(
                request.getEmail(), request.getPhoneNumber()
        );

        // Nếu đã đăng ký và đang ACTIVE thì không cho phép đăng ký lại
        if (existingOpt.isPresent() && existingOpt.get().getStatus() == Status.ACTIVE) {
            throw new AppException(ErrorCode.REGISTERED_ALREADY_ACTIVE);
        }

        RegisteredOnline entity = existingOpt.orElseGet(() -> mapper.toEntity(request));
        entity.setRegisteredAt(request.getRegisteredAt());
        entity.setMessage(request.getMessage());
        entity.setStatus(Status.ACTIVE);
        entity.setVisitCount(1);

        RegisteredOnline saved = repository.save(entity);

        // Tạo email xác nhận
        emailTaskRepository.save(
                EmailTask.builder()
                        .id(UUID.randomUUID().toString())
                        .emailTo(request.getEmail())
                        .subject("Xác nhận đăng ký khám bệnh thành công")
                        .content(buildRegistrationEmailContent(request))
                        .retryCount(0)
                        .status(Status.PENDING)
                        .build()
        );

        return mapper.toResponse(saved);
    }

    private String buildRegistrationEmailContent(RegisteredOnlineRequest request) {
        String fullName = request.getLastName() + " " + request.getMiddleName() + " " + request.getFirstName();
        String registeredDate = request.getRegisteredAt().toLocalDate().toString();

        return String.format(
                "Chào bạn <strong>%s</strong>,<br/><br/>" +
                        "Bạn đã đăng ký khám bệnh thành công vào ngày <strong>%s</strong>.<br/>" +
                        "Chúng tôi sẽ liên hệ với bạn nếu có thay đổi.<br/><br/>" +
                        "Trân trọng,<br/>" +
                        "<strong>Phòng khám</strong>",
                fullName, registeredDate
        );
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

        return mapper.toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public RegisteredOnlineResponse updateStatus(String id, RegisteredOnlineStatusRequest request) {
        // Tìm bản ghi theo ID, chỉ lấy bản ghi chưa bị xoá
        RegisteredOnline entity = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.REGISTERED_ONLINE_NOT_FOUND));

        // Cập nhật trạng thái nếu được truyền
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }

        // Cập nhật isConfirmed nếu được truyền
        if (request.getIsConfirmed() != null) {
            entity.setIsConfirmed(request.getIsConfirmed());
        }

        // Lưu và trả về response
        return mapper.toResponse(repository.save(entity));
    }

}
