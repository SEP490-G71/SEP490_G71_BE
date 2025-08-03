package vn.edu.fpt.medicaldiagnosis.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.common.AudioPlayer;
import vn.edu.fpt.medicaldiagnosis.context.TenantContext;
import vn.edu.fpt.medicaldiagnosis.service.TextToSpeechService;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;

@Slf4j
@Service
public class TextToSpeechServiceImpl implements TextToSpeechService {

    @Value("${viettelai.tts.url}")
    private String VIETTEL_TTS_URL;

    @Value("${viettelai.tts.token}")
    private String VIETTEL_TTS_TOKEN;

    @Value("${viettelai.tts.voice}")
    private String VIETTEL_TTS_VOICE;

    @Value("${viettelai.tts.speed}")
    private float VIETTEL_TTS_SPEED;

    @Value("${viettelai.tts.return_option}")
    private int VIETTEL_TTS_RETURN_OPTION;

    @Value("${viettelai.tts.filter}")
    private boolean VIETTEL_TTS_FILTER;

    @Override
    public void speak(String message) {
        try {
            URL url = new URL(VIETTEL_TTS_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            String payload = """
                    {
                        "text": "%s",
                        "voice": "%s",
                        "speed": %s,
                        "tts_return_option": %d,
                        "token": "%s",
                        "without_filter": %s
                    }
                    """.formatted(
                    message.replace("\"", "\\\""),
                    VIETTEL_TTS_VOICE,
                    VIETTEL_TTS_SPEED,
                    VIETTEL_TTS_RETURN_OPTION,
                    VIETTEL_TTS_TOKEN,
                    VIETTEL_TTS_FILTER
            );

            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes());
                os.flush();
            }

            String tenantCode = TenantContext.getTenantId();
            File tenantDir = new File("tts/" + tenantCode);
            if (!tenantDir.exists()) Files.createDirectories(tenantDir.toPath());

            String filePath = tenantDir.getPath() + "/tts_" + System.currentTimeMillis() + ".mp3";
            try (InputStream in = conn.getInputStream();
                 FileOutputStream out = new FileOutputStream(filePath)) {

                byte[] buffer = new byte[4096];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            }

            log.info("Đã tạo file TTS cho {}: {}", tenantCode, filePath);
            AudioPlayer.playAudio(tenantCode, filePath);

        } catch (IOException e) {
            log.error("Lỗi khi gọi Viettel TTS: {}", e.getMessage(), e);
        }
    }
}
