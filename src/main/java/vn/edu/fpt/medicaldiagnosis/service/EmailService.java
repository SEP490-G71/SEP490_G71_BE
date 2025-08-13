package vn.edu.fpt.medicaldiagnosis.service;

import vn.edu.fpt.medicaldiagnosis.entity.EmailDetails;

// Interface
public interface EmailService {

    // Method
    // To send a simple email
    String sendSimpleMail(String recipient, String subject, String htmlContent);
    String sendAccountMail(String recipient, String name, String username, String password, String url);

    String sendRoomAssignmentMail(String recipient, String name, int room, long order);

}
