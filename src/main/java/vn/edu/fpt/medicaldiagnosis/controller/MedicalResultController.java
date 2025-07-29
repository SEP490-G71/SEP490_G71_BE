package vn.edu.fpt.medicaldiagnosis.controller;


import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.service.MedicalResultService;

import java.util.Arrays;
import java.util.List;

import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/medical-results")
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class MedicalResultController {
    MedicalResultService medicalResultService;

    @PostMapping("/{medicalOrderId}/upload")
    public ApiResponse<String> uploadMultipleFiles(
            @PathVariable String medicalOrderId,
            @RequestPart(value = "file", required = false) MultipartFile[] files,
            @RequestParam(value = "note", required = false) String note,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("staffId") String staffId
    ) {
        log.info("Controller: upload multiple files");
        medicalResultService.uploadMedicalResults(medicalOrderId, files, note, staffId, description);
        return ApiResponse.<String>builder()
                .message("Upload success")
                .result("Uploaded " + files.length + " files")
                .build();
    }

    @PutMapping("/{resultId}/update")
    public ApiResponse<String> updateMedicalResults(
            @PathVariable String resultId,
            @RequestParam(value = "file", required = false) MultipartFile[] files,
            @RequestParam(value = "note", required = false) String note,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("staffId") String staffId,
            @RequestParam(value = "deleteImageIds", required = false) List<String> deleteImageIds
    ) {
        medicalResultService.updateMedicalResults(resultId, files, note, staffId, description, deleteImageIds);
        return ApiResponse.<String>builder()
                .message("Medical result updated successfully")
                .build();
    }


}
