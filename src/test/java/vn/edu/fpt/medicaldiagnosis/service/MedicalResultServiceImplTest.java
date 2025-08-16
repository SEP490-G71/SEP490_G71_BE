package vn.edu.fpt.medicaldiagnosis.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.fpt.medicaldiagnosis.dto.request.UpdateMedicalOrderStatusRequest;
import vn.edu.fpt.medicaldiagnosis.entity.*;
import vn.edu.fpt.medicaldiagnosis.enums.MedicalOrderStatus;
import vn.edu.fpt.medicaldiagnosis.enums.MedicalRecordStatus;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.repository.MedicalOrderRepository;
import vn.edu.fpt.medicaldiagnosis.repository.MedicalResultImageRepository;
import vn.edu.fpt.medicaldiagnosis.repository.MedicalResultRepository;
import vn.edu.fpt.medicaldiagnosis.repository.StaffRepository;
import vn.edu.fpt.medicaldiagnosis.service.impl.MedicalResultServiceImpl;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MedicalResultServiceImplTest {

    @Mock MedicalOrderRepository medicalOrderRepository;
    @Mock StaffRepository staffRepository;
    @Mock MedicalResultRepository medicalResultRepository;
    @Mock FileStorageService fileStorageService;
    @Mock MedicalResultImageRepository medicalResultImageRepository;
    @Mock MedicalOrderService medicalOrderService;
    @Mock WorkScheduleService workScheduleService;

    @InjectMocks
    MedicalResultServiceImpl service;

    // ========= Helpers =========
    private MedicalOrder order(String id, MedicalRecord record, MedicalOrderStatus status) {
        MedicalOrder o = new MedicalOrder();
        o.setId(id);
        o.setMedicalRecord(record);
        o.setStatus(status);
        return o;
    }

    private MedicalRecord record(MedicalRecordStatus status) {
        MedicalRecord r = new MedicalRecord();
        r.setStatus(status);
        return r;
    }

    private Staff staff(String id) {
        Staff s = new Staff();
        s.setId(id);
        return s;
    }

    private MedicalResult result(String id, MedicalOrder order) {
        MedicalResult res = new MedicalResult();
        res.setId(id);
        res.setMedicalOrder(order);
        return res;
    }

    // Chỉ cho nhánh lỗi: KHÔNG stub save(order)
    private void mockGuardsForErrorPath(String orderId, String staffId, MedicalOrder ord, Staff st) {
        when(medicalOrderRepository.findByIdAndDeletedAtIsNull(orderId)).thenReturn(Optional.of(ord));
        when(staffRepository.findByIdAndDeletedAtIsNull(staffId)).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow(staffId)).thenReturn(true);
        when(medicalResultRepository.save(any(MedicalResult.class)))
                .thenAnswer(inv -> inv.getArgument(0)); // result được save trước vòng upload file
    }

    // Cho happy-path: CÓ stub save(order)
    private void mockGuardsForHappyPath(String orderId, String staffId, MedicalOrder ord, Staff st) {
        mockGuardsForErrorPath(orderId, staffId, ord, st);
        when(medicalOrderRepository.save(any(MedicalOrder.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private void mockGuardsCommon(String resultId, String staffId, MedicalResult res, Staff st) {
        when(medicalResultRepository.findByIdAndDeletedAtIsNull(resultId)).thenReturn(Optional.of(res));
        when(staffRepository.findByIdAndDeletedAtIsNull(staffId)).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow(staffId)).thenReturn(true);
//        when(medicalResultRepository.save(any(MedicalResult.class))).thenAnswer(inv -> inv.getArgument(0));
    }


    // =========================================================
    // 1) Order không tồn tại → ném AppException MEDICAL_ORDER_NOT_FOUND
    // =========================================================
    @Test
    void uploadMedicalResults_orderNotFound_throws() {
        String orderId = "O404";
        when(medicalOrderRepository.findByIdAndDeletedAtIsNull(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.uploadMedicalResults(orderId, null, "note", "S1", "desc"))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.MEDICAL_ORDER_NOT_FOUND);

        verify(medicalOrderRepository).findByIdAndDeletedAtIsNull(orderId);
        verifyNoMoreInteractions(medicalOrderService, medicalResultRepository,
                medicalResultImageRepository, fileStorageService, staffRepository, workScheduleService);
    }

    // =========================================================
    // 2) Staff không tồn tại → ném AppException STAFF_NOT_FOUND
    // =========================================================
    @Test
    void uploadMedicalResults_staffNotFound_throws() {
        String orderId = "O1";
        MedicalRecord rec = record(MedicalRecordStatus.TESTING);
        MedicalOrder ord = order(orderId, rec, MedicalOrderStatus.WAITING);

        when(medicalOrderRepository.findByIdAndDeletedAtIsNull(orderId)).thenReturn(Optional.of(ord));
        when(staffRepository.findByIdAndDeletedAtIsNull("S404")).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.uploadMedicalResults(orderId, null, "note", "S404", "desc"))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.STAFF_NOT_FOUND);

        verify(staffRepository).findByIdAndDeletedAtIsNull("S404");
        verify(medicalOrderRepository).findByIdAndDeletedAtIsNull(orderId);
        verifyNoMoreInteractions(medicalOrderService, medicalResultRepository,
                medicalResultImageRepository, fileStorageService, workScheduleService);
    }

    // =========================================================
    // 3) Staff không trong ca → ném ACTION_NOT_ALLOWED
    // =========================================================
    @Test
    void uploadMedicalResults_staffNotOnShift_throws() {
        String orderId = "O2";
        String staffId = "S2";
        MedicalRecord rec = record(MedicalRecordStatus.TESTING);
        MedicalOrder ord = order(orderId, rec, MedicalOrderStatus.WAITING);
        Staff st = staff(staffId);

        when(medicalOrderRepository.findByIdAndDeletedAtIsNull(orderId)).thenReturn(Optional.of(ord));
        when(staffRepository.findByIdAndDeletedAtIsNull(staffId)).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow(staffId)).thenReturn(false);

        assertThatThrownBy(() ->
                service.uploadMedicalResults(orderId, null, "note", staffId, "desc"))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACTION_NOT_ALLOWED);

        verify(workScheduleService).isStaffOnShiftNow(staffId);
        verifyNoMoreInteractions(medicalOrderService, medicalResultRepository,
                medicalResultImageRepository, fileStorageService);
    }

    // =========================================================
    // 4) MedicalRecord đang WAITING_FOR_PAYMENT → ném PAYMENT_REQUIRED
    // =========================================================
    @Test
    void uploadMedicalResults_recordWaitingForPayment_throws() {
        String orderId = "O3";
        String staffId = "S3";
        MedicalRecord rec = record(MedicalRecordStatus.WAITING_FOR_PAYMENT);
        MedicalOrder ord = order(orderId, rec, MedicalOrderStatus.WAITING);
        Staff st = staff(staffId);

        when(medicalOrderRepository.findByIdAndDeletedAtIsNull(orderId)).thenReturn(Optional.of(ord));
        when(staffRepository.findByIdAndDeletedAtIsNull(staffId)).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow(staffId)).thenReturn(true);

        assertThatThrownBy(() ->
                service.uploadMedicalResults(orderId, null, "note", staffId, "desc"))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_REQUIRED);

        verifyNoMoreInteractions(medicalOrderService, medicalResultRepository,
                medicalResultImageRepository, fileStorageService);
    }

    // =========================================================
    // 5) MedicalOrder đã COMPLETED → ném MEDICAL_RESULT_IS_COMPLETED
    // =========================================================
    @Test
    void uploadMedicalResults_orderAlreadyCompleted_throws() {
        String orderId = "O5";
        String staffId = "S5";
        MedicalRecord rec = record(MedicalRecordStatus.TESTING);
        MedicalOrder ord = order(orderId, rec, MedicalOrderStatus.COMPLETED);
        Staff st = staff(staffId);

        when(medicalOrderRepository.findByIdAndDeletedAtIsNull(orderId)).thenReturn(Optional.of(ord));
        when(staffRepository.findByIdAndDeletedAtIsNull(staffId)).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow(staffId)).thenReturn(true);

        assertThatThrownBy(() ->
                service.uploadMedicalResults(orderId, new MultipartFile[0], "note", staffId, "desc"))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.MEDICAL_RESULT_IS_COMPLETED);

        verifyNoMoreInteractions(medicalOrderService, medicalResultRepository,
                medicalResultImageRepository, fileStorageService);
    }

    // =========================================================
    // 6) files == null → không lưu image, vẫn tạo result & cập nhật trạng thái
    // =========================================================
    @Test
    void filesNull_noImageSaved_success() {
        String orderId = "O6"; String staffId = "S6";
        MedicalOrder ord = order(orderId, record(MedicalRecordStatus.TESTING), MedicalOrderStatus.WAITING);
        Staff st = staff(staffId);
        mockGuardsForHappyPath(orderId, staffId, ord, st);

        service.uploadMedicalResults(orderId, null, "note", staffId, "desc");

        verifyNoInteractions(fileStorageService);
        verify(medicalResultImageRepository, never()).save(any());
        verify(medicalResultRepository, times(1)).save(any(MedicalResult.class));

        ArgumentCaptor<UpdateMedicalOrderStatusRequest> reqCap = ArgumentCaptor.forClass(UpdateMedicalOrderStatusRequest.class);
        verify(medicalOrderService).updateMedicalOrderStatus(reqCap.capture());
        UpdateMedicalOrderStatusRequest req = reqCap.getValue();
        org.assertj.core.api.Assertions.assertThat(req.getMedicalOrderId()).isEqualTo(orderId);
        org.assertj.core.api.Assertions.assertThat(req.getStatus()).isEqualTo(MedicalOrderStatus.COMPLETED);

        ArgumentCaptor<MedicalOrder> orderCap = ArgumentCaptor.forClass(MedicalOrder.class);
        verify(medicalOrderRepository, atLeastOnce()).save(orderCap.capture());
        org.assertj.core.api.Assertions.assertThat(orderCap.getValue().getStatus()).isEqualTo(MedicalOrderStatus.COMPLETED);
    }

    // =========================================================
    // 7) files.length == 0 → tương tự #6
    // =========================================================
    @Test
    void filesEmpty_noImageSaved_success() {
        String orderId = "O7"; String staffId = "S7";
        MedicalOrder ord = order(orderId, record(MedicalRecordStatus.TESTING), MedicalOrderStatus.WAITING);
        Staff st = staff(staffId);
        mockGuardsForHappyPath(orderId, staffId, ord, st);

        MultipartFile[] files = new MultipartFile[0];
        service.uploadMedicalResults(orderId, files, "note", staffId, "desc");

        verifyNoInteractions(fileStorageService);
        verify(medicalResultImageRepository, never()).save(any());
        verify(medicalResultRepository, times(1)).save(any(MedicalResult.class));
        verify(medicalOrderService, times(1)).updateMedicalOrderStatus(any());
        verify(medicalOrderRepository, atLeastOnce()).save(any(MedicalOrder.class));
    }

    // =========================================================
    // 8) 1 file OK → lưu 1 image
    // =========================================================
    @Test
    void singleFile_success_imageSavedOnce() throws Exception {
        String orderId = "O8"; String staffId = "S8";
        MedicalOrder ord = order(orderId, record(MedicalRecordStatus.TESTING), MedicalOrderStatus.WAITING);
        Staff st = staff(staffId);
        mockGuardsForHappyPath(orderId, staffId, ord, st);

        MultipartFile file = mock(MultipartFile.class);
        // ❌ BỎ DÒNG NÀY: when(file.getOriginalFilename()).thenReturn("a.png");
        when(fileStorageService.storeImageFile(eq(file), anyString()))
                .thenReturn("http://u/a.png");

        service.uploadMedicalResults(orderId, new MultipartFile[]{file}, "note", staffId, "desc");

        verify(fileStorageService, times(1)).storeImageFile(eq(file), anyString());
        verify(medicalResultImageRepository, times(1)).save(any());
        verify(medicalResultRepository, times(1)).save(any());
        verify(medicalOrderService, times(1)).updateMedicalOrderStatus(any());
        verify(medicalOrderRepository, atLeastOnce()).save(any(MedicalOrder.class));
    }


    // =========================================================
    // 9) nhiều file OK → lưu đủ số image
    // =========================================================
    @Test
    void multipleFiles_success_allImagesSaved() throws Exception {
        String orderId = "O9"; String staffId = "S9";
        MedicalOrder ord = order(orderId, record(MedicalRecordStatus.TESTING), MedicalOrderStatus.WAITING);
        Staff st = staff(staffId);
        mockGuardsForHappyPath(orderId, staffId, ord, st);

        MultipartFile f1 = mock(MultipartFile.class);
        MultipartFile f2 = mock(MultipartFile.class);
        // ❌ BỎ 2 dòng stub getOriginalFilename() vì không dùng ở happy-path

        when(fileStorageService.storeImageFile(eq(f1), anyString())).thenReturn("http://u/a.png");
        when(fileStorageService.storeImageFile(eq(f2), anyString())).thenReturn("http://u/b.png");

        service.uploadMedicalResults(orderId, new MultipartFile[]{f1, f2}, "note", staffId, "desc");

        verify(fileStorageService, times(1)).storeImageFile(eq(f1), anyString());
        verify(fileStorageService, times(1)).storeImageFile(eq(f2), anyString());
        verify(medicalResultImageRepository, times(2)).save(any());
        verify(medicalResultRepository, times(1)).save(any());
        verify(medicalOrderService, times(1)).updateMedicalOrderStatus(any());
        verify(medicalOrderRepository, atLeastOnce()).save(any(MedicalOrder.class));
    }


    // =========================================================
    // 10) storage trả URL rỗng → FILE_UPLOAD_FAILED, không có image, không update status
    // =========================================================
    @Test
    void storageReturnsBlankUrl_throwsAndRollback() throws Exception {
        String orderId = "O10"; String staffId = "S10";
        MedicalOrder ord = order(orderId, record(MedicalRecordStatus.TESTING), MedicalOrderStatus.WAITING);
        Staff st = staff(staffId);
        mockGuardsForErrorPath(orderId, staffId, ord, st); // <— ERROR PATH

        MultipartFile f1 = mock(MultipartFile.class);
        when(f1.getOriginalFilename()).thenReturn("bad.png");
        when(fileStorageService.storeImageFile(eq(f1), anyString())).thenReturn("   "); // blank

        assertThatThrownBy(() ->
                service.uploadMedicalResults(orderId, new MultipartFile[]{f1}, "note", staffId, "desc"))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FILE_UPLOAD_FAILED);

        verify(medicalResultImageRepository, never()).save(any());
        verify(medicalOrderService, never()).updateMedicalOrderStatus(any());
        verify(medicalOrderRepository, never()).save(any(MedicalOrder.class));
    }

    // =========================================================
    // 11) storage ném exception → FILE_UPLOAD_FAILED, không có image, không update status
    // =========================================================
    @Test
    void storageThrowsException_throwsAndRollback() throws Exception {
        String orderId = "O11"; String staffId = "S11";
        MedicalOrder ord = order(orderId, record(MedicalRecordStatus.TESTING), MedicalOrderStatus.WAITING);
        Staff st = staff(staffId);
        mockGuardsForErrorPath(orderId, staffId, ord, st); // <— ERROR PATH

        MultipartFile f1 = mock(MultipartFile.class);
        when(f1.getOriginalFilename()).thenReturn("err.png");
        when(fileStorageService.storeImageFile(eq(f1), anyString()))
                .thenThrow(new RuntimeException("IO fail"));

        assertThatThrownBy(() ->
                service.uploadMedicalResults(orderId, new MultipartFile[]{f1}, "note", staffId, "desc"))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FILE_UPLOAD_FAILED);

        verify(medicalResultImageRepository, never()).save(any());
        verify(medicalOrderService, never()).updateMedicalOrderStatus(any());
        verify(medicalOrderRepository, never()).save(any(MedicalOrder.class));
    }

    // =========================================================
    // 12) nhiều file, file 1 OK, file 2 fail → FILE_UPLOAD_FAILED (mô phỏng rollback)
    // =========================================================
    @Test
    void multiFiles_secondFails_throwsAndRollback() throws Exception {
        String orderId = "O12"; String staffId = "S12";
        MedicalOrder ord = order(orderId, record(MedicalRecordStatus.TESTING), MedicalOrderStatus.WAITING);
        Staff st = staff(staffId);
        mockGuardsForErrorPath(orderId, staffId, ord, st); // ERROR PATH

        MultipartFile f1 = mock(MultipartFile.class);
        MultipartFile f2 = mock(MultipartFile.class);

        // ❌ Bỏ dòng này đi vì không dùng: when(f1.getOriginalFilename()).thenReturn("ok.png");
        when(f2.getOriginalFilename()).thenReturn("fail.png"); // cần cho log lỗi

        when(fileStorageService.storeImageFile(eq(f1), anyString()))
                .thenReturn("http://u/ok.png");
        when(fileStorageService.storeImageFile(eq(f2), anyString()))
                .thenThrow(new RuntimeException("IO fail"));

        assertThatThrownBy(() ->
                service.uploadMedicalResults(orderId, new MultipartFile[]{f1, f2}, "note", staffId, "desc"))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FILE_UPLOAD_FAILED);

        // File 1 đã lưu image, file 2 fail -> tổng cộng đúng 1 lần
        verify(medicalResultImageRepository, times(1)).save(any());
        // Không update status & không save order do exception
        verify(medicalOrderService, never()).updateMedicalOrderStatus(any());
        verify(medicalOrderRepository, never()).save(any(MedicalOrder.class));
    }

    // =========================================================
