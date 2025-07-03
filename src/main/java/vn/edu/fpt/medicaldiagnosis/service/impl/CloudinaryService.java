package vn.edu.fpt.medicaldiagnosis.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import jakarta.annotation.Nonnull;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.text.Normalizer;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CloudinaryService {
    Cloudinary cloudinary;

    public String uploadFile(MultipartFile file) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(),
                    ObjectUtils.asMap("folder", "medical-results"));
            return result.get("secure_url").toString();
        } catch (IOException e) {
            throw new RuntimeException("Upload to Cloudinary failed", e);
        }
    }

    public String uploadFileTemplate(@Nonnull MultipartFile file) {
        try {
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.contains(".")) {
                throw new IllegalArgumentException("Invalid file name");
            }

            // Lấy tên file và đuôi mở rộng
            String extension = getExtension(originalFilename); // ví dụ: docx
            String baseName = getBaseName(originalFilename);   // ví dụ: Report_Plan
            String normalizedName = normalizeFilename(baseName);
            String uniqueName = normalizedName + "_" + UUID.randomUUID();
            String publicId = "medsoft/templates/" + uniqueName + "." + extension;

            log.info("Uploading file to Cloudinary: public_id={}, extension={}", publicId, extension);

            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "resource_type", "raw", // dùng cho mọi file
                            "public_id", publicId,
                            "use_filename", true,
                            "unique_filename", false
                    )
            );

            String url = result.get("secure_url").toString();
            log.info("Uploaded file URL: {}", url);
            return url;

        } catch (IOException e) {
            log.error("Upload to Cloudinary failed: {}", e.getMessage(), e);
            throw new RuntimeException("Upload to Cloudinary failed", e);
        }
    }

    public String uploadPreviewPdf(byte[] pdfData, String baseName) {
        try {
            String normalizedName = normalizeFilename(baseName);
            String uniqueName = normalizedName + "_preview_" + UUID.randomUUID();
            String publicId = "medsoft/templates/" + uniqueName + ".pdf";

            log.info("Uploading preview PDF to Cloudinary: {}", publicId);

            Map<?, ?> result = cloudinary.uploader().upload(
                    pdfData,
                    ObjectUtils.asMap(
                            "resource_type", "raw",
                            "public_id", publicId,
                            "use_filename", true,
                            "unique_filename", false
                    )
            );

            return result.get("secure_url").toString();
        } catch (IOException e) {
            log.error("Failed to upload PDF preview", e);
            throw new RuntimeException("Upload PDF to Cloudinary failed", e);
        }
    }

    // Lấy phần đuôi (extension)
    private String getExtension(String filename) {
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    // Lấy phần tên (không có đuôi)
    private String getBaseName(String filename) {
        return filename.substring(0, filename.lastIndexOf('.'));
    }

    // Chuẩn hóa tên file (loại bỏ dấu, ký tự đặc biệt, khoảng trắng)
    private String normalizeFilename(String input) {
        String noAccent = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String noSpecialChars = noAccent.replaceAll("[^a-zA-Z0-9\\-_]", "_");
        return StringUtils.stripAccents(noSpecialChars).toLowerCase();
    }

    public void deleteFile(String imageUrl) {
        String publicId = extractPublicIdFromUrl(imageUrl);
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (Exception e) {
            throw new RuntimeException("Cloudinary deletion failed", e);
        }
    }

    public void deleteTemplateFile(String fileUrl) {
        String publicId = extractPublicIdFromUrlTemplate(fileUrl);
        try {
            Map<?, ?> result = cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "raw"));
            String status = result.get("result").toString();

            if (!"ok".equalsIgnoreCase(status)) {
                log.warn("Cloudinary did not delete file. publicId: {}, result: {}", publicId, status);
            } else {
                log.info("Deleted file from Cloudinary: {}", publicId);
            }

        } catch (Exception e) {
            log.error("Failed to delete file on Cloudinary: {}", publicId, e);
            throw new RuntimeException("Cloudinary deletion failed", e);
        }
    }


    private String extractPublicIdFromUrlTemplate(String url) {
        try {
            // Cắt phần sau /upload/
            String afterUpload = url.substring(url.indexOf("/upload/") + 8);

            // Nếu có version (v1234567/...), cắt bỏ
            if (afterUpload.startsWith("v") && afterUpload.contains("/")) {
                afterUpload = afterUpload.substring(afterUpload.indexOf("/") + 1);
            }

            // GIỮ NGUYÊN đuôi .docx nếu là 1 phần của public_id
            return afterUpload;

        } catch (Exception e) {
            throw new RuntimeException("Failed to extract public ID from URL: " + url, e);
        }
    }

    private String extractPublicIdFromUrl(String url) {
        String[] parts = url.split("/");
        String filename = parts[parts.length - 1];
        return filename.split("\\.")[0];
    }
}
