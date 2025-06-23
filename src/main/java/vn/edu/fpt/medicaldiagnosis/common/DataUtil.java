package vn.edu.fpt.medicaldiagnosis.common;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import vn.edu.fpt.medicaldiagnosis.repository.AccountRepository;

import java.security.SecureRandom;
import java.text.Normalizer;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataUtil {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountRepository accountRepository;

    /**
     * Sinh mật khẩu ngẫu nhiên
     */
    public static String generateRandomPassword(int length) {
        if (length <= 0) throw new IllegalArgumentException("Password length must be greater than 0");

        StringBuilder password = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(CHARACTERS.length());
            password.append(CHARACTERS.charAt(index));
        }
        return password.toString();
    }

    public static String removeAccents(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{M}", "").replaceAll("[đĐ]", "d");
    }

    public static String normalizeForSearch(String input) {
        if (input == null) return "";
        String cleaned = removeAccents(input).toLowerCase().trim();
        return cleaned.replaceAll("\\s+", " ");
    }
}
