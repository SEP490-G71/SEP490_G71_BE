package vn.edu.fpt.medicaldiagnosis.service;


import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MedicalResultService {
    void uploadMedicalResults(String medicalOrderId, MultipartFile[] file, String note, String staffId, String description);

    void updateMedicalResults(String resultId, MultipartFile[] files, String note, String staffId, String description, List<String> deleteImageIds);
}
