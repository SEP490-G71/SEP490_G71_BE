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
    public void uploadMedicalResults(String medicalOrderId, MultipartFile[] files, String note, String staffId) {
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
                .build();
        medicalResultRepository.save(result);

        // Tạo các ảnh kết quả (MedicalResultImage)
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
        medicalOrder.setStatus(MedicalOrderStatus.COMPLETED);
        medicalOrderRepository.save(medicalOrder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateMedicalResults(String resultId, MultipartFile[] files, String note, String staffId) {
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

        // Xoá toàn bộ ảnh cũ
        List<MedicalResultImage> oldImages = medicalResultImageRepository.findAllByMedicalResultIdAndDeletedAtIsNull(resultId);
        medicalResultImageRepository.deleteAll(oldImages);

        for (MedicalResultImage image : oldImages) {
            try {
                log.info("Deleting image from server: {}", image.getImageUrl());
                fileStorageService.deleteFile(image.getImageUrl()); // Xoá trên server
                medicalResultImageRepository.delete(image);        // Xoá trong DB
            } catch (Exception e) {
                log.error("Failed to delete image from servers: {}", image.getImageUrl(), e);
                throw new AppException(ErrorCode.FILE_DELETE_FAILED);
            }
        }

        // Cập nhật ghi chú và staff hoàn tất
        result.setResultNote(note);
        result.setCompletedBy(staff);
        medicalResultRepository.save(result);

        // Upload ảnh mới
        for (MultipartFile file : files) {
            try {
                String url = fileStorageService.storeImageFile(file, "");
                if (url == null || url.isBlank()) {
                    throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
                }
                log.info("Uploaded new file: {}", file.getOriginalFilename());
                MedicalResultImage newImage = MedicalResultImage.builder()
                        .medicalResult(result)
                        .imageUrl(url)
                        .build();
                medicalResultImageRepository.save(newImage);
            } catch (Exception ex) {
                log.error("Error uploading new file: {}", file.getOriginalFilename(), ex);
                throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
            }
        }
        log.info("Updated medical result {} with {} new images", resultId, files.length);
    }
}
