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
    String BASE_UPLOAD_DIR = "/var/www/uploads/images";
    String BASE_PUBLIC_URL = "https://api.datnd.id.vn/uploads/images";

    @Override
    public String storeFile(MultipartFile file, String subFolder) throws IOException {
        log.info("Storing file: {}", file.getOriginalFilename());
        // Không sử dụng subFolder trong trường hợp này
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or null");
        }

        String originalFileName = Paths.get(file.getOriginalFilename()).getFileName().toString(); // tránh path injection
        String fileName = UUID.randomUUID() + "_" + originalFileName;
        Path filePath = Paths.get(BASE_UPLOAD_DIR, fileName);

        Files.createDirectories(filePath.getParent()); // Tạo thư mục nếu chưa có
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Stored file: {}", fileName);
        return BASE_PUBLIC_URL + "/" + fileName;
    }

    @Override
    public void deleteFile(String fileUrl) throws IOException {
        log.info("Deleting file: {}", fileUrl);
        if (fileUrl == null || fileUrl.isBlank()) return;

        String relativePath = fileUrl.replace(BASE_PUBLIC_URL, "");
        relativePath = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;

        Path filePath = Paths.get(BASE_UPLOAD_DIR, relativePath);
        Files.deleteIfExists(filePath);
        log.info("Deleted file: {}", relativePath);
    }
}
