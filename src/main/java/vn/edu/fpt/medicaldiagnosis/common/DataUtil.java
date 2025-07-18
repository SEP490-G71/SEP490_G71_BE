package vn.edu.fpt.medicaldiagnosis.common;

import com.spire.doc.*;
import com.spire.doc.documents.BreakType;
import com.spire.doc.documents.Paragraph;
import com.spire.doc.fields.DocPicture;
import com.spire.doc.fields.TextRange;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Component;
import vn.edu.fpt.medicaldiagnosis.repository.AccountRepository;

import java.math.BigDecimal;
import java.net.URL;
import java.security.SecureRandom;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
            input = input.trim();
            return Integer.parseInt(input);
        } catch (Exception e) {
            log.warn("Không thể parseInt từ '{}': {}", input, e.getMessage());
            return null;
        }
    }

    public static Long parseLong(String input) {
        try {
            input = input.trim();
            return Long.parseLong(input);
        } catch (Exception e) {
            log.warn("Không thể parseLong từ '{}': {}", input, e.getMessage());
            return null;
        }
    }

    public static Double parseDouble(String input) {
        try {
            input = input.trim();
            return Double.parseDouble(input);
        } catch (Exception e) {
            log.warn("Không thể parseDouble từ '{}': {}", input, e.getMessage());
            return null;
        }
    }

    public static BigDecimal parseBigDecimal(String input) {
        try {
            input = input.trim();
            return new BigDecimal(input);
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

    public static void replaceParagraphPlaceholders(Document doc, Map<String, Object> data) {
        for (int s = 0; s < doc.getSections().getCount(); s++) {
            Section section = doc.getSections().get(s);

            // 1. Replace in body paragraphs
            for (Object obj : section.getBody().getChildObjects()) {
                if (obj instanceof Paragraph paragraph) {
                    replaceInParagraph(paragraph, data);
                }

                // 2. Replace in all tables (header or footer data)
                if (obj instanceof Table table) {
                    for (int r = 0; r < table.getRows().getCount(); r++) {
                        TableRow row = table.getRows().get(r);
                        for (int c = 0; c < row.getCells().getCount(); c++) {
                            TableCell cell = row.getCells().get(c);
                            for (Object p : cell.getParagraphs()) {
                                if (p instanceof Paragraph paragraph) {
                                    replaceInParagraph(paragraph, data);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public static void replaceInParagraph(Paragraph paragraph, Map<String, Object> data) {
        StringBuilder fullText = new StringBuilder();
        List<TextRange> ranges = new ArrayList<>();

        for (int i = 0; i < paragraph.getChildObjects().getCount(); i++) {
            Object child = paragraph.getChildObjects().get(i);
            if (child instanceof TextRange textRange) {
                // Áp font bắt buộc cho toàn bộ text
                textRange.getCharacterFormat().setFontName("DejaVu Sans");
                textRange.getCharacterFormat().setFontSize(12f);

                fullText.append(textRange.getText());
                ranges.add(textRange);
            }
        }

        String combined = fullText.toString();
        String replaced = combined;

        for (String key : data.keySet()) {
            replaced = replaced.replace("{" + key + "}", data.get(key) != null ? data.get(key).toString() : "");
        }

        if (!combined.equals(replaced)) {
            paragraph.getChildObjects().clear();
            TextRange newRun = paragraph.appendText(replaced);
            newRun.getCharacterFormat().setFontName("DejaVu Sans");
            newRun.getCharacterFormat().setFontSize(12f);
        }
    }



    public static void replaceRowPlaceholders(TableRow row, Map<String, Object> data) {
        for (int c = 0; c < row.getCells().getCount(); c++) {
            TableCell cell = row.getCells().get(c);
            for (Object obj : cell.getParagraphs()) {
                if (obj instanceof Paragraph paragraph) {
                    for (int i = 0; i < paragraph.getChildObjects().getCount(); i++) {
                        Object child = paragraph.getChildObjects().get(i);
                        if (child instanceof TextRange textRange) {
                            String originalText = textRange.getText();
                            textRange.getCharacterFormat().setFontName("DejaVu Sans");
                            textRange.getCharacterFormat().setFontSize(12f);
                            for (String key : data.keySet()) {
                                String placeholder = "{" + key + "}";
                                if (originalText.contains(placeholder)) {
                                    String value = data.get(key) != null ? data.get(key).toString() : "";
                                    originalText = originalText.replace(placeholder, value);
                                }
                            }
                            textRange.setText(originalText);
                        }
                    }
                }
            }
        }
    }

    public static String getCellText(TableCell cell) {
        StringBuilder sb = new StringBuilder();
        for (Object obj : cell.getParagraphs()) {
            if (obj instanceof Paragraph paragraph) {
                for (int i = 0; i < paragraph.getChildObjects().getCount(); i++) {
                    Object child = paragraph.getChildObjects().get(i);
                    if (child instanceof TextRange textRange) {
                        sb.append(textRange.getText());
                    }
                }
            }
        }
        return sb.toString();
    }

    public static void replaceImagePlaceholder(Document doc, String placeholderKey, List<String> imageUrls) {
        for (int s = 0; s < doc.getSections().getCount(); s++) {
            Section section = doc.getSections().get(s);

            // 1. Duyệt các Paragraph ngoài body (giữ nguyên luồng cũ)
            for (Object bodyObj : section.getBody().getChildObjects()) {
                if (bodyObj instanceof Paragraph paragraph) {
                    insertImageIfMatch(paragraph, placeholderKey, imageUrls);
                }

                // 2. Mở rộng: Duyệt trong bảng (Table) để thay thế trong ô cell
                if (bodyObj instanceof Table table) {
                    for (int r = 0; r < table.getRows().getCount(); r++) {
                        TableRow row = table.getRows().get(r);
                        for (int c = 0; c < row.getCells().getCount(); c++) {
                            TableCell cell = row.getCells().get(c);
                            for (Object p : cell.getParagraphs()) {
                                if (p instanceof Paragraph paragraph) {
                                    insertImageIfMatch(paragraph, placeholderKey, imageUrls);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private static void insertImageIfMatch(Paragraph paragraph, String placeholderKey, List<String> imageUrls) {
        for (int j = 0; j < paragraph.getChildObjects().getCount(); j++) {
            Object child = paragraph.getChildObjects().get(j);
            if (child instanceof TextRange textRange) {
                if (textRange.getText().contains("{" + placeholderKey + "}")) {
                    paragraph.getChildObjects().clear(); // remove placeholder
                    for (String imageUrl : imageUrls) {
                        try {
                            byte[] imageBytes = IOUtils.toByteArray(new URL(imageUrl));
                            DocPicture pic = paragraph.appendPicture(imageBytes);
                            pic.setWidth(150f);  // chỉnh nếu cần
                            pic.setHeight(150f);
                            paragraph.appendBreak(BreakType.Line_Break); // xuống dòng sau ảnh
                        } catch (Exception e) {
                            System.err.println("Lỗi chèn ảnh: " + imageUrl);
                        }
                    }
                    break; // xử lý xong rồi, dừng lại
                }
            }
        }
    }


    public static String getGenderVietnamese(String gender) {
        return switch (gender) {
            case "MALE" -> "Nam";
            case "FEMALE" -> "Nữ";
            default -> "Khác";
        };
    }

}
