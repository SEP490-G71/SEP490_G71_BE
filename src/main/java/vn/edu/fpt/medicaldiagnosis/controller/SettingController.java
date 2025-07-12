package vn.edu.fpt.medicaldiagnosis.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.SettingRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.SettingResponse;
import vn.edu.fpt.medicaldiagnosis.service.SettingService;

@RestController
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingController {

    private final SettingService settingService;

    @GetMapping
    public ApiResponse<SettingResponse> getSetting() {
        return ApiResponse.<SettingResponse>builder()
                .result(settingService.getSetting())
                .build();
    }

    @PostMapping // hoặc @PutMapping nếu cần
    public ApiResponse<SettingResponse> upsertSetting(@RequestBody @Valid SettingRequest request) {
        return ApiResponse.<SettingResponse>builder()
                .result(settingService.upsertSetting(request))
                .build();
    }
}
