package vn.edu.fpt.medicaldiagnosis.service;

// Interface
public interface EmailService {

    String sendSimpleMail(String recipient, String subject, String htmlContent);
}