// 13) Happy path: gọi updateOrderStatus với request đúng
// =========================================================
    @Test
    void happyPath_callsUpdateOrderStatusWithCorrectRequest() {
        String orderId = "O13"; String staffId = "S13";
        MedicalOrder ord = order(orderId, record(MedicalRecordStatus.TESTING), MedicalOrderStatus.WAITING);
        Staff st = staff(staffId);
        mockGuardsForHappyPath(orderId, staffId, ord, st);

        service.uploadMedicalResults(orderId, null, "ghi chú", staffId, "mô tả");

        ArgumentCaptor<UpdateMedicalOrderStatusRequest> reqCap = ArgumentCaptor.forClass(UpdateMedicalOrderStatusRequest.class);
        verify(medicalOrderService).updateMedicalOrderStatus(reqCap.capture());
        UpdateMedicalOrderStatusRequest req = reqCap.getValue();
        org.assertj.core.api.Assertions.assertThat(req.getMedicalOrderId()).isEqualTo(orderId);
        org.assertj.core.api.Assertions.assertThat(req.getStatus()).isEqualTo(MedicalOrderStatus.COMPLETED);
    }

    // =========================================================
// 14) Happy path: set trạng thái order = COMPLETED và save order
// =========================================================
    @Test
    void happyPath_setsOrderStatusCompleted_andSavesOrder() {
        String orderId = "O14"; String staffId = "S14";
        MedicalOrder ord = order(orderId, record(MedicalRecordStatus.TESTING), MedicalOrderStatus.WAITING);
        Staff st = staff(staffId);
        mockGuardsForHappyPath(orderId, staffId, ord, st);

        service.uploadMedicalResults(orderId, null, "note", staffId, "desc");

        ArgumentCaptor<MedicalOrder> orderCap = ArgumentCaptor.forClass(MedicalOrder.class);
        verify(medicalOrderRepository, atLeastOnce()).save(orderCap.capture());
        org.assertj.core.api.Assertions.assertThat(orderCap.getValue().getStatus())
                .isEqualTo(MedicalOrderStatus.COMPLETED);
    }

    // =========================================================
