package vn.edu.fpt.medicaldiagnosis.common;

import javazoom.jl.player.Player;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.Map;
import java.util.concurrent.*;

/**
 * AudioPlayer đa tenant: mỗi tenant có hàng đợi và worker thread riêng để phát file mp3.
 */
@Slf4j
public class AudioPlayer {

    // Hàng đợi theo tenant
    private static final Map<String, BlockingQueue<String>> tenantQueues = new ConcurrentHashMap<>();

    // Thread worker theo tenant
    private static final Map<String, Thread> tenantWorkers = new ConcurrentHashMap<>();

    /**
     * Gửi file vào hàng đợi tương ứng với tenant để phát.
     *
     * @param tenantCode mã tenant
     * @param filePath   đường dẫn file mp3
     */
    public static void playAudio(String tenantCode, String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            log.warn("[{}] Không tìm thấy file audio: {}", tenantCode, filePath);
            return;
        }

        BlockingQueue<String> queue = tenantQueues.computeIfAbsent(tenantCode, code -> {
            BlockingQueue<String> q = new LinkedBlockingQueue<>();
            startWorkerForTenant(code, q);
            return q;
        });

        queue.offer(filePath);
        log.info("[{}] Đã thêm vào hàng đợi phát audio: {}", tenantCode, filePath);
    }

    /**
     * Khởi động worker thread để xử lý hàng đợi audio cho tenant
     */
    private static void startWorkerForTenant(String tenantCode, BlockingQueue<String> queue) {
        Thread worker = new Thread(() -> {
            log.info("[{}] Khởi động audio worker", tenantCode);
            while (true) {
                try {
                    String filePath = queue.take();
                    playFile(tenantCode, filePath);
                    Thread.sleep(2000); // delay giữa 2 file
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("[{}] Audio worker bị dừng", tenantCode);
                    break;
                } catch (Exception e) {
                    log.error("[{}] Lỗi trong worker: {}", tenantCode, e.getMessage(), e);
                }
            }
        }, "audio-worker-" + tenantCode);

        worker.setDaemon(true);
        worker.start();
        tenantWorkers.put(tenantCode, worker);
    }

    /**
     * Phát một file mp3 sử dụng JLayer
     */
    private static void playFile(String tenantCode, String filePath) {
        File file = new File(filePath);
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(file))) {
            Player player = new Player(bis);
            log.info("[{}] Bắt đầu phát: {}", tenantCode, filePath);
            player.play();
            log.info("[{}] Đã phát xong: {}", tenantCode, filePath);
        } catch (Exception e) {
            log.error("[{}] Lỗi phát file {}: {}", tenantCode, filePath, e.getMessage(), e);
        }
    }

}
