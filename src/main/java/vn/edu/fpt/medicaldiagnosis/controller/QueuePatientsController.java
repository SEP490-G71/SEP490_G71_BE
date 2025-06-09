package vn.edu.fpt.medicaldiagnosis.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.QueuePatientsRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientsResponse;
import vn.edu.fpt.medicaldiagnosis.service.QueuePatientsService;

import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/queue-patients")
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class QueuePatientsController {

    QueuePatientsService queuePatientsService;

    @PostMapping
    public ApiResponse<QueuePatientsResponse> create(@RequestBody @Valid QueuePatientsRequest request) {
        log.info("Controller: create queue patient {}", request);
        return ApiResponse.<QueuePatientsResponse>builder()
                .result(queuePatientsService.create(request))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<QueuePatientsResponse> update(@PathVariable String id, @RequestBody @Valid QueuePatientsRequest request) {
        log.info("Controller: update queue patient {}", id);
        return ApiResponse.<QueuePatientsResponse>builder()
                .result(queuePatientsService.update(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> delete(@PathVariable String id) {
        log.info("Controller: delete queue patient {}", id);
        queuePatientsService.delete(id);
        return ApiResponse.<String>builder()
                .message("QueuePatient deleted successfully.")
                .build();
    }

    @GetMapping("/{id}")
    public ApiResponse<QueuePatientsResponse> getById(@PathVariable String id) {
        log.info("Controller: get queue patient by id: {}", id);
        return ApiResponse.<QueuePatientsResponse>builder()
                .result(queuePatientsService.getById(id))
                .build();
    }

    @GetMapping
    public ApiResponse<List<QueuePatientsResponse>> getAll() {
        log.info("Controller: get all queue patients");
        return ApiResponse.<List<QueuePatientsResponse>>builder()
                .result(queuePatientsService.getAll())
                .build();
    }
}
