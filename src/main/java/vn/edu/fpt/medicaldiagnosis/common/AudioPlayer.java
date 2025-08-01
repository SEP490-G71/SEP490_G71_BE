package vn.edu.fpt.medicaldiagnosis.common;

import javazoom.jl.player.Player;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Trình phát audio dùng hàng đợi để phát từng file mp3 theo thứ tự.
 * Không phát đồng thời, đảm bảo không bị chồng tiếng khi nhiều phòng gọi TTS cùng lúc.
 */
@Slf4j
public class AudioPlayer {

    // Hàng đợi chứa đường dẫn các file mp3 cần phát
    private static final BlockingQueue<String> playQueue = new LinkedBlockingQueue<>();

    // Thread chạy ngầm để phát từng file mp3 theo thứ tự FIFO
    private static final Thread workerThread;

    // Khởi tạo worker khi class được load
    static {
        workerThread = new Thread(() -> {
            while (true) {
                try {
                    // Lấy file từ hàng đợi
                    String filePath = playQueue.take();

                    // Phát file audio (blocking)
                    playFile(filePath);

                    // Ngắt quãng 2 giây giữa 2 file
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("AudioPlayer worker bị dừng.");
                    break;
                } catch (Exception e) {
                    log.error("Lỗi khi xử lý file trong AudioPlayer worker: {}", e.getMessage(), e);
                }
            }
        }, "audio-play-worker");

        // Cho phép thread tự động dừng khi JVM kết thúc
        workerThread.setDaemon(true);
        workerThread.start();
    }

    /**
     * Gửi file audio vào hàng đợi để phát sau.
     * Nếu nhiều phòng gọi cùng lúc, các file sẽ được xử lý tuần tự.
     *
     * @param filePath đường dẫn file mp3
     */
    public static void playAudio(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            log.warn("Không tìm thấy file audio: {}", filePath);
            return;
        }

        playQueue.offer(filePath);
        log.info("Đã thêm vào hàng đợi phát audio: {}", filePath);
    }

    /**
     * Phát 1 file mp3 bằng thư viện JLayer (không phụ thuộc hệ điều hành).
     * Hàm này sẽ bị blocking trong quá trình phát, nhưng được chạy riêng trong thread worker.
     *
     * @param filePath đường dẫn file mp3 cần phát
     */
    private static void playFile(String filePath) {
        File file = new File(filePath);
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            Player player = new Player(bis);
            log.info("Bắt đầu phát file audio: {}", filePath);
            player.play(); // Blocking nhưng không ảnh hưởng vì đang chạy trong thread riêng
            log.info("Hoàn tất phát file audio: {}", filePath);
        } catch (Exception e) {
            log.error("Lỗi khi phát file audio: {}", filePath, e);
        }
    }
}
