package vn.edu.fpt.medicaldiagnosis.schedule;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.fpt.medicaldiagnosis.context.TenantContext;
import vn.edu.fpt.medicaldiagnosis.entity.EmailTask;
import vn.edu.fpt.medicaldiagnosis.entity.Patient;
import vn.edu.fpt.medicaldiagnosis.entity.Tenant;
import vn.edu.fpt.medicaldiagnosis.enums.Status;
import vn.edu.fpt.medicaldiagnosis.repository.EmailTaskRepository;
import vn.edu.fpt.medicaldiagnosis.repository.PatientRepository;
import vn.edu.fpt.medicaldiagnosis.service.TenantService;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static lombok.AccessLevel.PRIVATE;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class BirthdayEmailJob {
    PatientRepository patientRepository;
    EmailTaskRepository emailTaskRepository;
    TenantService tenantService;
    // Chạy lúc 00:00 ngày 1 hàng tháng
    @Scheduled(cron = "0 0 0 1,10,20 * *")
    public void sendBirthdayEmails() {
        int month = LocalDate.now().getMonthValue();
        log.info("🎉 BirthdayEmailJob started for month {}", month);

        List<Tenant> tenants = tenantService.getAllTenantsActive();

        for (Tenant tenant : tenants) {
            try {
                TenantContext.setTenantId(tenant.getCode());
                log.info("[{}] Đang xử lý email sinh nhật cho tháng {}", tenant.getCode(), month);

                List<Patient> birthdayPatients = patientRepository.findAll((root, query, cb) ->
                        cb.equal(cb.function("MONTH", Integer.class, root.get("dob")), month)
                );

                if (birthdayPatients.isEmpty()) {
                    log.info("[{}] Không có bệnh nhân nào sinh nhật trong tháng {}", tenant.getCode(), month);
                    continue;
                }

                List<EmailTask> tasks = new ArrayList<>();
                for (Patient patient : birthdayPatients) {
                    if (patient.getEmail() == null || patient.getEmail().isBlank()) continue;

                    String url = "https://" + tenant.getCode() + ".datnd.id.vn/home";
                    String content;

                    try {
                        ClassPathResource resource = new ClassPathResource("templates/birthday-email.html");
                        String template = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                        content = template
                                .replace("{{name}}", patient.getFullName())
                                .replace("{{url}}", url);
                    } catch (Exception e) {
                        log.warn("[{}] Không thể load template birthday-email.html: {}", tenant.getCode(), e.getMessage());
                        content = String.format("Xin chào %s,\n\nTruy cập hệ thống tại: %s\n\nTrân trọng.", patient.getFullName(), url);
                    }

                    String subject = "🎂 Chúc mừng sinh nhật, " + patient.getFullName() + "!";

                    EmailTask task = EmailTask.builder()
                            .id(UUID.randomUUID().toString())
                            .emailTo(patient.getEmail())
                            .subject(subject)
                            .content(content)
                            .retryCount(0)
                            .status(Status.PENDING)
                            .build();

                    tasks.add(task);
                }

                emailTaskRepository.saveAll(tasks);
                log.info("[{}] Đã tạo {} email sinh nhật cho tháng {}", tenant.getCode(), tasks.size(), month);

            } catch (Exception e) {
                log.error("[{}] Lỗi khi gửi email sinh nhật: {}", tenant.getCode(), e.getMessage(), e);
            } finally {
                TenantContext.clear(); // luôn clear sau mỗi tenant
            }
        }
    }

}
