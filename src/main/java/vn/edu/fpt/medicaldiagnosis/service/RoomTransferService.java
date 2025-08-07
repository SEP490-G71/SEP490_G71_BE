package vn.edu.fpt.medicaldiagnosis.service;

import vn.edu.fpt.medicaldiagnosis.dto.request.RoomTransferRequestDTO;
import vn.edu.fpt.medicaldiagnosis.dto.response.RoomTransferResponseDTO;

public interface RoomTransferService {
    RoomTransferResponseDTO transferRoom(String medicalRecordId, RoomTransferRequestDTO request);
}
