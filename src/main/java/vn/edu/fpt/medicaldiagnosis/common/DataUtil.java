package vn.edu.fpt.medicaldiagnosis.common;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import vn.edu.fpt.medicaldiagnosis.repository.AccountRepository;

import java.math.BigDecimal;
import java.security.SecureRandom;

import java.text.Normalizer;
import java.util.List;

@Slf4j
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

    public static Integer parseInt(String input) {
        try {
            return Integer.parseInt(input.trim());
        } catch (Exception e) {
            log.warn("Không thể parseInt từ '{}': {}", input, e.getMessage());
            return null;
        }
    }

    public static Long parseLong(String input) {
        try {
            return Long.parseLong(input.trim());
        } catch (Exception e) {
            log.warn("Không thể parseLong từ '{}': {}", input, e.getMessage());
            return null;
        }
    }

    public static Double parseDouble(String input) {
        try {
            return Double.parseDouble(input.trim());
        } catch (Exception e) {
            log.warn("Không thể parseDouble từ '{}': {}", input, e.getMessage());
            return null;
        }
    }

    public static BigDecimal parseBigDecimal(String input) {
        try {
            return new BigDecimal(input.trim());
        } catch (Exception e) {
            log.warn("Không thể parseBigDecimal từ '{}': {}", input, e.getMessage());
            return null;
        }
    }
  
  public static Boolean parseBoolean(String input) {
        if (input == null) {
            log.warn("parseBoolean nhận null");
            return null;
        }
        String trimmed = input.trim().toLowerCase();
        if (trimmed.equals("true") || trimmed.equals("1")) return true;
        if (trimmed.equals("false") || trimmed.equals("0")) return false;

        log.warn("Không thể parseBoolean từ '{}'", input);
        return null;
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

    public static String getStatusVietnamese(String status) {
        return switch (status) {
            case "PAID" -> "Đã thanh toán";
            case "UNPAID" -> "Chưa thanh toán";
            case "CANCELED" -> "Đã hủy";
            default -> "Không xác định";
        };
    }

    public static String getPaymentTypeVietnamese(String paymentType) {
        return switch (paymentType) {
            case "CASH" -> "Tiền mặt";
            case "CARD" -> "Thẻ";
            case "INSURANCE" -> "Bảo hiểm";
            default -> "Không xác định";
        };
    }

    public static String safeString(String input) {
        return input != null ? input : "";
    }

}
