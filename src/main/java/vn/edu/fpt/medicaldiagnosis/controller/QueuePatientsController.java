package vn.edu.fpt.medicaldiagnosis.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.medicaldiagnosis.dto.request.QueuePatientsRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.*;
import vn.edu.fpt.medicaldiagnosis.service.QueuePatientsService;
import vn.edu.fpt.medicaldiagnosis.service.QueuePollingService;

import java.util.List;
import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/queue-patients")
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class QueuePatientsController {

    QueuePatientsService queuePatientsService;

    QueuePollingService queuePollingService;

    @PostMapping
    public ApiResponse<QueuePatientsResponse> createQueuePatients(@RequestBody @Valid QueuePatientsRequest request) {
        log.info("Controller: create queue patient {}", request);
        return ApiResponse.<QueuePatientsResponse>builder()
                .result(queuePatientsService.createQueuePatients(request))
                .build();
    }

    @PostMapping("/import")
    public ApiResponse<List<QueuePatientsResponse>> importQueuePatients(@RequestParam("file") MultipartFile file) {
        log.info("Controller: import queue patients from excel file {}", file.getOriginalFilename());

        List<QueuePatientsResponse> responses = queuePatientsService.importQueuePatientsFromExcel(file);

        return ApiResponse.<List<QueuePatientsResponse>>builder()
                .result(responses)
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<QueuePatientsResponse> update(@PathVariable String id, @RequestBody @Valid QueuePatientsRequest request) {
        log.info("Controller: update queue patient {}", id);
        return ApiResponse.<QueuePatientsResponse>builder()
                .result(queuePatientsService.updateQueuePatients(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteQueuePatients(@PathVariable String id) {
        log.info("Controller: delete queue patient {}", id);
        queuePatientsService.deleteQueuePatients(id);
        return ApiResponse.<String>builder()
                .message("QueuePatient deleted successfully.")
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<QueuePatientsResponse> getQueuePatientsById(@PathVariable String id) {
        log.info("Controller: get queue patient by id: {}", id);
        return ApiResponse.<QueuePatientsResponse>builder()
                .result(queuePatientsService.getQueuePatientsById(id))
                .build();
    }

    @GetMapping
    public ApiResponse<List<QueuePatientsResponse>> getAll() {
        log.info("Controller: get all queue patients");
        return ApiResponse.<List<QueuePatientsResponse>>builder()
                .result(queuePatientsService.getAllQueuePatients())
                .build();
    }

    @GetMapping("/polling")
    public DeferredResult<List<QueuePatientsResponse>> pollUpdates() {
        log.info("Client long-polling for queue patient updates");
        return queuePollingService.registerListener();
    }

    @GetMapping("/search")
    public ApiResponse<PagedResponse<QueuePatientCompactResponse>> searchQueuePatients(
            @RequestParam(required = false) Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "registeredTime") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        log.info("Controller: search queue patients with filters {}", filters);
        Page<QueuePatientCompactResponse> result = queuePatientsService.searchQueuePatients(filters, page, size, sortBy, sortDir);

        PagedResponse<QueuePatientCompactResponse> response = new PagedResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );

        return ApiResponse.<PagedResponse<QueuePatientCompactResponse>>builder()
                .result(response)
                .build();
    }

    @GetMapping("/detail/{id}")
    public ApiResponse<QueuePatientCompactResponse> getQueuePatientById(@PathVariable String id) {
        log.info("Controller: get queue patient by id {}", id);
        QueuePatientCompactResponse result = queuePatientsService.getQueuePatientDetail(id);
        return ApiResponse.<QueuePatientCompactResponse>builder()
                .result(result)
                .build();
    }

    @PutMapping("/status/{id}")
    public ApiResponse<QueuePatientsResponse> updateStatus(
            @PathVariable String id,
            @RequestBody @Valid UpdateQueuePatientController request
    ) {
        log.info("Controller: update status of queue patient {} to {}", id, request.getStatus());

        QueuePatientsResponse result = queuePatientsService.updateQueuePatientStatus(id, request.getStatus());

        return ApiResponse.<QueuePatientsResponse>builder()
                .result(result)
                .build();
    }

}
