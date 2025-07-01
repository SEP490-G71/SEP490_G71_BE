package vn.edu.fpt.medicaldiagnosis.service;

import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.medicaldiagnosis.dto.request.TemplateFileRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.TemplateFileResponse;

public interface TemplateFileService {
    TemplateFileResponse uploadTemplate(MultipartFile file, TemplateFileRequest request);
}
