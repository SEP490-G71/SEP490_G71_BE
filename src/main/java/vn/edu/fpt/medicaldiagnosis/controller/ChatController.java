package vn.edu.fpt.medicaldiagnosis.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.ChatRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.InvoiceDetailResponse;
import vn.edu.fpt.medicaldiagnosis.entity.ChatHistory;
import vn.edu.fpt.medicaldiagnosis.service.ChatbotService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chats")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatController {
    ChatbotService chatbotService;

    @PostMapping()
    public ApiResponse<String> askQuestion(@Valid @RequestBody ChatRequest request) {
        String answer = chatbotService.askQuestion(request);
        return ApiResponse.<String>builder()
                .result(answer)
                .build();
    }


    @GetMapping("/history")
    public ApiResponse<List<ChatHistory>> getHistory(@RequestParam String userId) {
        List<ChatHistory> history = chatbotService.getHistory(userId);
        return ApiResponse.<List<ChatHistory>>builder()
                .result(history)
                .build();
    }
}
