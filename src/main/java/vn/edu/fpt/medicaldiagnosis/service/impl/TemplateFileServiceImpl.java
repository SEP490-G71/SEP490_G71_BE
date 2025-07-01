package vn.edu.fpt.medicaldiagnosis.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.medicaldiagnosis.common.DocxConverterService;
import vn.edu.fpt.medicaldiagnosis.dto.request.TemplateFileRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.TemplateFileResponse;
import vn.edu.fpt.medicaldiagnosis.entity.TemplateFile;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.repository.TemplateFileRepository;
import vn.edu.fpt.medicaldiagnosis.service.TemplateFileService;

import java.util.Optional;

import static lombok.AccessLevel.PRIVATE;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class TemplateFileServiceImpl implements TemplateFileService {
    CloudinaryService cloudinaryService;
    TemplateFileRepository templateFileRepository;
    DocxConverterService docxConverterService;

    @Override
    public TemplateFileResponse uploadTemplate(MultipartFile file, TemplateFileRequest request) {
        try {
            String originalFilename = file.getOriginalFilename();
            String baseName = originalFilename != null
                    ? originalFilename.substring(0, originalFilename.lastIndexOf('.'))
                    : "template";

            // 1. Upload DOCX lên Cloudinary
            String fileUrl = cloudinaryService.uploadFileTemplate(file);

//            // 2. Convert DOCX → PDF
//            byte[] pdfBytes = docxConverterService.convertDocxToPdf(file);
//
//            // 3. Upload PDF lên Cloudinary
//            String previewUrl = cloudinaryService.uploadPreviewPdf(pdfBytes, baseName);
            String previewUrl = "";
            // 4. Save DB
            TemplateFile template = TemplateFile.builder()
                    .type(request.getType())
                    .name(Optional.ofNullable(request.getName()).orElse(originalFilename))
                    .fileUrl(fileUrl)
                    .previewUrl(previewUrl)
                    .isDefault(false)
                    .build();

            templateFileRepository.save(template);

            return TemplateFileResponse.builder()
                    .id(template.getId())
                    .name(template.getName())
                    .type(template.getType())
                    .fileUrl(template.getFileUrl())
                    .previewUrl(template.getPreviewUrl())
                    .isDefault(template.getIsDefault())
                    .build();

        } catch (Exception e) {
            log.error("Failed to upload template file", e);
            throw new AppException(ErrorCode.UPLOAD_TO_CLOUDINARY_FAILED);
        }
    }
}
