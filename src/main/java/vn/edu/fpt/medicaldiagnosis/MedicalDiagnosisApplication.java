package vn.edu.fpt.medicaldiagnosis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MedicalDiagnosisApplication {
    public static void main(String[] args) {
        SpringApplication.run(MedicalDiagnosisApplication.class, args);
    }
}
