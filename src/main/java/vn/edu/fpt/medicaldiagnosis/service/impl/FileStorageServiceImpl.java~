package vn.edu.fpt.medicaldiagnosis.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.medicaldiagnosis.service.FileStorageService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileStorageServiceImpl implements FileStorageService {
    // Cấu hình thư mục gốc cho từng loại
    String IMAGE_BASE_UPLOAD_DIR = "/var/www/uploads/images";
    String IMAGE_BASE_PUBLIC_URL = "https://api.datnd.id.vn/uploads/images";

    String FILE_BASE_UPLOAD_DIR = "/var/www/uploads/files";
    String FILE_BASE_PUBLIC_URL = "https://api.datnd.id.vn/uploads/files";

    @Override
    public String storeImageFile(MultipartFile file, String subFolder) throws IOException {
        log.info("Storing file: {}", file.getOriginalFilename());

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or null");
        }

        String originalFileName = Paths.get(file.getOriginalFilename()).getFileName().toString();
        String fileName = UUID.randomUUID() + "_" + originalFileName;

        boolean isImage = isImageFile(originalFileName);

        String baseUploadDir = isImage ? IMAGE_BASE_UPLOAD_DIR : FILE_BASE_UPLOAD_DIR;
        String basePublicUrl = isImage ? IMAGE_BASE_PUBLIC_URL : FILE_BASE_PUBLIC_URL;

        Path folderPath = (subFolder != null && !subFolder.isBlank())
                ? Paths.get(baseUploadDir, subFolder)
                : Paths.get(baseUploadDir);

        Path filePath = folderPath.resolve(fileName);
        Files.createDirectories(folderPath); // Tạo thư mục nếu chưa có

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Stored file: {}", filePath);

        String publicPath = (subFolder != null && !subFolder.isBlank())
                ? subFolder + "/" + fileName
                : fileName;

        return basePublicUrl + "/" + publicPath;
    }

    @Override
    public String storeDocFile(MultipartFile file, String subFolder, String customFileName) throws IOException {
        log.info("Storing file with custom name: {}", customFileName);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or null");
        }

        String originalFileName = file.getOriginalFilename();
        boolean isImage = isImageFile(originalFileName);

        String baseUploadDir = isImage ? IMAGE_BASE_UPLOAD_DIR : FILE_BASE_UPLOAD_DIR;
        String basePublicUrl = isImage ? IMAGE_BASE_PUBLIC_URL : FILE_BASE_PUBLIC_URL;

        Path folderPath = (subFolder != null && !subFolder.isBlank())
                ? Paths.get(baseUploadDir, subFolder)
                : Paths.get(baseUploadDir);

        Path filePath = folderPath.resolve(customFileName);
        Files.createDirectories(folderPath); // Tạo thư mục nếu chưa có

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Stored file: {}", filePath);

        String publicPath = (subFolder != null && !subFolder.isBlank())
                ? subFolder + "/" + customFileName
                : customFileName;

        return basePublicUrl + "/" + publicPath;
    }


    @Override
    public void deleteFile(String fileUrl) throws IOException {
        log.info("Deleting file: {}", fileUrl);
        if (fileUrl == null || fileUrl.isBlank()) return;

        String basePublicUrl = fileUrl.contains("/uploads/files/")
                ? FILE_BASE_PUBLIC_URL
                : IMAGE_BASE_PUBLIC_URL;

        String baseUploadDir = fileUrl.contains("/uploads/files/")
                ? FILE_BASE_UPLOAD_DIR
                : IMAGE_BASE_UPLOAD_DIR;

        String relativePath = fileUrl.replace(basePublicUrl, "");
        relativePath = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;

        Path filePath = Paths.get(baseUploadDir, relativePath);
        Files.deleteIfExists(filePath);
        log.info("Deleted file: {}", relativePath);
    }

    private boolean isImageFile(String filename) {
        String lower = filename.toLowerCase();
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".gif") || lower.endsWith(".webp");
    }
}
