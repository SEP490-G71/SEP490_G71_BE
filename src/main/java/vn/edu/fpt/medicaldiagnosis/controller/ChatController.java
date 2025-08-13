package vn.edu.fpt.medicaldiagnosis.controller;

import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.ChatRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.AiInsight;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.entity.ChatHistory;
import vn.edu.fpt.medicaldiagnosis.service.ChatbotService;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/chats")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatController {
    ChatbotService chatbotService;

    @PostMapping
    public ApiResponse<String> askQuestion(@Valid @RequestBody ChatRequest request,
                                           Authentication authentication) {

        String username = authentication.getName();
        String answer = chatbotService.askQuestion(request, username);
        return ApiResponse.<String>builder()
                .result(answer)
                .build();
    }


    @GetMapping("/history")
    public ApiResponse<List<ChatHistory>> getHistory(Authentication authentication) {
        String username = authentication.getName(); // lấy từ token
        List<ChatHistory> history = chatbotService.getHistoryByUsername(username);
        return ApiResponse.<List<ChatHistory>>builder()
                .result(history)
                .build();
    }

    @GetMapping("/eod")
    public ApiResponse<AiInsight> analyzeEod(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        LocalDate target = date;

        AiInsight insight = chatbotService.analyzeDailyRevenueEod(target);

        return ApiResponse.<AiInsight>builder()
                .result(insight)
                .build();
    }
}
