package vn.edu.fpt.medicaldiagnosis.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.MedicalRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.UpdateMedicalRecordRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.*;
import vn.edu.fpt.medicaldiagnosis.service.MedicalRecordService;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/medical-records")
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class MedicalRecordController {
    MedicalRecordService medicalRecordService;

    @PostMapping
    public ApiResponse<MedicalResponse> createMedicalRecord(@RequestBody @Valid MedicalRequest request) {
        log.info("Controller: Creating medical record with request: {}", request);
        ApiResponse<MedicalResponse> response = new ApiResponse<>();
        response.setResult(medicalRecordService.createMedicalRecord(request));
        return response;
    }

    @GetMapping("/{recordId}")
    public ApiResponse<MedicalRecordDetailResponse> getMedicalRecordDetail(@PathVariable String recordId) {
        return ApiResponse.<MedicalRecordDetailResponse>builder()
                .message("Get medical record detail successfully")
                .result(medicalRecordService.getMedicalRecordDetail(recordId))
                .build();
    }

    @GetMapping("/history/{patientId}")
    public ApiResponse<List<MedicalRecordResponse>> getMedicalRecordHistory(@PathVariable String patientId) {
        return ApiResponse.<List<MedicalRecordResponse>>builder()
                .message("Lấy lịch sử bệnh án thành công")
                .result(medicalRecordService.getMedicalRecordHistory(patientId))
                .build();
    }

    @PutMapping("/{recordId}")
    public ApiResponse<MedicalRecordDetailResponse> updateMedicalRecord(
            @PathVariable String recordId,
            @RequestBody @Valid UpdateMedicalRecordRequest request
    ) {
        MedicalRecordDetailResponse updatedRecord = medicalRecordService.updateMedicalRecord(recordId, request);
        return ApiResponse.<MedicalRecordDetailResponse>builder()
                .message("Cập nhật hồ sơ bệnh án thành công")
                .result(updatedRecord)
                .build();
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<byte[]> previewMedicalRecord(@PathVariable String id) {
        log.info("Controller - Preview medical record: {}", id);
        ByteArrayInputStream pdfStream = medicalRecordService.generateMedicalRecordPdf(id);
        byte[] pdfBytes;

        pdfBytes = pdfStream.readAllBytes();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline()
                .filename("medical-record-" + id + ".pdf").build());

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadMedicalRecord(@PathVariable String id) {
        log.info("Controller - Download medical record: {}", id);
        ByteArrayInputStream pdfStream = medicalRecordService.generateMedicalRecordPdf(id);
        byte[] pdfBytes;

        pdfBytes = pdfStream.readAllBytes();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("medical-record-" + id + ".pdf").build());

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping
    public ApiResponse<PagedResponse<MedicalRecordResponse>> getMedicalRecords(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        log.info("Controller: get medical records with filters={}, page={}, size={}, sortBy={}, sortDir={}",
                filters, page, size, sortBy, sortDir);

        Page<MedicalRecordResponse> result = medicalRecordService.getMedicalRecordsPaged(filters, page, size, sortBy, sortDir);

        PagedResponse<MedicalRecordResponse> response = new PagedResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );
        return ApiResponse.<PagedResponse<MedicalRecordResponse>>builder().result(response).build();
    }

    @GetMapping("/room/{roomNumber}")
    public ApiResponse<PagedResponse<MedicalRecordResponse>> getMedicalRecordsByRoomNumber(
            @PathVariable String roomNumber,
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        log.info("Get medical records by roomNumber={}, filters={}, page={}, size={}, sortBy={}, sortDir={}",
                roomNumber, filters, page, size, sortBy, sortDir);

        // Thêm roomNumber vào filters để xử lý chung
        filters.put("roomNumber", roomNumber);

        Page<MedicalRecordResponse> result = medicalRecordService
                .getMedicalRecordsByRoomNumber(filters, page, size, sortBy, sortDir);

        PagedResponse<MedicalRecordResponse> response = new PagedResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );

        return ApiResponse.<PagedResponse<MedicalRecordResponse>>builder().result(response).build();
    }


    @GetMapping("/orders/department/{departmentId}")
    public ApiResponse<List<MedicalRecordOrderResponse>> getOrdersByDepartment(
            @PathVariable String departmentId
    ) {
        return ApiResponse.<List<MedicalRecordOrderResponse>>builder()
                .message("Lấy danh sách order bệnh án theo phòng ban thành công")
                .result(medicalRecordService.getOrdersByDepartment(departmentId))
                .build();
    }
}
