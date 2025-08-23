package vn.edu.fpt.medicaldiagnosis.common;

import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;

import java.util.List;
import java.util.Objects;

public class FileValidator {
    // Danh sách MIME type hợp lệ cho ảnh
    private static final List<String> IMAGE_CONTENT_TYPES = List.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/bmp"
    );

    // Danh sách đuôi file hợp lệ (backup check)
    private static final List<String> IMAGE_EXTENSIONS = List.of(
            "jpg", "jpeg", "png", "gif", "webp", "bmp"
    );

    public static void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED, "File rỗng hoặc null");
        }

        // Check content-type
        String contentType = file.getContentType();
        if (contentType == null || !IMAGE_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED,
                    "Chỉ được phép upload file ảnh (jpg, png, gif, webp, bmp)");
        }

        // Check extension (phòng trường hợp content-type bị fake)
        String filename = Objects.requireNonNull(file.getOriginalFilename());
        String ext = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        if (!IMAGE_EXTENSIONS.contains(ext)) {
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED,
                    "Định dạng file không hợp lệ: ." + ext);
        }
    }
}
