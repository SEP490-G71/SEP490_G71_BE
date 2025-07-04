package vn.edu.fpt.medicaldiagnosis.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.medicaldiagnosis.common.DocxConverterService;
import vn.edu.fpt.medicaldiagnosis.dto.request.TemplateFileRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.TemplateFileResponse;
import vn.edu.fpt.medicaldiagnosis.entity.TemplateFile;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.TemplateFileMapper;
import vn.edu.fpt.medicaldiagnosis.repository.TemplateFileRepository;
import vn.edu.fpt.medicaldiagnosis.service.TemplateFileService;
import vn.edu.fpt.medicaldiagnosis.specification.TemplateFileSpecification;

import java.time.LocalDateTime;
import java.util.Map;
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
    TemplateFileMapper templateFileMapper;
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

    @Override
    public Page<TemplateFileResponse> getTemplatesPaged(Map<String, String> filters, int page, int size, String sortBy, String sortDir) {
        String sortColumn = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortColumn).ascending() : Sort.by(sortColumn).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Specification<TemplateFile> spec = TemplateFileSpecification.buildSpecification(filters);
        Page<TemplateFile> pageResult = templateFileRepository.findAll(spec, pageable);
        return pageResult.map(templateFileMapper::toTemplateFileResponse);
    }

    @Override
    public TemplateFileResponse updateTemplate(String id, MultipartFile file, TemplateFileRequest request) {
        log.info("Service: update template {}", id);

        TemplateFile template = templateFileRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.TEMPLATE_FILE_NOT_FOUND));

        boolean isChangingDefault = !request.getIsDefault() && template.getIsDefault();

        // Nếu đang bỏ mặc định mẫu duy nhất → không cho phép
        if (isChangingDefault) {
            long defaultCount = templateFileRepository.countByTypeAndIsDefaultTrueAndDeletedAtIsNull(template.getType());
            if (defaultCount <= 1) {
                throw new AppException(ErrorCode.CANNOT_REMOVE_LAST_DEFAULT_TEMPLATE);
            }
        }

        // Nếu có file mới, thì xóa file cũ và upload file mới
        if (file != null && !file.isEmpty()) {
            // Xoá file cũ khỏi Cloudinary
            cloudinaryService.deleteTemplateFile(template.getFileUrl());

            // Upload file mới
            String fileUrl = cloudinaryService.uploadFileTemplate(file);
            template.setFileUrl(fileUrl);
            template.setName(Optional.ofNullable(request.getName()).orElse(file.getOriginalFilename()));
        }

        // Cập nhật các thông tin còn lại
        template.setType(request.getType());
        template.setIsDefault(request.getIsDefault());
        template.setUpdatedAt(LocalDateTime.now());

        templateFileRepository.save(template);
        return templateFileMapper.toTemplateFileResponse(template);
    }


    @Override
    public void deleteTemplate(String id) {
        log.info("Service: delete template {}", id);
        TemplateFile template = templateFileRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.TEMPLATE_FILE_NOT_FOUND));

        // Nếu mẫu đang là mặc định, kiểm tra xem còn mẫu mặc định khác không
        if (template.getIsDefault()) {
            long countDefaults = templateFileRepository.countByTypeAndIsDefaultTrueAndDeletedAtIsNull(template.getType());
            if (countDefaults <= 1) {
                throw new AppException(ErrorCode.CANNOT_DELETE_DEFAULT_TEMPLATE);
            }
        }

        // Xoá file khỏi Cloudinary
        cloudinaryService.deleteTemplateFile(template.getFileUrl());

        // Soft delete
        template.setDeletedAt(LocalDateTime.now());
        templateFileRepository.save(template);
    }
}
