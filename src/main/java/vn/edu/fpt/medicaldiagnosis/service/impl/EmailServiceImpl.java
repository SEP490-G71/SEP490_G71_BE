package vn.edu.fpt.medicaldiagnosis.service.impl;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.entity.EmailDetails;
import vn.edu.fpt.medicaldiagnosis.service.EmailService;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

// Annotation
@Service
@Slf4j
// Class
// Implementing EmailService interface
public class EmailServiceImpl implements EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}") private String sender;

    private String loadTemplate(String path) throws Exception {
        ClassPathResource resource = new ClassPathResource(path);
        byte[] bytes = resource.getInputStream().readAllBytes();
        return new String(bytes, StandardCharsets.UTF_8);
    }
    // Method 1
    // To send a simple email
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

    public String sendAccountMail(String recipient, String name, String username, String password, String url) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(new InternetAddress(sender, "Phần mềm quản lý bệnh viện - Medsoft"));
            helper.setTo(recipient);
            helper.setSubject("Thông tin tài khoản của bạn trên hệ thống Medsoft");

            // Đọc nội dung template từ file
            String template = loadTemplate("templates/account-email.html");

            // Thay thế các placeholder trong template bằng thông tin thực tế
            String htmlMsg = template
                    .replace("{{name}}", name)
                    .replace("{{username}}", username)
                    .replace("{{password}}", password)
                    .replace("{{url}}", url);

            helper.setText(htmlMsg, true); // true để gửi HTML

            javaMailSender.send(mimeMessage);

            log.info("Email đã gửi tới: {}", recipient);
            return "Mail Sent Successfully...";
        } catch (Exception e) {
            log.error("Lỗi khi gửi mail: ", e);
            return "Error while Sending Mail";
        }
    }

    public String sendRoomAssignmentMail(String recipient, String name, int room, long order) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(new InternetAddress(sender, "Phần mềm quản lý bệnh viện - Medsoft"));
            helper.setTo(recipient);
            helper.setSubject("Thông báo phân phòng khám");

            // Load template và thay thế placeholder
            String template = loadTemplate("templates/room-assignment-email.html");
            String htmlMsg = template
                    .replace("{{name}}", name)
                    .replace("{{room}}", String.valueOf(room))
                    .replace("{{order}}", String.valueOf(order));

            helper.setText(htmlMsg, true);
            javaMailSender.send(mimeMessage);

            log.info("Email đã gửi tới: {}", recipient);
            return "Mail Sent Successfully...";
        } catch (Exception e) {
            log.error("Lỗi khi gửi mail phân phòng: ", e);
            return "Error while Sending Mail";
        }
    }

}
