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
import vn.edu.fpt.medicaldiagnosis.dto.request.RoomTransferRequestDTO;
import vn.edu.fpt.medicaldiagnosis.dto.response.MedicalRecordResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.RoomTransferResponseDTO;
import vn.edu.fpt.medicaldiagnosis.dto.response.RoomTransferResponsePagination;
import vn.edu.fpt.medicaldiagnosis.entity.*;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.DepartmentMapper;
import vn.edu.fpt.medicaldiagnosis.mapper.StaffMapper;
import vn.edu.fpt.medicaldiagnosis.repository.*;
import vn.edu.fpt.medicaldiagnosis.service.RoomTransferService;
import vn.edu.fpt.medicaldiagnosis.specification.RoomTransferHistorySpecification;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RoomTransferServiceImpl implements RoomTransferService {
    MedicalRecordRepository medicalRecordRepository;
    RoomTransferHistoryRepository roomTransferHistoryRepository;
    QueuePatientsRepository queuePatientsRepository;
    StaffRepository staffRepository;
    AccountRepository accountRepository;
    DepartmentRepository departmentRepository;
    DepartmentMapper departmentMapper;
    StaffMapper staffMapper;
    @Override
    public RoomTransferResponseDTO createTransfer(String medicalRecordId, RoomTransferRequestDTO request) {
        log.info("Service: create room transfer");
        // 1) Load medical record
        MedicalRecord record = medicalRecordRepository.findByIdAndDeletedAtIsNull(medicalRecordId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND, "Không tìm thấy hồ sơ bệnh án"));

        // 2) Current staff (transferredBy)
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        Account account = accountRepository.findByUsernameAndDeletedAtIsNull(username)
                .orElseThrow(() -> new AppException(ErrorCode.UNAUTHORIZED, "Không tìm thấy tài khoản đăng nhập"));
        Staff transferredBy = staffRepository.findByAccountIdAndDeletedAtIsNull(account.getId())
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND, "Không tìm thấy thông tin nhân viên"));

        QueuePatients queuePatient = record.getVisit();
        if (queuePatient == null) {
            throw new AppException(ErrorCode.QUEUE_PATIENT_NOT_FOUND, "Không tìm thấy lượt khám liên quan");
        }

        // 3) Resolve departments
        Department toDept = departmentRepository.findByIdAndDeletedAtIsNull(request.getToDepartmentNumber())
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND, "Không tìm thấy phòng muốn chuyển tới"));

        Department fromDept = departmentRepository.findByRoomNumberAndDeletedAtIsNull(queuePatient.getRoomNumber())
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND, "Không tìm thấy phòng ban đầu"));

        if (fromDept.getId().equals(toDept.getId())) {
            throw new AppException(ErrorCode.SAME_ROOM, "Phòng chuyển đến phải khác phòng hiện tại");
        }

        // 5) Build entity
        RoomTransferHistory entity = RoomTransferHistory.builder()
                .medicalRecord(record)
                .fromDepartment(fromDept)
                .toDepartment(toDept)
                .transferredBy(transferredBy)
                .transferTime(LocalDateTime.now())
                .reason(request.getReason())
                .build();

        entity = roomTransferHistoryRepository.save(entity);

        return toResponse(entity);
    }

    @Override
    public Page<RoomTransferResponsePagination> getRoomTransfersPaged(Map<String, String> filters,
                                                                      int page, int size,
                                                                      String sortBy, String sortDir) {
        String sortColumn = (sortBy == null || sortBy.isBlank()) ? "transferTime" : sortBy;
        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortColumn).ascending()
                : Sort.by(sortColumn).descending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Specification<RoomTransferHistory> spec = RoomTransferHistorySpecification.build(filters);

        Page<RoomTransferHistory> rs = roomTransferHistoryRepository.findAll(spec, pageable);
        return rs.map(this::toDto);
    }

    private RoomTransferResponsePagination toDto(RoomTransferHistory e) {

            var mr = e.getMedicalRecord();

            MedicalRecordResponse medicalRecordDto = MedicalRecordResponse.builder()
                    .id(mr.getId())
                    .medicalRecordCode(mr.getMedicalRecordCode())
                    .patientName(mr.getPatient() != null ? mr.getPatient().getFullName() : null)
                    .doctorName(mr.getCreatedBy() != null ? mr.getCreatedBy().getFullName() : null)
                    .status(String.valueOf(mr.getStatus()))
                    .createdAt(mr.getCreatedAt())
                    .build();

            return RoomTransferResponsePagination.builder()
                    .id(e.getId())
                    .medicalRecord(medicalRecordDto)
                    .fromDepartment(departmentMapper.toDepartmentBasicInfo(e.getFromDepartment()))
                    .toDepartment(departmentMapper.toDepartmentBasicInfo(e.getToDepartment()))
                    .transferredBy(staffMapper.toBasicResponse(e.getTransferredBy()))
                    .transferTime(e.getTransferTime())
                    .reason(e.getReason())
                    .doctor(staffMapper.toBasicResponse(e.getDoctor()))
                    .conclusionText(e.getConclusionText())
                    .isFinal(e.getIsFinal())
                    .build();


    }

    private RoomTransferResponseDTO toResponse(RoomTransferHistory e) {
        return RoomTransferResponseDTO.builder()
                .id(e.getId())
                .medicalRecordId(e.getMedicalRecord().getId())
                .fromDepartment(departmentMapper.toDepartmentBasicInfo(e.getFromDepartment()))
                .toDepartment(departmentMapper.toDepartmentBasicInfo(e.getToDepartment()))
                .transferredBy(staffMapper.toBasicResponse(e.getTransferredBy()))
                .transferTime(e.getTransferTime())
                .reason(e.getReason())
                .doctor(staffMapper.toBasicResponse(e.getDoctor()))
                .conclusionText(e.getConclusionText())
                .isFinal(e.getIsFinal())
                .build();
    }
}
