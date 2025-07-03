package vn.edu.fpt.medicaldiagnosis.service;

import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.medicaldiagnosis.dto.request.TemplateFileRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.TemplateFileResponse;

import java.util.Map;

public interface TemplateFileService {
    TemplateFileResponse uploadTemplate(MultipartFile file, TemplateFileRequest request);

    Page<TemplateFileResponse> getTemplatesPaged(Map<String, String> filters, int page, int size, String sortBy, String sortDir);

    TemplateFileResponse updateTemplate(String id, MultipartFile file, TemplateFileRequest request);

    void deleteTemplate(String id);
}
