package vn.edu.fpt.medicaldiagnosis.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.dto.request.RoomTransferRequestDTO;
import vn.edu.fpt.medicaldiagnosis.dto.response.RoomTransferResponseDTO;
import vn.edu.fpt.medicaldiagnosis.entity.*;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.repository.*;
import vn.edu.fpt.medicaldiagnosis.service.RoomTransferService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RoomTransferServiceImpl implements RoomTransferService {
    MedicalRecordRepository medicalRecordRepository;
    RoomTransferHistoryRepository roomTransferHistoryRepository;
    QueuePatientsRepository queuePatientsRepository;
    StaffRepository staffRepository;
    AccountRepository accountRepository;
    DepartmentRepository departmentRepository;
    @Override
    public RoomTransferResponseDTO createTransfer(String medicalRecordId, RoomTransferRequestDTO request) {
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
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND, "Không tìm thấy phòng/khoa đích"));

        Department fromDept = departmentRepository.findByRoomNumberAndDeletedAtIsNull(queuePatient.getRoomNumber())
                .orElseThrow(() -> new AppException(ErrorCode.DEPARTMENT_NOT_FOUND, "Không tìm thấy phòng/khoa nguồn"));

        if (fromDept.getId().equals(toDept.getId())) {
            throw new AppException(ErrorCode.SAME_ROOM, "Phòng chuyển đến phải khác phòng hiện tại");
        }

        Staff doctor = staffRepository.findByIdAndDeletedAtIsNull(request.getDoctorId())
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND, "Không tìm thấy bác sĩ"));



        // 5) Build entity
        RoomTransferHistory entity = RoomTransferHistory.builder()
                .medicalRecord(record)
                .fromDepartment(fromDept)
                .toDepartment(toDept)
                .transferredBy(transferredBy)
                .transferTime(LocalDateTime.now())
                .reason(request.getReason())
                .doctor(doctor)
                .conclusionText(request.getConclusionText())
                .isFinal(request.getIsFinal())
                .build();

        entity = roomTransferHistoryRepository.save(entity);

        // 6) Nếu là final → đồng bộ sang MedicalRecord.summary (tuỳ bạn, có thể bỏ)
        if (Boolean.TRUE.equals(request.getIsFinal()) && request.getConclusionText() != null) {
            record.setSummary(request.getConclusionText());
            medicalRecordRepository.save(record);
        }

        return toResponse(entity);
    }

    private RoomTransferResponseDTO toResponse(RoomTransferHistory e) {
        return RoomTransferResponseDTO.builder()
                .id(e.getId())
                .medicalRecordId(e.getMedicalRecord().getId())
                .fromDepartmentId(e.getFromDepartment() != null ? e.getFromDepartment().getId() : null)
                .toDepartmentId(e.getToDepartment() != null ? e.getToDepartment().getId() : null)
                .transferredById(e.getTransferredBy() != null ? e.getTransferredBy().getId() : null)
                .transferTime(e.getTransferTime())
                .reason(e.getReason())
                .doctorId(e.getDoctor() != null ? e.getDoctor().getId() : null)
                .conclusionText(e.getConclusionText())
                .isFinal(e.getIsFinal())
                .build();
    }
}
