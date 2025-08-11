package vn.edu.fpt.medicaldiagnosis.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.RoomTransferRequestDTO;
import vn.edu.fpt.medicaldiagnosis.dto.request.ServicePackageRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.*;
import vn.edu.fpt.medicaldiagnosis.service.RoomTransferService;

import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/room-transfers")
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class RoomTransferController {
    RoomTransferService service;
    @PostMapping("/{medicalRecordId}/transfer")
    ApiResponse<RoomTransferResponseDTO> create(@Valid @RequestBody RoomTransferRequestDTO request, @PathVariable String medicalRecordId) {
        log.info("Controller: create room transfer for medical record {}", medicalRecordId);
        RoomTransferResponseDTO response = service.createTransfer(medicalRecordId, request);
        return ApiResponse.<RoomTransferResponseDTO>builder().result(response).build();
    }

    @GetMapping
    public ApiResponse<PagedResponse<RoomTransferResponsePagination>> getRoomTransfers(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "transferTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Page<RoomTransferResponsePagination> result =
                service.getRoomTransfersPaged(filters, page, size, sortBy, sortDir);

        PagedResponse<RoomTransferResponsePagination> response = new PagedResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );

        return ApiResponse.<PagedResponse<RoomTransferResponsePagination>>builder()
                .result(response).build();
    }

}
