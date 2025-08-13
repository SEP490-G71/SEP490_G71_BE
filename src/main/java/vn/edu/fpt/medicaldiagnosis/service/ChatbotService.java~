package vn.edu.fpt.medicaldiagnosis.service;

import vn.edu.fpt.medicaldiagnosis.dto.request.ChatRequest;
import vn.edu.fpt.medicaldiagnosis.entity.ChatHistory;

import java.util.List;

public interface ChatbotService {
    String askQuestion(ChatRequest request, String userName);

    List<ChatHistory> getHistoryByUsername(String username);
}
