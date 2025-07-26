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
            @RequestParam("file") MultipartFile[] files,
            @RequestParam(value = "note", required = false) String note,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("staffId") String staffId
    ) {
        log.info("Controller: upload multiple files");
        if (files == null || files.length == 0) {
            throw new AppException(ErrorCode.FILE_NOT_PROVIDED);
        }

        medicalResultService.uploadMedicalResults(medicalOrderId, files, note, staffId, description);
        return ApiResponse.<String>builder()
                .message("Upload success")
                .result("Uploaded " + files.length + " files")
                .build();
    }

    @PutMapping("/{resultId}/update")
    public ApiResponse<String> updateMedicalResults(
            @PathVariable String resultId,
            @RequestParam("file") MultipartFile[] files,
            @RequestParam(value = "note", required = false) String note,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("staffId") String staffId
    ) {
        medicalResultService.updateMedicalResults(resultId, files, note, staffId, description);
        return ApiResponse.<String>builder()
                .message("Medical result updated successfully")
                .build();
    }

}
