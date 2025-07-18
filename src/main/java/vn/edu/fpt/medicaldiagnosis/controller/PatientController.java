package vn.edu.fpt.medicaldiagnosis.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.PatientRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.PagedResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.PatientResponse;
import vn.edu.fpt.medicaldiagnosis.service.PatientService;
import vn.edu.fpt.medicaldiagnosis.service.impl.ExportServiceImpl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/patients")
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class PatientController {

    PatientService patientService;
    ExportServiceImpl exportService;

    @PostMapping
    public ApiResponse<PatientResponse> createPatient(@RequestBody @Valid PatientRequest request) {
        return ApiResponse.<PatientResponse>builder()
                .result(patientService.createPatient(request))
                .build();
    }

    @GetMapping("/all")
    public ApiResponse<List<PatientResponse>> getAllPatients() {
        return ApiResponse.<List<PatientResponse>>builder()
                .result(patientService.getAllPatients())
                .build();
    }

    @GetMapping
    public ApiResponse<PagedResponse<PatientResponse>> getPatients(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        log.info("Controller: get patients with filters={}, page={}, size={}, sortBy={}, sortDir={}",
                filters, page, size, sortBy, sortDir);

        Page<PatientResponse> result = patientService.getPatientsPaged(filters, page, size, sortBy, sortDir);

        PagedResponse<PatientResponse> response = new PagedResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );

        return ApiResponse.<PagedResponse<PatientResponse>>builder().result(response).build();
    }


    @GetMapping("/{id}")
    public ApiResponse<PatientResponse> getPatientById(@PathVariable String id) {
        return ApiResponse.<PatientResponse>builder()
                .result(patientService.getPatientById(id))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<PatientResponse> updatePatient(@PathVariable String id,
                                                      @RequestBody @Valid PatientRequest request) {
        return ApiResponse.<PatientResponse>builder()
                .result(patientService.updatePatient(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deletePatient(@PathVariable String id) {
        patientService.deletePatient(id);
        return ApiResponse.<String>builder().message("Patient deleted successfully.").build();
    }

    @GetMapping("/today")
    public ApiResponse<PagedResponse<PatientResponse>> getPatientsRegisteredToday(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        log.info("Controller: get today's patients with filters={}, page={}, size={}, sortBy={}, sortDir={}",
                filters, page, size, sortBy, sortDir);

        Page<PatientResponse> result = patientService.getPatientsRegisteredTodayPaged(filters, page, size, sortBy, sortDir);

        PagedResponse<PatientResponse> response = new PagedResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );

        return ApiResponse.<PagedResponse<PatientResponse>>builder().result(response).build();
    }

    @GetMapping("/search")
    public ApiResponse<List<PatientResponse>> searchPatients(
            @RequestParam(value = "search", required = false) String keyword) {

        List<PatientResponse> results;

        if (keyword == null || keyword.isBlank()) {
            results = patientService.getAllPatients();
        } else {
            results = patientService.searchByNameOrCode(keyword);
        }

        return ApiResponse.<List<PatientResponse>>builder()
                .result(results)
                .build();
    }

    @GetMapping("/birthdays")
    public ApiResponse<PagedResponse<PatientResponse>> getPatientsWithBirthdaysInMonth(
            @RequestParam(defaultValue = "1") int month,
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        log.info("Controller: get patients with birthday in month={}, filters={}, page={}, size={}", month, filters, page, size);

        Page<PatientResponse> result = patientService.getPatientsWithBirthdaysInMonth(month, filters, page, size, sortBy, sortDir);

        return ApiResponse.<PagedResponse<PatientResponse>>builder()
                .result(new PagedResponse<>(
                        result.getContent(),
                        result.getNumber(),
                        result.getSize(),
                        result.getTotalElements(),
                        result.getTotalPages(),
                        result.isLast()
                ))
                .build();
    }

    @GetMapping("/birthdays-this-month/export")
    public ResponseEntity<byte[]> exportBirthdayPatientsToExcel(
            @RequestParam(defaultValue = "1") int month,
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) throws IOException {
        log.info("Controller: export birthday patients to excel with filters={}, sortBy={}, sortDir={}", filters, sortBy, sortDir);

        int currentMonth = month == 0 ? LocalDate.now().getMonthValue() : month;
        List<PatientResponse> patients = patientService.getAllPatientBirthdays(currentMonth, filters, sortBy, sortDir);

        ByteArrayInputStream in = exportService.exportPatientBirthdayToExcel(patients);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=birthday_patients_month_" + currentMonth + ".xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(in.readAllBytes());
    }

    @PostMapping("/send-birthday")
    public ApiResponse<String> sendBirthdayEmailsManually() {

        int targetMonth = LocalDate.now().getMonthValue();
        int totalCreated = patientService.generateBirthdayEmailsForCurrentTenant(targetMonth);

        if (totalCreated == 0) {
            return ApiResponse.<String>builder()
                    .message("Không có bệnh nhân nào sinh trong tháng " + targetMonth)
                    .code(400)
                    .build();
        }

        return ApiResponse.<String>builder()
                .result("Đã tạo " + totalCreated + " email sinh nhật cho tháng " + targetMonth)
                .build();
    }

}
