package vn.edu.fpt.medicaldiagnosis.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.dto.request.RoomTransferRequestDTO;
import vn.edu.fpt.medicaldiagnosis.dto.response.RoomTransferResponseDTO;
import vn.edu.fpt.medicaldiagnosis.entity.MedicalRecord;
import vn.edu.fpt.medicaldiagnosis.entity.QueuePatients;
import vn.edu.fpt.medicaldiagnosis.entity.RoomTransferHistory;
import vn.edu.fpt.medicaldiagnosis.entity.Staff;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.repository.MedicalRecordRepository;
import vn.edu.fpt.medicaldiagnosis.repository.QueuePatientsRepository;
import vn.edu.fpt.medicaldiagnosis.repository.RoomTransferHistoryRepository;
import vn.edu.fpt.medicaldiagnosis.repository.StaffRepository;
import vn.edu.fpt.medicaldiagnosis.service.RoomTransferService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomTransferServiceImpl implements RoomTransferService {
    MedicalRecordRepository medicalRecordRepository;
    RoomTransferHistoryRepository roomTransferHistoryRepository;
    QueuePatientsRepository queuePatientsRepository;
    StaffRepository staffRepository;
    @Override
    public RoomTransferResponseDTO transferRoom(String medicalRecordId, RoomTransferRequestDTO request) {
        // 1. Lấy bệnh án
        MedicalRecord medicalRecord = medicalRecordRepository.findById(medicalRecordId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND, "Không tìm thấy hồ sơ bệnh án"));

        // 2. Lấy lượt khám liên quan
        QueuePatients queuePatient = medicalRecord.getVisit();
        if (queuePatient == null) {
            throw new AppException(ErrorCode.QUEUE_PATIENT_NOT_FOUND, "Không tìm thấy lượt khám liên quan");
        }

        // 3. Lấy người chuyển
        Staff staff = staffRepository.findById(request.getTransferredById())
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND, "Không tìm thấy người chuyển"));

        String fromRoom = queuePatient.getRoomNumber();
        String toRoom = request.getToRoomNumber();

        if (fromRoom.equalsIgnoreCase(toRoom)) {
            throw new AppException(ErrorCode.SAME_ROOM, "Phòng chuyển đến phải khác phòng hiện tại");
        }

        // 4. Lưu lịch sử chuyển phòng
        RoomTransferHistory history = RoomTransferHistory.builder()
                .medicalRecord(medicalRecord)
                .fromRoomNumber(fromRoom)
                .toRoomNumber(toRoom)
                .transferTime(LocalDateTime.now())
                .reason(request.getReason())
                .transferredBy(staff)
                .build();

        roomTransferHistoryRepository.save(history);

        // 5. Cập nhật phòng hiện tại
        queuePatient.setRoomNumber(toRoom);
        queuePatientsRepository.save(queuePatient);

        // 6. Trả về DTO phản hồi
        return RoomTransferResponseDTO.builder()
                .medicalRecordId(medicalRecord.getId())
                .fromRoomNumber(fromRoom)
                .toRoomNumber(toRoom)
                .transferredBy(staff.getFullName())
                .reason(request.getReason())
                .transferTime(history.getTransferTime())
                .build();
    }
}
