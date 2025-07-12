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
import vn.edu.fpt.medicaldiagnosis.entity.ByteArrayMultipartFile;
import vn.edu.fpt.medicaldiagnosis.entity.TemplateFile;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.TemplateFileMapper;
import vn.edu.fpt.medicaldiagnosis.repository.TemplateFileRepository;
import vn.edu.fpt.medicaldiagnosis.service.FileStorageService;
import vn.edu.fpt.medicaldiagnosis.service.TemplateFileService;
import vn.edu.fpt.medicaldiagnosis.specification.TemplateFileSpecification;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static lombok.AccessLevel.PRIVATE;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class TemplateFileServiceImpl implements TemplateFileService {
    TemplateFileRepository templateFileRepository;
    FileStorageService fileStorageService;
    TemplateFileMapper templateFileMapper;
    DocxConverterService docxConverterService;
    @Override
    public TemplateFileResponse uploadTemplate(MultipartFile file, TemplateFileRequest request) {
        try {
            String originalFilename = file.getOriginalFilename();
            String baseName = originalFilename != null
                    ? originalFilename.substring(0, originalFilename.lastIndexOf('.'))
                    : "template";
            // Nếu người dùng muốn tạo file mặc định, kiểm tra xem đã tồn tại file mặc định chưa
            boolean isDefault = Boolean.TRUE.equals(request.getIsDefault()); // tránh null
            if (isDefault) {
                boolean existsDefault = templateFileRepository.existsByTypeAndIsDefaultTrueAndDeletedAtIsNull(request.getType());
                if (existsDefault) {
                    throw new AppException(ErrorCode.ALREADY_HAS_DEFAULT_TEMPLATE);
                }
            }

            // 0. Generate shared UUID for both DOCX and PDF
            String uuid = UUID.randomUUID().toString();

            // 1. Tạo tên file cố định
            String docxFileName = baseName + "_" + uuid + ".docx";
            String pdfFileName = baseName + "_" + uuid + ".pdf";

            // 2. Upload DOCX
            String fileUrl = fileStorageService.storeDocFile(file, "", docxFileName);

            // 3. Convert DOCX → PDF và upload
            byte[] pdfBytes = docxConverterService.convertDocxToPdf(file);
            MultipartFile pdfMultipartFile = new ByteArrayMultipartFile(pdfBytes, pdfFileName, "application/pdf");
            String previewUrl = fileStorageService.storeDocFile(pdfMultipartFile, "", pdfFileName);

            // 4. Save DB
            TemplateFile template = TemplateFile.builder()
                    .type(request.getType())
                    .name(Optional.ofNullable(request.getName()).orElse(originalFilename))
                    .fileUrl(fileUrl)
                    .previewUrl(previewUrl)
                    .isDefault(false)
                    .build();

            templateFileRepository.save(template);

            return templateFileMapper.toTemplateFileResponse(template);
        } catch (Exception e) {
            log.error("Failed to upload template file", e);
            throw new AppException(ErrorCode.UPLOAD_TO_VPS_FAILED);
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


        // Nếu đang từ mặc định -> không mặc định
        boolean isChangingDefault = !request.getIsDefault() && template.getIsDefault();
        if (isChangingDefault) {
            long defaultCount = templateFileRepository.countByTypeAndIsDefaultTrueAndDeletedAtIsNull(template.getType());
            if (defaultCount <= 1) {
                throw new AppException(ErrorCode.CANNOT_REMOVE_LAST_DEFAULT_TEMPLATE);
            }
        }

        // Nếu đang từ không mặc định -> mặc định => check đã có mặc định khác chưa
        boolean isAssigningDefault = request.getIsDefault() && !template.getIsDefault();
        if (isAssigningDefault) {
            boolean existsOtherDefault = templateFileRepository.existsByTypeAndIsDefaultTrueAndIdNotAndDeletedAtIsNull(
                    template.getType(), template.getId()
            );
            if (existsOtherDefault) {
                throw new AppException(ErrorCode.ALREADY_HAS_DEFAULT_TEMPLATE);
            }
        }

        if (file != null && !file.isEmpty()) {
            try {
                // Xoá file cũ nếu tồn tại
                fileStorageService.deleteFile(template.getFileUrl());
                if (template.getPreviewUrl() != null && !template.getPreviewUrl().isBlank()) {
                    fileStorageService.deleteFile(template.getPreviewUrl());
                }
                // Tạo UUID mới để đồng bộ DOCX và PDF
                String uuid = UUID.randomUUID().toString();

                // Lấy tên base (không có đuôi .docx)
                String originalFilename = file.getOriginalFilename();
                String baseName = originalFilename != null
                        ? originalFilename.substring(0, originalFilename.lastIndexOf('.'))
                        : "template";

                // Tạo tên file
                String docxFileName = baseName + "_" + uuid + ".docx";
                String pdfFileName = baseName + "_" + uuid + ".pdf";

                // Upload file DOCX mới
                String fileUrl = fileStorageService.storeDocFile(file, "", docxFileName);
                template.setFileUrl(fileUrl);
                template.setName(Optional.ofNullable(request.getName()).orElse(originalFilename));

                // Convert DOCX → PDF và upload
                byte[] pdfBytes = docxConverterService.convertDocxToPdf(file);
                MultipartFile pdfFile = new ByteArrayMultipartFile(pdfBytes, pdfFileName, "application/pdf");
                String previewUrl = fileStorageService.storeDocFile(pdfFile, "", pdfFileName);
                template.setPreviewUrl(previewUrl);
            } catch (Exception e) {
                log.error("Failed to update template file and preview", e);
                throw new AppException(ErrorCode.UPLOAD_TO_VPS_FAILED);
            }
        }

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

        // Không cho xóa nếu đây là file mặc định cuối cùng
        if (template.getIsDefault()) {
            long countDefaults = templateFileRepository.countByTypeAndIsDefaultTrueAndDeletedAtIsNull(template.getType());
            if (countDefaults <= 1) {
                throw new AppException(ErrorCode.CANNOT_DELETE_DEFAULT_TEMPLATE);
            }
        }

        // Không cho xóa nếu đây là file duy nhất còn lại của type
        long totalCount = templateFileRepository.countByTypeAndDeletedAtIsNull(template.getType());
        if (totalCount <= 1) {
            throw new AppException(ErrorCode.CANNOT_DELETE_LAST_TEMPLATE);
        }

        try {
            fileStorageService.deleteFile(template.getFileUrl());
            fileStorageService.deleteFile(template.getPreviewUrl());
        } catch (IOException e) {
            log.warn("Failed to delete file from VPS: {}", e.getMessage());
        }

        template.setDeletedAt(LocalDateTime.now());
        templateFileRepository.save(template);
    }
}
