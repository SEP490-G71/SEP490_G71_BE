package vn.edu.fpt.medicaldiagnosis.service.impl;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.service.EmailService;

import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    private static final String WELCOME_TEMPLATE_PATH = "templates/welcome-email.html";

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String sender;

    /**
     * Gửi email với nội dung HTML đã được render sẵn
     */
    @Override
    public String sendSimpleMail(String recipient, String subject, String htmlContent) {
        if (recipient == null || subject == null || htmlContent == null) {
            log.error("Thiếu thông tin khi gửi email");
            return "Missing email details";
        }

        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(new InternetAddress(sender, "Phần mềm quản lý bệnh viện - Medsoft"));
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            javaMailSender.send(mimeMessage);

            log.info("Email đã gửi tới: {}", recipient);
            return "Mail Sent Successfully...";
        } catch (Exception e) {
            log.error("Lỗi khi gửi mail đến {}: {}", recipient, e.getMessage(), e);
            return "Error while Sending Mail";
        }
    }

    /**
     * Gửi email chào mừng sử dụng template HTML có sẵn
     */
    public String sendWelcomeMail(String recipient, String subject, String name, String url) {
        try {
            String htmlContent = renderWelcomeTemplate(name, url);
            return sendSimpleMail(recipient, subject, htmlContent);
        } catch (Exception e) {
            log.error("Không thể tạo nội dung email từ template: {}", e.getMessage(), e);
            return "Failed to render welcome email";
        }
    }

    /**
     * Đọc và render nội dung template chào mừng
     */
    private String renderWelcomeTemplate(String name, String url) throws Exception {
        ClassPathResource resource = new ClassPathResource(WELCOME_TEMPLATE_PATH);
        byte[] bytes = resource.getInputStream().readAllBytes();
        String template = new String(bytes, StandardCharsets.UTF_8);

        return template.replace("{{name}}", name)
                .replace("{{url}}", url);
    }
}
