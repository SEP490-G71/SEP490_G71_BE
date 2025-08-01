package vn.edu.fpt.medicaldiagnosis.common;

import lombok.extern.slf4j.Slf4j;

import javax.sound.sampled.*;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

@Slf4j
public class AudioPlayer {

    /**
     * Phát file audio theo định dạng (MP3/WAV) tuỳ vào hệ điều hành.
     * @param filePath đường dẫn file audio.
     */
    public static void playAudio(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            log.warn("Không tìm thấy file audio: {}", filePath);
            return;
        }

        try {
            String os = System.getProperty("os.name").toLowerCase();

            // Với file WAV → dùng Clip phát trong JVM, tránh phụ thuộc hệ điều hành
            if (filePath.endsWith(".wav")) {
                playWavWithClip(file);
                return;
            }

            // Với MP3 → dùng lệnh hệ điều hành để phát
            if (os.contains("win")) {
                // Windows: sử dụng Windows Media Player hoặc ứng dụng mặc định
                Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", "\"\"", file.getAbsolutePath()});
            } else if (os.contains("mac")) {
                // macOS: sử dụng afplay
                Runtime.getRuntime().exec(new String[]{"afplay", file.getAbsolutePath()});
            } else if (os.contains("nux") || os.contains("nix")) {
                // Linux: sử dụng mpg123 (cần cài sẵn)
                Runtime.getRuntime().exec(new String[]{"mpg123", file.getAbsolutePath()});
            } else {
                // Hệ điều hành không xác định → fallback
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(file);
                }
            }

            log.info("Đã phát file audio: {}", filePath);

        } catch (Exception e) {
            log.error("Lỗi khi phát file audio: {}", e.getMessage(), e);
        }
    }

    /**
     * Phát file WAV nội bộ bằng javax.sound.sampled (hỗ trợ tốt trong JVM).
     * @param file file WAV cần phát.
     */
    private static void playWavWithClip(File file) {
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(file)) {
            Clip clip = AudioSystem.getClip();
            clip.open(stream);
            clip.start();

            // Chờ đến khi phát xong
            while (clip.isRunning()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt(); // Đảm bảo thread không bị treo nếu bị interrupt
                    break;
                }
            }

            clip.close();
            log.info("Phát WAV hoàn tất: {}", file.getName());

        } catch (UnsupportedAudioFileException | LineUnavailableException | IOException e) {
            log.error("Lỗi phát WAV: {}", e.getMessage(), e);
        }
    }
}
