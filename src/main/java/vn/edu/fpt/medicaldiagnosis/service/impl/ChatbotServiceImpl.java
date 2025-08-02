package vn.edu.fpt.medicaldiagnosis.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.dto.request.ChatRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.DashboardOverviewResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Account;
import vn.edu.fpt.medicaldiagnosis.entity.ChatHistory;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.repository.AccountRepository;
import vn.edu.fpt.medicaldiagnosis.repository.ChatHistoryRepository;
import vn.edu.fpt.medicaldiagnosis.service.ChatbotService;
import vn.edu.fpt.medicaldiagnosis.service.DashboardService;

import java.time.LocalDateTime;
import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class ChatbotServiceImpl implements ChatbotService {
    ChatHistoryRepository chatHistoryRepository;
    ChatClient customChatClient;
    AccountRepository accountRepository;
    DashboardService dashboardService;
    ObjectMapper objectMapper;
    @Override
    public String askQuestion(ChatRequest request) {
        // Lấy Account từ userId
        Account account = accountRepository.findByIdAndDeletedAtIsNull(request.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND,
                        "Không tìm thấy tài khoản với ID: " + request.getUserId()));

        DashboardOverviewResponse dashboardOverviewResponse = dashboardService.getDashboardOverview();
        String jsonDashboard;
        try {
            jsonDashboard = objectMapper.writeValueAsString(dashboardOverviewResponse);
        } catch (Exception e) {
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Lỗi khi chuyển dashboard thành JSON.");
        }
        String message = """
Dưới đây là dữ liệu thống kê bệnh viện ở dạng JSON:

%s

Dựa vào dữ liệu trên, hãy trả lời câu hỏi sau bằng tiếng Việt ngắn gọn, dễ hiểu:

%s
""".formatted(jsonDashboard, request.getQuestion());

        String answer = customChatClient
                .prompt()
                .system("Bạn là trợ lý AI phân tích thống kê bệnh viện, luôn trả lời chính xác và ngắn gọn bằng tiếng Việt.")
                .user(message)
                .call()
                .content();

        chatHistoryRepository.save(ChatHistory.builder()
                .account(account)
                .question(request.getQuestion())
                .answer(answer)
                .createdAt(LocalDateTime.now())
                .build());

        return answer;
    }

    @Override
    public List<ChatHistory> getHistory(String userId) {
        Account account = accountRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND, "Tài khoản không tồn tại hoặc đã bị xóa"));

        return chatHistoryRepository.findByAccountOrderByCreatedAtDesc(account);
    }

}
