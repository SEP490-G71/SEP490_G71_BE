package vn.edu.fpt.medicaldiagnosis.schedule;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.fpt.medicaldiagnosis.entity.CloudflareTask;
import vn.edu.fpt.medicaldiagnosis.enums.Status;
import vn.edu.fpt.medicaldiagnosis.repository.CloudflareTaskRepository;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

@Slf4j
@Component
public class CloudflareTaskJob {

    private static final int MAX_RETRIES = 3;

    @Autowired
    private CloudflareTaskRepository taskRepository;

    @Value("${cloudflare.zone-id}") private String zoneId;
    @Value("${cloudflare.api-token}") private String apiToken;
    @Value("${cloudflare.ip-address}") private String ipAddress;

    @Scheduled(fixedDelay = 15000)
    public void processCloudflareTasks() {
        List<CloudflareTask> tasks = taskRepository.findTop10ByStatusOrderByCreatedAtAsc(Status.PENDING);

        for (CloudflareTask task : tasks) {
            try {
                String subdomain = task.getSubdomain();
                URL url = new URL("https://api.cloudflare.com/client/v4/zones/" + zoneId + "/dns_records");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("Authorization", "Bearer " + apiToken);
                con.setRequestProperty("Content-Type", "application/json");
                con.setDoOutput(true);

                String body = String.format("""
                        {
                          "type": "A",
                          "name": "%s",
                          "content": "%s",
                          "ttl": 1,
                          "proxied": true
                        }
                        """, subdomain, ipAddress);

                try (OutputStream os = con.getOutputStream()) {
                    os.write(body.getBytes("utf-8"));
                }

                int responseCode = con.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    task.setStatus(Status.DONE);
                    log.info("Subdomain tạo thành công: {}", subdomain);
                } else {
                    handleFailure(task, "Cloudflare trả về mã lỗi: " + responseCode);
                }
            } catch (Exception e) {
                handleFailure(task, e.getMessage());
            }

            taskRepository.save(task);
        }
    }

    private void handleFailure(CloudflareTask task, String message) {
        int retry = task.getRetryCount() + 1;
        task.setRetryCount(retry);

        if (retry >= MAX_RETRIES) {
            task.setStatus(Status.FAILED);
            log.error("Tạo subdomain thất bại vĩnh viễn {} sau {} lần thử: {}", task.getSubdomain(), retry, message);
        } else {
            task.setStatus(Status.PENDING);
            log.warn("Lỗi tạo subdomain lần {} cho {}: {}. Sẽ thử lại.", retry, task.getSubdomain(), message);
        }
    }
}
