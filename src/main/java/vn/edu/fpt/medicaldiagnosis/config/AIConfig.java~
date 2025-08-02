package vn.edu.fpt.medicaldiagnosis.common;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {
    @Bean
    public ChatClient customChatClient(ChatClient.Builder chatBuilder) {
        return chatBuilder.build(); // Không có system prompt mặc định
    }
}
