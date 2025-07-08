package vn.edu.fpt.medicaldiagnosis.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileStorageService {
    String storeFile(MultipartFile file, String subFolder) throws IOException;

    void deleteFile(String fileUrl) throws IOException;
}
