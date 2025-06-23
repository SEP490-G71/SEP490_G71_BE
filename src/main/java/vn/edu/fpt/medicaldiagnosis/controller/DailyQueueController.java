package vn.edu.fpt.medicaldiagnosis.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.DailyQueueRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.DailyQueueResponse;
import vn.edu.fpt.medicaldiagnosis.service.DailyQueueService;

import java.util.List;

@RestController
@RequestMapping("/daily-queues")
@RequiredArgsConstructor
public class DailyQueueController {

    private final DailyQueueService service;

    @PostMapping
    public ApiResponse<DailyQueueResponse> createDailyQueue(@RequestBody @Valid DailyQueueRequest request) {
        return ApiResponse.<DailyQueueResponse>builder()
                .result(service.createDailyQueue(request))
                .build();
    }

    @PutMapping("/{id}")
    public ApiResponse<DailyQueueResponse> updateDailyQueue(@PathVariable String id,
                                                  @RequestBody @Valid DailyQueueRequest request) {
        return ApiResponse.<DailyQueueResponse>builder()
                .result(service.updateDailyQueue(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        service.deleteDailyQueue(id);
        return ApiResponse.<Void>builder().build();
    }

    @GetMapping("/{id}")
    public ApiResponse<DailyQueueResponse> getDailyQueueById(@PathVariable String id) {
        return ApiResponse.<DailyQueueResponse>builder()
                .result(service.getDailyQueueById(id))
                .build();
    }

    @GetMapping
    public ApiResponse<List<DailyQueueResponse>> getAllDailyQueues() {
        return ApiResponse.<List<DailyQueueResponse>>builder()
                .result(service.getAllDailyQueues())
                .build();
    }
}
