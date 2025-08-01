package vn.edu.fpt.medicaldiagnosis.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.common.AudioPlayer;
import vn.edu.fpt.medicaldiagnosis.service.TextToSpeechService;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;

@Slf4j
@Service
public class TextToSpeechServiceImpl implements TextToSpeechService {

    @Value("${openai.api-key}")
    private String OPENAI_API_KEY;

    @Value("${openai.speech-url}")
    private String OPENAI_SPEECH_URL;

    @Override
    public void speak(String message) {
        try {
            // 1. Gửi request tới OpenAI để tạo file audio
            URL url = new URL(OPENAI_SPEECH_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + OPENAI_API_KEY);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String payload = """
            {
              "model": "tts-1",
              "voice": "nova",
              "input": "%s"
            }
            """.formatted(message.replace("\"", "\\\""));

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes());
                os.flush();
            }

            // 2. Nhận file MP3 từ OpenAI và lưu vào thư mục
            File dir = new File("tts");
            if (!dir.exists()) Files.createDirectories(dir.toPath());

            String filePath = "tts/tts_" + System.currentTimeMillis() + ".mp3";
            try (InputStream in = conn.getInputStream();
                 FileOutputStream out = new FileOutputStream(filePath)) {

                byte[] buffer = new byte[4096];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            }

            log.info("Đã tạo file TTS: {}", filePath);

            // 3. Gọi AudioPlayer để phát ra loa
            AudioPlayer.playAudio(filePath);

        } catch (IOException e) {
            log.error("Lỗi khi gọi OpenAI TTS: {}", e.getMessage(), e);
        }
    }
}
