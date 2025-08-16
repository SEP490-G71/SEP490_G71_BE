package vn.edu.fpt.medicaldiagnosis.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.fpt.medicaldiagnosis.dto.request.UpdateMedicalOrderStatusRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.MedicalOrderStatusResponse;
import vn.edu.fpt.medicaldiagnosis.entity.MedicalOrder;
import vn.edu.fpt.medicaldiagnosis.entity.MedicalRecord;
import vn.edu.fpt.medicaldiagnosis.enums.MedicalOrderStatus;
import vn.edu.fpt.medicaldiagnosis.enums.MedicalRecordStatus;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.repository.MedicalOrderRepository;
import vn.edu.fpt.medicaldiagnosis.repository.MedicalRecordRepository;
import vn.edu.fpt.medicaldiagnosis.service.impl.MedicalOrderServiceImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MedicalOrderServiceImplTest {
    @Mock
    private MedicalOrderRepository medicalOrderRepository;

    @Mock
    private MedicalRecordRepository medicalRecordRepository;

    @InjectMocks
    private MedicalOrderServiceImpl service;

    @BeforeEach
    void setup() {
        service = new MedicalOrderServiceImpl(medicalOrderRepository, medicalRecordRepository);
    }

    // ------------------------
    // Helpers
    // ------------------------
    private MedicalRecord record(String id, MedicalRecordStatus status) {
        MedicalRecord r = new MedicalRecord();
        r.setId(id);
        r.setStatus(status);
        return r;
    }

    private MedicalOrder order(String id, MedicalOrderStatus status, MedicalRecord r) {
        MedicalOrder o = new MedicalOrder();
        o.setId(id);
        o.setStatus(status);
        o.setMedicalRecord(r);
        return o;
    }

    private UpdateMedicalOrderStatusRequest req(String orderId, MedicalOrderStatus status) {
        UpdateMedicalOrderStatusRequest rq = new UpdateMedicalOrderStatusRequest();
        rq.setMedicalOrderId(orderId);
        rq.setStatus(status);
        return rq;
    }
    // =========================================================
    // 1) Order không tồn tại → ném AppException
    // =========================================================
    @Test
    void updateStatus_orderNotFound_throws() {
        UpdateMedicalOrderStatusRequest request = req("O404", MedicalOrderStatus.COMPLETED);

        when(medicalOrderRepository.findByIdAndDeletedAtIsNull("O404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateMedicalOrderStatus(request))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.MEDICAL_ORDER_NOT_FOUND);

        verify(medicalOrderRepository, times(1)).findByIdAndDeletedAtIsNull("O404");
        verify(medicalOrderRepository, never()).save(any());
        verify(medicalRecordRepository, never()).save(any());
    }

    // =========================================================
    // 2) 1 order → cập nhật COMPLETED → record chuyển TESTING_COMPLETED
    // =========================================================
    @Test
    void singleOrder_updateToCompleted_recordBecomesTestingCompleted() {
        MedicalRecord r1 = record("R1", MedicalRecordStatus.TESTING);
        MedicalOrder o1 = order("O1", MedicalOrderStatus.PENDING, r1);

        when(medicalOrderRepository.findByIdAndDeletedAtIsNull("O1"))
                .thenReturn(Optional.of(o1));
        when(medicalOrderRepository.save(any(MedicalOrder.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull("R1"))
                .thenReturn(List.of(o1));
        when(medicalRecordRepository.save(any(MedicalRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        MedicalOrderStatusResponse resp =
                service.updateMedicalOrderStatus(req("O1", MedicalOrderStatus.COMPLETED));

        assertThat(resp.isAllOrdersCompleted()).isTrue();
        assertThat(resp.getRecordStatus()).isEqualTo(MedicalRecordStatus.TESTING_COMPLETED);

        verify(medicalOrderRepository).save(argThat(o -> o.getStatus() == MedicalOrderStatus.COMPLETED));
        verify(medicalRecordRepository, times(1))
                .save(argThat(r -> r.getStatus() == MedicalRecordStatus.TESTING_COMPLETED));
    }

    // =========================================================
    // 3) Nhiều order, sau cập nhật tất cả COMPLETED → record chuyển TESTING_COMPLETED
    // =========================================================
    @Test
    void multiOrders_afterUpdate_allCompleted_recordBecomesTestingCompleted() {
        MedicalRecord r1 = record("R1", MedicalRecordStatus.TESTING);
        MedicalOrder o1 = order("O1", MedicalOrderStatus.PENDING, r1);
        MedicalOrder o2 = order("O2", MedicalOrderStatus.COMPLETED, r1);
        MedicalOrder o3 = order("O3", MedicalOrderStatus.COMPLETED, r1);

        when(medicalOrderRepository.findByIdAndDeletedAtIsNull("O1"))
                .thenReturn(Optional.of(o1));
        when(medicalOrderRepository.save(any(MedicalOrder.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull("R1"))
                .then(inv -> {
                    // phản ánh trạng thái sau khi O1 được set COMPLETED
                    o1.setStatus(MedicalOrderStatus.COMPLETED);
                    return List.of(o1, o2, o3);
                });
        when(medicalRecordRepository.save(any(MedicalRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        MedicalOrderStatusResponse resp =
                service.updateMedicalOrderStatus(req("O1", MedicalOrderStatus.COMPLETED));

        assertThat(resp.isAllOrdersCompleted()).isTrue();
        assertThat(resp.getRecordStatus()).isEqualTo(MedicalRecordStatus.TESTING_COMPLETED);
        verify(medicalRecordRepository, times(1)).save(any(MedicalRecord.class));
    }

    // =========================================================
    // 4) Nhiều order, vẫn còn 1 order chưa COMPLETED → record giữ nguyên
    // =========================================================
    @Test
    void multiOrders_stillSomeNotCompleted_recordUnchanged() {
        MedicalRecord r1 = record("R1", MedicalRecordStatus.TESTING);
        MedicalOrder o1 = order("O1", MedicalOrderStatus.PENDING, r1);
        MedicalOrder o2 = order("O2", MedicalOrderStatus.COMPLETED, r1);
        MedicalOrder o3 = order("O3", MedicalOrderStatus.PENDING, r1);

        when(medicalOrderRepository.findByIdAndDeletedAtIsNull("O1"))
                .thenReturn(Optional.of(o1));
        when(medicalOrderRepository.save(any(MedicalOrder.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull("R1"))
                .thenReturn(List.of(
                        order("O1", MedicalOrderStatus.COMPLETED, r1), // sau update
                        o2,
                        o3
                ));

        MedicalOrderStatusResponse resp =
                service.updateMedicalOrderStatus(req("O1", MedicalOrderStatus.COMPLETED));

        assertThat(resp.isAllOrdersCompleted()).isFalse();
        assertThat(resp.getRecordStatus()).isEqualTo(MedicalRecordStatus.TESTING);
        verify(medicalRecordRepository, never()).save(any(MedicalRecord.class));
    }

    // =========================================================
    // 5) Cập nhật sang CANCELLED → allCompleted = false, record không đổi
    // =========================================================
    @Test
    void updateToCancelled_allCompletedFalse_recordUnchanged() {
        MedicalRecord r1 = record("R1", MedicalRecordStatus.TESTING);
        MedicalOrder o1 = order("O1", MedicalOrderStatus.PENDING, r1);
        MedicalOrder other = order("O2", MedicalOrderStatus.COMPLETED, r1);

        when(medicalOrderRepository.findByIdAndDeletedAtIsNull("O1"))
                .thenReturn(Optional.of(o1));
        when(medicalOrderRepository.save(any(MedicalOrder.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull("R1"))
                .thenReturn(List.of(order("O1", MedicalOrderStatus.CANCELLED, r1), other));

        MedicalOrderStatusResponse resp =
                service.updateMedicalOrderStatus(req("O1", MedicalOrderStatus.CANCELLED));

        assertThat(resp.isAllOrdersCompleted()).isFalse();
        assertThat(resp.getRecordStatus()).isEqualTo(MedicalRecordStatus.TESTING);
        verify(medicalRecordRepository, never()).save(any(MedicalRecord.class));
    }

    // =========================================================
    // 6) Record đã TESTING_COMPLETED & allCompleted true → không save lại record
    // =========================================================
    @Test
    void recordAlreadyTestingCompleted_andAllCompleted_noResaveRecord() {
        MedicalRecord r1 = record("R1", MedicalRecordStatus.TESTING_COMPLETED);
        MedicalOrder o1 = order("O1", MedicalOrderStatus.COMPLETED, r1);
        MedicalOrder o2 = order("O2", MedicalOrderStatus.COMPLETED, r1);

        when(medicalOrderRepository.findByIdAndDeletedAtIsNull("O1"))
                .thenReturn(Optional.of(o1));
        when(medicalOrderRepository.save(any(MedicalOrder.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull("R1"))
                .thenReturn(List.of(o1, o2));

        MedicalOrderStatusResponse resp =
                service.updateMedicalOrderStatus(req("O1", MedicalOrderStatus.COMPLETED));

        assertThat(resp.isAllOrdersCompleted()).isTrue();
        assertThat(resp.getRecordStatus()).isEqualTo(MedicalRecordStatus.TESTING_COMPLETED);
        verify(medicalRecordRepository, never()).save(any(MedicalRecord.class));
    }

    // =========================================================
    // 7) Record đã TESTING_COMPLETED nhưng thực tế chưa allCompleted → không đổi gì
    // =========================================================
    @Test
    void recordTestingCompleted_butNotAllCompleted_keepAsIs() {
        MedicalRecord r1 = record("R1", MedicalRecordStatus.TESTING_COMPLETED);
        MedicalOrder o1 = order("O1", MedicalOrderStatus.PENDING, r1);
        MedicalOrder o2 = order("O2", MedicalOrderStatus.PENDING, r1);

        when(medicalOrderRepository.findByIdAndDeletedAtIsNull("O1"))
                .thenReturn(Optional.of(o1));
        when(medicalOrderRepository.save(any(MedicalOrder.class)))
                .then(inv -> {
                    MedicalOrder saved = inv.getArgument(0);
                    // giả lập đã đổi status theo request
                    saved.setStatus(MedicalOrderStatus.COMPLETED);
                    return saved;
                });
        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull("R1"))
                .thenReturn(List.of(
                        order("O1", MedicalOrderStatus.COMPLETED, r1),
                        o2 // vẫn chưa completed
                ));

        MedicalOrderStatusResponse resp =
                service.updateMedicalOrderStatus(req("O1", MedicalOrderStatus.COMPLETED));

        assertThat(resp.isAllOrdersCompleted()).isFalse();
        assertThat(resp.getRecordStatus()).isEqualTo(MedicalRecordStatus.TESTING_COMPLETED);
        verify(medicalRecordRepository, never()).save(any(MedicalRecord.class));
    }

    // =========================================================
    // 8) relatedOrders rỗng (edge) → allCompleted = true, record set TESTING_COMPLETED
    // =========================================================
    @Test
    void relatedOrdersEmpty_edge_allCompletedTrue_recordSetTestingCompleted() {
        MedicalRecord r1 = record("R1", MedicalRecordStatus.TESTING);
        MedicalOrder o1 = order("O1", MedicalOrderStatus.PENDING, r1);

        when(medicalOrderRepository.findByIdAndDeletedAtIsNull("O1"))
                .thenReturn(Optional.of(o1));
        when(medicalOrderRepository.save(any(MedicalOrder.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull("R1"))
                .thenReturn(Collections.emptyList());
        when(medicalRecordRepository.save(any(MedicalRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        MedicalOrderStatusResponse resp =
                service.updateMedicalOrderStatus(req("O1", MedicalOrderStatus.COMPLETED));

        assertThat(resp.isAllOrdersCompleted()).isTrue();
        assertThat(resp.getRecordStatus()).isEqualTo(MedicalRecordStatus.TESTING_COMPLETED);
        verify(medicalRecordRepository, times(1)).save(any(MedicalRecord.class));
    }

    // =========================================================
    // 9) Idempotent: order đã COMPLETED, update lại COMPLETED
    // =========================================================
    @Test
    void idempotent_updateCompletedToCompleted_ok() {
        MedicalRecord r1 = record("R1", MedicalRecordStatus.TESTING);
        MedicalOrder o1 = order("O1", MedicalOrderStatus.COMPLETED, r1);
        MedicalOrder o2 = order("O2", MedicalOrderStatus.PENDING, r1);

        when(medicalOrderRepository.findByIdAndDeletedAtIsNull("O1"))
                .thenReturn(Optional.of(o1));
        when(medicalOrderRepository.save(any(MedicalOrder.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull("R1"))
                .thenReturn(List.of(o1, o2)); // chưa all completed

        MedicalOrderStatusResponse resp =
                service.updateMedicalOrderStatus(req("O1", MedicalOrderStatus.COMPLETED));

        assertThat(resp.getOrderStatus()).isEqualTo(MedicalOrderStatus.COMPLETED);
        assertThat(resp.isAllOrdersCompleted()).isFalse();
        verify(medicalOrderRepository, times(1)).save(any(MedicalOrder.class));
        verify(medicalRecordRepository, never()).save(any(MedicalRecord.class));
    }

    // =========================================================
    // 10) Xác thực đầy đủ response mapping (happy-path)
    // =========================================================
    @Test
    void responseMapping_happyPath_allFieldsCorrect() {
        MedicalRecord r1 = record("R-XYZ", MedicalRecordStatus.TESTING);
        MedicalOrder o1 = order("ORD-1", MedicalOrderStatus.PENDING, r1);
        MedicalOrder o2 = order("ORD-2", MedicalOrderStatus.COMPLETED, r1);

        when(medicalOrderRepository.findByIdAndDeletedAtIsNull("ORD-1"))
                .thenReturn(Optional.of(o1));
        when(medicalOrderRepository.save(any(MedicalOrder.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull("R-XYZ"))
                .then(inv -> {
                    o1.setStatus(MedicalOrderStatus.COMPLETED);
                    return List.of(o1, o2);
                });
        when(medicalRecordRepository.save(any(MedicalRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        MedicalOrderStatusResponse resp =
                service.updateMedicalOrderStatus(req("ORD-1", MedicalOrderStatus.COMPLETED));

        assertThat(resp.getMedicalOrderId()).isEqualTo("ORD-1");
        assertThat(resp.getOrderStatus()).isEqualTo(MedicalOrderStatus.COMPLETED);
        assertThat(resp.getMedicalRecordId()).isEqualTo("R-XYZ");
        assertThat(resp.getRecordStatus()).isEqualTo(MedicalRecordStatus.TESTING_COMPLETED);
        assertThat(resp.isAllOrdersCompleted()).isTrue();

        verify(medicalOrderRepository, times(1)).findByIdAndDeletedAtIsNull("ORD-1");
        verify(medicalOrderRepository, times(1)).save(any(MedicalOrder.class));
        verify(medicalOrderRepository, times(1))
                .findAllByMedicalRecordIdAndDeletedAtIsNull("R-XYZ");
        verify(medicalRecordRepository, times(1)).save(any(MedicalRecord.class));
    }


    // 11) Không save record khi allCompleted = true nhưng record đã TESTING_COMPLETED
    @Test
    void noResaveRecord_whenAllCompletedAndRecordAlreadyTestingCompleted_strictVerify() {
        MedicalRecord r1 = record("R1", MedicalRecordStatus.TESTING_COMPLETED);
        MedicalOrder o1 = order("O1", MedicalOrderStatus.COMPLETED, r1);
        MedicalOrder o2 = order("O2", MedicalOrderStatus.COMPLETED, r1);

        when(medicalOrderRepository.findByIdAndDeletedAtIsNull("O1"))
                .thenReturn(Optional.of(o1));
        when(medicalOrderRepository.save(any(MedicalOrder.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull("R1"))
                .thenReturn(List.of(o1, o2));

        var resp = service.updateMedicalOrderStatus(req("O1", MedicalOrderStatus.COMPLETED));

        assertThat(resp.isAllOrdersCompleted()).isTrue();
        assertThat(resp.getRecordStatus()).isEqualTo(MedicalRecordStatus.TESTING_COMPLETED);

        // verify đúng số lần và KHÔNG save record
        verify(medicalOrderRepository, times(1)).findByIdAndDeletedAtIsNull("O1");
        verify(medicalOrderRepository, times(1)).save(any(MedicalOrder.class));
        verify(medicalOrderRepository, times(1)).findAllByMedicalRecordIdAndDeletedAtIsNull("R1");
        verify(medicalRecordRepository, times(0)).save(any(MedicalRecord.class));
    }

    // 12) Verify thứ tự & số lần gọi repository (happy-path record được update 1 lần)
    @Test
    void verifyOrderAndCallCounts_happyPath_recordUpdatedOnce() {
        MedicalRecord r1 = record("R1", MedicalRecordStatus.TESTING);
        MedicalOrder o1 = order("O1", MedicalOrderStatus.PENDING, r1);

        when(medicalOrderRepository.findByIdAndDeletedAtIsNull("O1"))
                .thenReturn(Optional.of(o1));
        when(medicalOrderRepository.save(any(MedicalOrder.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull("R1"))
                .then(inv -> {
                    o1.setStatus(MedicalOrderStatus.COMPLETED);
                    return List.of(o1); // allCompleted = true
                });
        when(medicalRecordRepository.save(any(MedicalRecord.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        var resp = service.updateMedicalOrderStatus(req("O1", MedicalOrderStatus.COMPLETED));
        assertThat(resp.isAllOrdersCompleted()).isTrue();
        assertThat(resp.getRecordStatus()).isEqualTo(MedicalRecordStatus.TESTING_COMPLETED);

        // Số lần
        verify(medicalOrderRepository, times(1)).findByIdAndDeletedAtIsNull("O1");
        verify(medicalOrderRepository, times(1)).save(any(MedicalOrder.class));
        verify(medicalOrderRepository, times(1)).findAllByMedicalRecordIdAndDeletedAtIsNull("R1");
        verify(medicalRecordRepository, times(1)).save(any(MedicalRecord.class));

        // Thứ tự (findById -> save(order) -> findAllByRecord -> save(record))
        InOrder inOrder = inOrder(medicalOrderRepository, medicalRecordRepository);
        inOrder.verify(medicalOrderRepository).findByIdAndDeletedAtIsNull("O1");
        inOrder.verify(medicalOrderRepository).save(any(MedicalOrder.class));
        inOrder.verify(medicalOrderRepository).findAllByMedicalRecordIdAndDeletedAtIsNull("R1");
        inOrder.verify(medicalRecordRepository).save(any(MedicalRecord.class));
    }

    @Test
    void recordTestingCompleted_thenOrderDowngraded_recordStaysTestingCompleted() {
        MedicalRecord r1 = record("R1", MedicalRecordStatus.TESTING_COMPLETED);
        MedicalOrder o1 = order("O1", MedicalOrderStatus.COMPLETED, r1);
        MedicalOrder o2 = order("O2", MedicalOrderStatus.COMPLETED, r1);

        when(medicalOrderRepository.findByIdAndDeletedAtIsNull("O1")).thenReturn(Optional.of(o1));
        when(medicalOrderRepository.save(any())).then(inv -> inv.getArgument(0));
        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull("R1"))
                .thenReturn(List.of(order("O1", MedicalOrderStatus.CANCELLED, r1), o2));

        var resp = service.updateMedicalOrderStatus(req("O1", MedicalOrderStatus.CANCELLED));

        assertThat(resp.isAllOrdersCompleted()).isFalse();
        assertThat(resp.getRecordStatus()).isEqualTo(MedicalRecordStatus.TESTING_COMPLETED);
        verify(medicalRecordRepository, never()).save(any());
    }

    @Test
    void updateToInProgress_allCompletedFalse_recordUnchanged() {
        MedicalRecord r1 = record("R1", MedicalRecordStatus.TESTING);
        MedicalOrder o1 = order("O1", MedicalOrderStatus.PENDING, r1);
        MedicalOrder o2 = order("O2", MedicalOrderStatus.COMPLETED, r1);

        when(medicalOrderRepository.findByIdAndDeletedAtIsNull("O1")).thenReturn(Optional.of(o1));
        when(medicalOrderRepository.save(any())).then(inv -> inv.getArgument(0));
        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull("R1"))
                .thenReturn(List.of(order("O1", MedicalOrderStatus.WAITING, r1), o2));

        var resp = service.updateMedicalOrderStatus(req("O1", MedicalOrderStatus.WAITING));

        assertThat(resp.isAllOrdersCompleted()).isFalse();
        assertThat(resp.getRecordStatus()).isEqualTo(MedicalRecordStatus.TESTING);
        verify(medicalRecordRepository, never()).save(any());
    }


}
