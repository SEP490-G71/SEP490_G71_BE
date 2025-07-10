package vn.edu.fpt.medicaldiagnosis.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileStorageService {
    String storeImageFile(MultipartFile file, String subFolder) throws IOException;

    String storeDocFile(MultipartFile file, String subFolder, String customFileName) throws IOException;

    void deleteFile(String fileUrl) throws IOException;
}
