package vn.edu.fpt.medicaldiagnosis.schedule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Stream;

@Slf4j
@Component
public class TTSFileCleaner {

    private static final String TTS_DIR = "tts";
    private static final long ONE_HOUR_IN_MILLIS = 60 * 60 * 1000;

    @Scheduled(fixedRate = 10 * 60 * 1000) // mỗi 10 phút
    public void cleanOldFiles() {
        long now = System.currentTimeMillis();
        Path rootPath = Paths.get(TTS_DIR);

        if (!Files.exists(rootPath)) return;

        try (Stream<Path> paths = Files.walk(rootPath)) {
            paths.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".mp3"))
                    .filter(path -> {
                        try {
                            return now - Files.getLastModifiedTime(path).toMillis() > ONE_HOUR_IN_MILLIS;
                        } catch (IOException e) {
                            log.warn("Không thể đọc thời gian sửa đổi file: {}", path);
                            return false;
                        }
                    })
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            log.info("Đã xoá file cũ: {}", path);
                        } catch (IOException e) {
                            log.error("Không thể xoá file: {}", path, e);
                        }
                    });
        } catch (IOException e) {
            log.error("Lỗi khi dọn file tts/", e);
        }
    }
}