// 15) Happy path: lưu MedicalResult với đúng trường (note/desc/completedBy/order)
// =========================================================
    @Test
    void happyPath_savesMedicalResultWithCorrectFields() {
        String orderId = "O15"; String staffId = "S15";
        MedicalOrder ord = order(orderId, record(MedicalRecordStatus.TESTING), MedicalOrderStatus.WAITING);
        Staff st = staff(staffId);
        mockGuardsForHappyPath(orderId, staffId, ord, st);

        String note = "NOTE_X";
        String desc = "DESC_Y";
        service.uploadMedicalResults(orderId, null, note, staffId, desc);

        ArgumentCaptor<MedicalResult> resCap = ArgumentCaptor.forClass(MedicalResult.class);
        verify(medicalResultRepository).save(resCap.capture());
        MedicalResult saved = resCap.getValue();
        org.assertj.core.api.Assertions.assertThat(saved.getResultNote()).isEqualTo(note);
        org.assertj.core.api.Assertions.assertThat(saved.getDescription()).isEqualTo(desc);
        org.assertj.core.api.Assertions.assertThat(saved.getCompletedBy().getId()).isEqualTo(staffId);
        org.assertj.core.api.Assertions.assertThat(saved.getMedicalOrder().getId()).isEqualTo(orderId);
    }

    // =========================================================
