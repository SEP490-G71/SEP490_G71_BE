package vn.edu.fpt.medicaldiagnosis.service.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.fpt.medicaldiagnosis.entity.*;
import vn.edu.fpt.medicaldiagnosis.enums.MedicalOrderStatus;
import vn.edu.fpt.medicaldiagnosis.enums.MedicalRecordStatus;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.repository.MedicalOrderRepository;
import vn.edu.fpt.medicaldiagnosis.repository.MedicalResultImageRepository;
import vn.edu.fpt.medicaldiagnosis.repository.MedicalResultRepository;
import vn.edu.fpt.medicaldiagnosis.repository.StaffRepository;
import vn.edu.fpt.medicaldiagnosis.service.FileStorageService;
import vn.edu.fpt.medicaldiagnosis.service.MedicalResultService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MedicalResultServiceImpl implements MedicalResultService {

    MedicalOrderRepository medicalOrderRepository;
    StaffRepository staffRepository;
    MedicalResultRepository medicalResultRepository;
    FileStorageService fileStorageService;
    MedicalResultImageRepository medicalResultImageRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadMedicalResults(String medicalOrderId, MultipartFile[] files, String note, String staffId, String description) {
        log.info("Service: upload medical results for order {}", medicalOrderId);

        MedicalOrder medicalOrder = medicalOrderRepository.findByIdAndDeletedAtIsNull(medicalOrderId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_ORDER_NOT_FOUND));

        Staff staff = staffRepository.findByIdAndDeletedAtIsNull(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        MedicalRecord medicalRecord = medicalOrder.getMedicalRecord();
        if(medicalRecord.getStatus() == MedicalRecordStatus.WAITING_FOR_PAYMENT) {
            throw new AppException(ErrorCode.PAYMENT_REQUIRED);
        }

        if(medicalOrder.getStatus() == MedicalOrderStatus.COMPLETED) {
            throw new AppException(ErrorCode.MEDICAL_RESULT_IS_COMPLETED);
        }
        // Tạo kết quả khám (MedicalResult)
        MedicalResult result = MedicalResult.builder()
                .medicalOrder(medicalOrder)
                .resultNote(note)
                .completedBy(staff)
                .description(description)
                .build();
        medicalResultRepository.save(result);

        // Tạo các ảnh kết quả (MedicalResultImage) nếu có file
        if (files != null) {
            if (files.length > 0) {
                for (MultipartFile file : files) {
                    try {
                        String url = fileStorageService.storeImageFile(file, "");

                        if (url == null || url.isBlank()) {
                            log.error("Upload failed or empty URL for file: {}", file.getOriginalFilename());
                            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
                        }

                        MedicalResultImage image = MedicalResultImage.builder()
                                .medicalResult(result)
                                .imageUrl(url)
                                .build();

                        medicalResultImageRepository.save(image);

                    } catch (Exception ex) {
                        log.error("Error uploading file: {}", file.getOriginalFilename(), ex);
                        throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
                    }
                }
            }
        }
        medicalOrder.setStatus(MedicalOrderStatus.COMPLETED);
        medicalOrderRepository.save(medicalOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMedicalResults(String resultId, MultipartFile[] files, String note, String staffId, String description, List<String> deleteImageIds) {
        log.info("Service: update medical results for resultId {}", resultId);

        MedicalResult result = medicalResultRepository.findByIdAndDeletedAtIsNull(resultId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RESULT_NOT_FOUND));

        // Kiểm tra staff
        Staff staff = staffRepository.findByIdAndDeletedAtIsNull(staffId)
                .orElseThrow(() -> new AppException(ErrorCode.STAFF_NOT_FOUND));

        // Kiểm tra trạng thái của medical record
        MedicalRecord record = result.getMedicalOrder().getMedicalRecord();
        if (record.getStatus() == MedicalRecordStatus.WAITING_FOR_PAYMENT) {
            throw new AppException(ErrorCode.PAYMENT_REQUIRED);
        }

        // Xoá ảnh theo ID nếu được yêu cầu
        if (deleteImageIds != null && !deleteImageIds.isEmpty()) {
            List<MedicalResultImage> imagesToDelete = medicalResultImageRepository.findAllById(deleteImageIds);
            for (MedicalResultImage image : imagesToDelete) {
                try {
                    log.info("Deleting image from server: {}", image.getImageUrl());
                    fileStorageService.deleteFile(image.getImageUrl());
                } catch (Exception e) {
                    log.error("Failed to delete image: {}", image.getImageUrl(), e);
                    throw new AppException(ErrorCode.FILE_DELETE_FAILED);
                }
            }
            // ⚠️ Gọi 1 lần để xoá hàng loạt ảnh trong DB
            medicalResultImageRepository.deleteAllInBatch(imagesToDelete);
            log.info("Deleted {} images from result {}", imagesToDelete.size(), resultId);
        }

        // Cập nhật ghi chú và staff hoàn tất
        result.setResultNote(note);
        result.setDescription(description);
        result.setCompletedBy(staff);
        medicalResultRepository.save(result);

        // 6. Xử lý upload ảnh mới (nếu có)
        boolean hasValidFile = files != null && Arrays.stream(files).anyMatch(f -> f != null && !f.isEmpty());
        if (hasValidFile) {
            List<MedicalResultImage> newImages = new ArrayList<>();

            for (MultipartFile file : files) {
                if (file == null || file.isEmpty()) continue;

                try {
                    String url = fileStorageService.storeImageFile(file, "");

                    if (url == null || url.isBlank()) {
                        log.error("File uploaded but URL is blank: {}", file.getOriginalFilename());
                        throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
                    }

                    MedicalResultImage image = MedicalResultImage.builder()
                            .medicalResult(result)
                            .imageUrl(url)
                            .build();

                    newImages.add(image);
                    log.info("Prepared image for upload: {}", file.getOriginalFilename());
                } catch (Exception ex) {
                    log.error("Error uploading file: {}", file.getOriginalFilename(), ex);
                    throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
                }
            }

            // ⛳ Batch save ảnh mới nếu có ít nhất 1 file hợp lệ
            if (!newImages.isEmpty()) {
                medicalResultImageRepository.saveAll(newImages);
                log.info("Uploaded {} new images for result {}", newImages.size(), resultId);
            }
        } else {
            log.info("No new images uploaded for result {}", resultId);
        }

        log.info("Update medical result {} completed.", resultId);
    }
}
