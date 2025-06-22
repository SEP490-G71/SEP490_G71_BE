package vn.edu.fpt.medicaldiagnosis.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import vn.edu.fpt.medicaldiagnosis.dto.request.QueuePatientsRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientsResponse;
import vn.edu.fpt.medicaldiagnosis.service.QueuePatientsService;
import vn.edu.fpt.medicaldiagnosis.service.QueuePollingService;

import java.util.List;

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
}
