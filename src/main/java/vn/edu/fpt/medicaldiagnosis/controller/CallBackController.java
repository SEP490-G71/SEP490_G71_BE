package vn.edu.fpt.medicaldiagnosis.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;

import java.util.Map;

@RestController
@RequestMapping("/callback")
public class CallBackController {

    @PostMapping()
    public ApiResponse<?> handleCallback(@RequestBody Map<String, Object> payload) {
        return ApiResponse.builder()
                .result(payload)
                .build();
    }

}
