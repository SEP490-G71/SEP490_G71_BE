package vn.edu.fpt.medicaldiagnosis.service;

import org.springframework.data.domain.Page;
import vn.edu.fpt.medicaldiagnosis.dto.request.RoomTransferRequestDTO;
import vn.edu.fpt.medicaldiagnosis.dto.response.RoomTransferResponseDTO;
import vn.edu.fpt.medicaldiagnosis.dto.response.RoomTransferResponsePagination;

import java.util.Map;

public interface RoomTransferService {
    RoomTransferResponseDTO createTransfer(String medicalRecordId, RoomTransferRequestDTO request);

    Page<RoomTransferResponsePagination> getRoomTransfersPaged(Map<String, String> filters,
                                                               int page, int size,
                                                               String sortBy, String sortDir);
}