// 16) (tuỳ chọn) Happy path: lưu images với URL trả về (không blank)
// =========================================================
    @Test
    void happyPath_savesImagesWithReturnedUrls() throws Exception {
        String orderId = "O16"; String staffId = "S16";
        MedicalOrder ord = order(orderId, record(MedicalRecordStatus.TESTING), MedicalOrderStatus.WAITING);
        Staff st = staff(staffId);
        mockGuardsForHappyPath(orderId, staffId, ord, st);

        MultipartFile f1 = mock(MultipartFile.class);
        MultipartFile f2 = mock(MultipartFile.class);
        // happy-path không cần stub getOriginalFilename()
        when(fileStorageService.storeImageFile(eq(f1), anyString())).thenReturn("http://u/a.png");
        when(fileStorageService.storeImageFile(eq(f2), anyString())).thenReturn("http://u/b.png");

        service.uploadMedicalResults(orderId, new MultipartFile[]{f1, f2}, "note", staffId, "desc");

        ArgumentCaptor<vn.edu.fpt.medicaldiagnosis.entity.MedicalResultImage> imgCap =
                ArgumentCaptor.forClass(vn.edu.fpt.medicaldiagnosis.entity.MedicalResultImage.class);
        verify(medicalResultImageRepository, times(2)).save(imgCap.capture());

        var savedImgs = imgCap.getAllValues();
        org.assertj.core.api.Assertions.assertThat(savedImgs)
                .extracting(vn.edu.fpt.medicaldiagnosis.entity.MedicalResultImage::getImageUrl)
                .containsExactlyInAnyOrder("http://u/a.png", "http://u/b.png");
        // đảm bảo không blank
        savedImgs.forEach(i ->
                org.assertj.core.api.Assertions.assertThat(i.getImageUrl()).isNotBlank());
    }


    // 1) resultNotFound -> MEDICAL_RESULT_NOT_FOUND
    @Test
    void update_resultNotFound_throws() {
        String resultId = "R404";
        when(medicalResultRepository.findByIdAndDeletedAtIsNull(resultId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.updateMedicalResults(resultId, null, "n", "S1", "d", null))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.MEDICAL_RESULT_NOT_FOUND);

        verifyNoInteractions(staffRepository, workScheduleService, fileStorageService,
                medicalResultImageRepository, medicalOrderService);
    }

    // 2) staffNotFound -> STAFF_NOT_FOUND
    @Test
    void update_staffNotFound_throws() {
        String resultId = "R2"; String staffId = "S404";
        MedicalOrder ord = order("O2", record(MedicalRecordStatus.TESTING), MedicalOrderStatus.WAITING);
        MedicalResult res = result(resultId, ord);

        when(medicalResultRepository.findByIdAndDeletedAtIsNull(resultId)).thenReturn(Optional.of(res));
        when(staffRepository.findByIdAndDeletedAtIsNull(staffId)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.updateMedicalResults(resultId, null, "n", staffId, "d", null))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.STAFF_NOT_FOUND);

        verifyNoInteractions(fileStorageService, medicalResultImageRepository, medicalOrderService);
    }

    // 3) staffNotOnShift -> ACTION_NOT_ALLOWED
    @Test
    void update_staffNotOnShift_throws() {
        String resultId = "R3"; String staffId = "S3";
        MedicalOrder ord = order("O3", record(MedicalRecordStatus.TESTING), MedicalOrderStatus.WAITING);
        MedicalResult res = result(resultId, ord);
        Staff st = staff(staffId);

        when(medicalResultRepository.findByIdAndDeletedAtIsNull(resultId)).thenReturn(Optional.of(res));
        when(staffRepository.findByIdAndDeletedAtIsNull(staffId)).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow(staffId)).thenReturn(false);

        assertThatThrownBy(() ->
                service.updateMedicalResults(resultId, null, "n", staffId, "d", null))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACTION_NOT_ALLOWED);

        verifyNoInteractions(fileStorageService, medicalResultImageRepository, medicalOrderService);
    }

    // 4) recordWaitingForPayment -> PAYMENT_REQUIRED
    @Test
    void update_recordWaitingForPayment_throws() {
        String resultId = "R4"; String staffId = "S4";
        MedicalOrder ord = order("O4", record(MedicalRecordStatus.WAITING_FOR_PAYMENT), MedicalOrderStatus.WAITING);
        MedicalResult res = result(resultId, ord);
        Staff st = staff(staffId);

        when(medicalResultRepository.findByIdAndDeletedAtIsNull(resultId)).thenReturn(Optional.of(res));
        when(staffRepository.findByIdAndDeletedAtIsNull(staffId)).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow(staffId)).thenReturn(true);

        assertThatThrownBy(() ->
                service.updateMedicalResults(resultId, null, "n", staffId, "d", null))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.PAYMENT_REQUIRED);

        verifyNoInteractions(fileStorageService, medicalResultImageRepository, medicalOrderService);
        verify(medicalResultRepository, never()).save(any());
    }

    // =================================
    // B) Xoá ảnh theo ID (6)
    // =================================

    // 5) deleteIds == null -> no delete
    @Test
    void deleteIds_null_noDelete() throws IOException {
        String resultId = "R5"; String staffId = "S5";
        MedicalOrder ord = order("O5", record(MedicalRecordStatus.TESTING), MedicalOrderStatus.WAITING);
        MedicalResult res = result(resultId, ord);
        Staff st = staff(staffId);
        mockGuardsCommon(resultId, staffId, res, st);

        service.updateMedicalResults(resultId, null, "n", staffId, "d", null);

        verify(medicalResultImageRepository, never()).findAllById(anyList());
        verify(medicalResultImageRepository, never()).deleteAllInBatch(anyList());
        verify(fileStorageService, never()).deleteFile(anyString());
        verify(medicalOrderService, times(1)).updateMedicalOrderStatus(any());
    }

    // 6) deleteIds empty -> no delete
    @Test
    void deleteIds_empty_noDelete() throws IOException {
        String resultId = "R6"; String staffId = "S6";
        MedicalOrder ord = order("O6", record(MedicalRecordStatus.TESTING), MedicalOrderStatus.WAITING);
        MedicalResult res = result(resultId, ord);
        Staff st = staff(staffId);
        mockGuardsCommon(resultId, staffId, res, st);

        service.updateMedicalResults(resultId, null, "n", staffId, "d", List.of());

        verify(medicalResultImageRepository, never()).findAllById(anyList());
        verify(medicalResultImageRepository, never()).deleteAllInBatch(anyList());
        verify(fileStorageService, never()).deleteFile(anyString());
        verify(medicalOrderService, times(1)).updateMedicalOrderStatus(any());
    }

    // 7) deleteIds some found -> deleteFile for each + deleteAllInBatch
    @Test
    void deleteIds_someFound_success() throws IOException {
        String resultId = "R7"; String staffId = "S7";
        MedicalOrder ord = order("O7", record(MedicalRecordStatus.TESTING), MedicalOrderStatus.WAITING);
        MedicalResult res = result(resultId, ord);
        Staff st = staff(staffId);
        mockGuardsCommon(resultId, staffId, res, st);

        MedicalResultImage img1 = new MedicalResultImage();
        img1.setId("I1"); img1.setImageUrl("u1");
        MedicalResultImage img2 = new MedicalResultImage();
        img2.setId("I2"); img2.setImageUrl("u2");

        when(medicalResultImageRepository.findAllById(List.of("I1","I2"))).thenReturn(List.of(img1, img2));

        service.updateMedicalResults(resultId, null, "n", staffId, "d", List.of("I1","I2"));

        verify(fileStorageService, times(1)).deleteFile("u1");
        verify(fileStorageService, times(1)).deleteFile("u2");
        ArgumentCaptor<List<MedicalResultImage>> cap = ArgumentCaptor.forClass(List.class);
        verify(medicalResultImageRepository, times(1)).deleteAllInBatch(cap.capture());
        // ensure right list
        org.assertj.core.api.Assertions.assertThat(cap.getValue()).containsExactlyInAnyOrder(img1, img2);

        verify(medicalOrderService, times(1)).updateMedicalOrderStatus(any());
    }

    // 8) deleteIds none found -> noop
    @Test
    void deleteIds_noneFound_noop() throws IOException {
        String resultId = "R8"; String staffId = "S8";
        MedicalOrder ord = order("O8", record(MedicalRecordStatus.TESTING), MedicalOrderStatus.WAITING);
        MedicalResult res = result(resultId, ord);
        Staff st = staff(staffId);
        mockGuardsCommon(resultId, staffId, res, st);

        when(medicalResultImageRepository.findAllById(List.of("X","Y"))).thenReturn(List.of());

        service.updateMedicalResults(resultId, null, "n", staffId, "d", List.of("X","Y"));

        verify(fileStorageService, never()).deleteFile(anyString());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<java.util.List<vn.edu.fpt.medicaldiagnosis.entity.MedicalResultImage>> cap =
                ArgumentCaptor.forClass((Class) java.util.List.class);
        verify(medicalResultImageRepository, times(1)).deleteAllInBatch(cap.capture());
        org.assertj.core.api.Assertions.assertThat(cap.getValue()).isEmpty();

        verify(medicalOrderService, times(1)).updateMedicalOrderStatus(any());
    }


    // 9) deleteFile throws -> FILE_DELETE_FAILED, no deleteAllInBatch, no updateStatus
    @Test
    void deleteFileThrows_throwsAndNoBatchDelete() throws IOException {
        String resultId = "R9"; String staffId = "S9";
        MedicalOrder ord = order("O9", record(MedicalRecordStatus.TESTING), MedicalOrderStatus.WAITING);
        MedicalResult res = result(resultId, ord);
        Staff st = staff(staffId);
        mockGuardsCommon(resultId, staffId, res, st);

        MedicalResultImage img = new MedicalResultImage();
        img.setId("I");
        img.setImageUrl("url");
        when(medicalResultImageRepository.findAllById(List.of("I"))).thenReturn(List.of(img));
        doThrow(new RuntimeException("io")).when(fileStorageService).deleteFile("url");

        assertThatThrownBy(() ->
                service.updateMedicalResults(resultId, null, "n", staffId, "d", List.of("I")))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FILE_DELETE_FAILED);

        verify(medicalResultImageRepository, never()).deleteAllInBatch(anyList());
        verify(medicalOrderService, never()).updateMedicalOrderStatus(any());
        verify(medicalResultRepository, never()).save(any()); // fields update not committed
    }

    // 10) second deleteFile throws -> FILE_DELETE_FAILED, no batch delete, no update
    @Test
    void deleteFileSecondThrows_partialNotCommitted() throws IOException {
        String resultId = "R10"; String staffId = "S10";
        MedicalOrder ord = order("O10", record(MedicalRecordStatus.TESTING), MedicalOrderStatus.WAITING);
        MedicalResult res = result(resultId, ord);
        Staff st = staff(staffId);
        mockGuardsCommon(resultId, staffId, res, st);

        MedicalResultImage img1 = new MedicalResultImage(); img1.setId("I1"); img1.setImageUrl("u1");
        MedicalResultImage img2 = new MedicalResultImage(); img2.setId("I2"); img2.setImageUrl("u2");

        when(medicalResultImageRepository.findAllById(List.of("I1","I2"))).thenReturn(List.of(img1, img2));
        // first ok, second throws
        doNothing().when(fileStorageService).deleteFile("u1");
        doThrow(new RuntimeException("io")).when(fileStorageService).deleteFile("u2");

        assertThatThrownBy(() ->
                service.updateMedicalResults(resultId, null, "n", staffId, "d", List.of("I1","I2")))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FILE_DELETE_FAILED);

        verify(medicalResultImageRepository, never()).deleteAllInBatch(anyList());
        verify(medicalOrderService, never()).updateMedicalOrderStatus(any());
        verify(medicalResultRepository, never()).save(any());
    }

    // =================================
    // C) Cập nhật thông tin kết quả (3)
    // =================================

    // 11) luôn save result với fields cập nhật
    @Test
    void happy_updatesResultFields_once() {
        String resultId = "R11"; String staffId = "S11";
        MedicalOrder ord = order("O11", record(MedicalRecordStatus.TESTING), MedicalOrderStatus.WAITING);
        MedicalResult res = result(resultId, ord);
        Staff st = staff(staffId);
        mockGuardsCommon(resultId, staffId, res, st);

        String note = "NOTE_X"; String desc = "DESC_Y";
        service.updateMedicalResults(resultId, null, note, staffId, desc, null);

        ArgumentCaptor<MedicalResult> cap = ArgumentCaptor.forClass(MedicalResult.class);
        verify(medicalResultRepository, times(1)).save(cap.capture());
        MedicalResult saved = cap.getValue();
        org.assertj.core.api.Assertions.assertThat(saved.getResultNote()).isEqualTo(note);
        org.assertj.core.api.Assertions.assertThat(saved.getDescription()).isEqualTo(desc);
        org.assertj.core.api.Assertions.assertThat(saved.getCompletedBy().getId()).isEqualTo(staffId);
        org.assertj.core.api.Assertions.assertThat(saved.getMedicalOrder().getId()).isEqualTo("O11");
    }

    // 12) gọi updateOrderStatus với request đúng
    @Test
    void happy_callsUpdateOrderStatus_correctRequest() {
        String resultId = "R12"; String staffId = "S12";
        MedicalOrder ord = order("O12", record(MedicalRecordStatus.TESTING), MedicalOrderStatus.WAITING);
        MedicalResult res = result(resultId, ord);
        Staff st = staff(staffId);
        mockGuardsCommon(resultId, staffId, res, st);

        service.updateMedicalResults(resultId, null, "n", staffId, "d", null);

        ArgumentCaptor<UpdateMedicalOrderStatusRequest> reqCap = ArgumentCaptor.forClass(UpdateMedicalOrderStatusRequest.class);
        verify(medicalOrderService).updateMedicalOrderStatus(reqCap.capture());
        UpdateMedicalOrderStatusRequest req = reqCap.getValue();
        org.assertj.core.api.Assertions.assertThat(req.getMedicalOrderId()).isEqualTo("O12");
        org.assertj.core.api.Assertions.assertThat(req.getStatus()).isEqualTo(MedicalOrderStatus.COMPLETED);
    }

    // 13) không có file hợp lệ -> không saveAll ảnh
    @Test
    void happy_noNewImages_noSaveAll() {
        String resultId = "R13"; String staffId = "S13";
        MedicalOrder ord = order("O13", record(MedicalRecordStatus.TESTING), MedicalOrderStatus.WAITING);
        MedicalResult res = result(resultId, ord);
        Staff st = staff(staffId);
        mockGuardsCommon(resultId, staffId, res, st);

        MultipartFile empty = mock(MultipartFile.class);
        when(empty.isEmpty()).thenReturn(true);
        MultipartFile[] files = new MultipartFile[]{null, empty};

        service.updateMedicalResults(resultId, files, "n", staffId, "d", null);

        verify(medicalResultImageRepository, never()).saveAll(anyList());
        verify(medicalOrderService, times(1)).updateMedicalOrderStatus(any());
    }

    // =================================
    // D) Upload ảnh mới (7)
    // =================================

    // 14) files == null -> không saveAll
    @Test
    void filesNull_noSaveAll_success() {
        String resultId = "R14"; String staffId = "S14";
        MedicalOrder ord = order("O14", record(MedicalRecordStatus.TESTING), MedicalOrderStatus.WAITING);
        MedicalResult res = result(resultId, ord);
        Staff st = staff(staffId);
        mockGuardsCommon(resultId, staffId, res, st);

        service.updateMedicalResults(resultId, null, "n", staffId, "d", null);

        verify(medicalResultImageRepository, never()).saveAll(anyList());
        verify(medicalOrderService, times(1)).updateMedicalOrderStatus(any());
    }

    // 15) files rỗng -> không saveAll
    @Test
    void filesEmptyArray_noSaveAll_success() {
        String resultId = "R15"; String staffId = "S15";
        MedicalOrder ord = order("O15", record(MedicalRecordStatus.TESTING), MedicalOrderStatus.WAITING);
        MedicalResult res = result(resultId, ord);
        Staff st = staff(staffId);
        mockGuardsCommon(resultId, staffId, res, st);

        service.updateMedicalResults(resultId, new MultipartFile[0], "n", staffId, "d", null);

        verify(medicalResultImageRepository, never()).saveAll(anyList());
        verify(medicalOrderService, times(1)).updateMedicalOrderStatus(any());
    }

    // 16) tất cả phần tử null/empty -> không saveAll
    @Test
    void filesAllNullOrEmpty_noSaveAll_success() {
        String resultId = "R16"; String staffId = "S16";
        MedicalOrder ord = order("O16", record(MedicalRecordStatus.TESTING), MedicalOrderStatus.WAITING);
        MedicalResult res = result(resultId, ord);
        Staff st = staff(staffId);
        mockGuardsCommon(resultId, staffId, res, st);

        MultipartFile empty = mock(MultipartFile.class);
        when(empty.isEmpty()).thenReturn(true);

        service.updateMedicalResults(resultId, new MultipartFile[]{null, empty}, "n", staffId, "d", null);

        verify(medicalResultImageRepository, never()).saveAll(anyList());
        verify(medicalOrderService, times(1)).updateMedicalOrderStatus(any());
    }

    // 17) single file OK -> saveAll size 1
    @Test
    void singleFile_success_saveAllWithOne() throws Exception {
        String resultId = "R17"; String staffId = "S17";
        MedicalOrder ord = order("O17", record(MedicalRecordStatus.TESTING), MedicalOrderStatus.WAITING);
        MedicalResult res = result(resultId, ord);
        Staff st = staff(staffId);
        mockGuardsCommon(resultId, staffId, res, st);

        MultipartFile f1 = mock(MultipartFile.class);
        when(fileStorageService.storeImageFile(eq(f1), anyString())).thenReturn("http://u/a.png");

        service.updateMedicalResults(resultId, new MultipartFile[]{f1}, "n", staffId, "d", null);

        ArgumentCaptor<List<MedicalResultImage>> cap = ArgumentCaptor.forClass(List.class);
        verify(medicalResultImageRepository).saveAll(cap.capture());
        List<MedicalResultImage> list = cap.getValue();
        org.assertj.core.api.Assertions.assertThat(list).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(list.get(0).getImageUrl()).isEqualTo("http://u/a.png");

        verify(medicalOrderService, times(1)).updateMedicalOrderStatus(any());
    }

    // 18) multiple files OK -> saveAll size n, urls khớp
    @Test
    void multipleFiles_success_saveAllWithAll() throws Exception {
        String resultId = "R18"; String staffId = "S18";
        MedicalOrder ord = order("O18", record(MedicalRecordStatus.TESTING), MedicalOrderStatus.WAITING);
        MedicalResult res = result(resultId, ord);
        Staff st = staff(staffId);
        mockGuardsCommon(resultId, staffId, res, st);

        MultipartFile f1 = mock(MultipartFile.class);
        MultipartFile f2 = mock(MultipartFile.class);
        when(fileStorageService.storeImageFile(eq(f1), anyString())).thenReturn("http://u/a.png");
        when(fileStorageService.storeImageFile(eq(f2), anyString())).thenReturn("http://u/b.png");

        service.updateMedicalResults(resultId, new MultipartFile[]{f1, f2}, "n", staffId, "d", null);

        ArgumentCaptor<List<MedicalResultImage>> cap = ArgumentCaptor.forClass(List.class);
        verify(medicalResultImageRepository).saveAll(cap.capture());
        List<MedicalResultImage> list = cap.getValue();
        org.assertj.core.api.Assertions.assertThat(list).hasSize(2);
        org.assertj.core.api.Assertions.assertThat(list)
                .extracting(MedicalResultImage::getImageUrl)
                .containsExactlyInAnyOrder("http://u/a.png", "http://u/b.png");

        verify(medicalOrderService, times(1)).updateMedicalOrderStatus(any());
    }

    // 19) store returns blank -> FILE_UPLOAD_FAILED, no saveAll, no updateStatus
    @Test
    void storeReturnsBlankUrl_throwsAndNoSaveAll() throws Exception {
        String resultId = "R19"; String staffId = "S19";
        MedicalOrder ord = order("O19", record(MedicalRecordStatus.TESTING), MedicalOrderStatus.WAITING);
        MedicalResult res = result(resultId, ord);
        Staff st = staff(staffId);
        mockGuardsCommon(resultId, staffId, res, st);

        MultipartFile f1 = mock(MultipartFile.class);
        when(fileStorageService.storeImageFile(eq(f1), anyString())).thenReturn("  ");

        assertThatThrownBy(() ->
                service.updateMedicalResults(resultId, new MultipartFile[]{f1}, "n", staffId, "d", null))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FILE_UPLOAD_FAILED);

        verify(medicalResultImageRepository, never()).saveAll(anyList());
        verify(medicalOrderService, never()).updateMedicalOrderStatus(any());
    }

    // 20) store throws -> FILE_UPLOAD_FAILED, no saveAll, no updateStatus
    @Test
    void storeThrowsException_throwsAndNoSaveAll() throws Exception {
        String resultId = "R20"; String staffId = "S20";
        MedicalOrder ord = order("O20", record(MedicalRecordStatus.TESTING), MedicalOrderStatus.WAITING);
        MedicalResult res = result(resultId, ord);
        Staff st = staff(staffId);
        mockGuardsCommon(resultId, staffId, res, st);

        MultipartFile f1 = mock(MultipartFile.class);
        when(fileStorageService.storeImageFile(eq(f1), anyString())).thenThrow(new RuntimeException("io"));

        assertThatThrownBy(() ->
                service.updateMedicalResults(resultId, new MultipartFile[]{f1}, "n", staffId, "d", null))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FILE_UPLOAD_FAILED);

        verify(medicalResultImageRepository, never()).saveAll(anyList());
        verify(medicalOrderService, never()).updateMedicalOrderStatus(any());
    }

    // 21) multi files, second fails -> FILE_UPLOAD_FAILED, no saveAll, no updateStatus
    @Test
    void multiFiles_secondFails_throwsAndNoSaveAll() throws Exception {
        String resultId = "R21"; String staffId = "S21";
        MedicalOrder ord = order("O21", record(MedicalRecordStatus.TESTING), MedicalOrderStatus.WAITING);
        MedicalResult res = result(resultId, ord);
        Staff st = staff(staffId);
        mockGuardsCommon(resultId, staffId, res, st);

        MultipartFile f1 = mock(MultipartFile.class);
        MultipartFile f2 = mock(MultipartFile.class);
        when(fileStorageService.storeImageFile(eq(f1), anyString())).thenReturn("http://u/ok.png");
        when(fileStorageService.storeImageFile(eq(f2), anyString())).thenThrow(new RuntimeException("io"));

        assertThatThrownBy(() ->
                service.updateMedicalResults(resultId, new MultipartFile[]{f1, f2}, "n", staffId, "d", null))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.FILE_UPLOAD_FAILED);

        verify(medicalResultImageRepository, never()).saveAll(anyList());
        verify(medicalOrderService, never()).updateMedicalOrderStatus(any());
    }
}
