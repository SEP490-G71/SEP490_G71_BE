package vn.edu.fpt.medicaldiagnosis;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootApplication
@EnableScheduling
@EnableCaching
@EnableAsync
public class MedicalDiagnosisApplication {
    public static void main(String[] args) {
//        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
//        System.out.println(passwordEncoder.encode("manager"));

        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        dotenv.entries().forEach(entry ->
                System.setProperty(entry.getKey(), entry.getValue())
        );

        SpringApplication.run(MedicalDiagnosisApplication.class, args);
    }
}
