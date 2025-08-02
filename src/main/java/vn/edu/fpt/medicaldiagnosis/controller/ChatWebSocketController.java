package vn.edu.fpt.medicaldiagnosis.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import vn.edu.fpt.medicaldiagnosis.dto.request.ChatRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ChatMessage;
import vn.edu.fpt.medicaldiagnosis.service.ChatbotService;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatbotService chatbotService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send") // Khi client gửi message tới /app/chat.send
    public void sendMessage(ChatMessage message) {
        String answer = chatbotService.askQuestion(
                new ChatRequest(message.getUserId(), message.getQuestion())
        );

        // Gán câu trả lời vào tin nhắn
        message.setAnswer(answer);

        // Gửi về cho client theo topic riêng
        messagingTemplate.convertAndSend("/topic/reply/" + message.getUserId(), message);
    }
}

