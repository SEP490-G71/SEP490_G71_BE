package vn.edu.fpt.medicaldiagnosis.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.medicaldiagnosis.dto.request.TemplateFileRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.PagedResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.TemplateFileResponse;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.service.TemplateFileService;

import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/template-files")
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class TemplateFileController {
    TemplateFileService templateFileService;
    @PostMapping
    public ApiResponse<TemplateFileResponse> uploadTemplate(
            @RequestPart("file") MultipartFile file,
            @RequestPart("info") String rawInfo) {
        log.info("Service: upload template file");
        ObjectMapper mapper = new ObjectMapper();
        TemplateFileRequest request;

        try {
            request = mapper.readValue(rawInfo, TemplateFileRequest.class);
        } catch (Exception e) {
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        TemplateFileResponse response = templateFileService.uploadTemplate(file, request);
        return ApiResponse.<TemplateFileResponse>builder()
                .result(response)
                .build();
    }

    @GetMapping
    public ApiResponse<PagedResponse<TemplateFileResponse>> getTemplates(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        log.info("Controller: get templates with filters={}, page={}, size={}, sortBy={}, sortDir={}",
                filters, page, size, sortBy, sortDir);

        Page<TemplateFileResponse> result = templateFileService.getTemplatesPaged(filters, page, size, sortBy, sortDir);

        PagedResponse<TemplateFileResponse> response = new PagedResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );

        return ApiResponse.<PagedResponse<TemplateFileResponse>>builder().result(response).build();
    }

    @PutMapping("/{id}")
    public ApiResponse<TemplateFileResponse> updateTemplate(
            @PathVariable String id,
            @RequestPart("file") MultipartFile file,
            @RequestPart("info") String rawInfo) {

        ObjectMapper mapper = new ObjectMapper();
        TemplateFileRequest request;
        try {
            request = mapper.readValue(rawInfo, TemplateFileRequest.class);
        } catch (Exception e) {
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }

        TemplateFileResponse response = templateFileService.updateTemplate(id, file, request);
        return ApiResponse.<TemplateFileResponse>builder().result(response).build();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteTemplate(@PathVariable String id) {
        templateFileService.deleteTemplate(id);
        return ApiResponse.<String>builder().result("Deleted successfully").build();
    }
}
