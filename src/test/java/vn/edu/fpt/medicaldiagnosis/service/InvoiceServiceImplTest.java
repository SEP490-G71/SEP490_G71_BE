package vn.edu.fpt.medicaldiagnosis.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import vn.edu.fpt.medicaldiagnosis.dto.request.PayInvoiceRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.UpdateInvoiceRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.InvoiceDetailResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.InvoiceItemResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.InvoiceResponse;
import vn.edu.fpt.medicaldiagnosis.entity.*;
import vn.edu.fpt.medicaldiagnosis.enums.*;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.InvoiceMapper;
import vn.edu.fpt.medicaldiagnosis.repository.*;
import vn.edu.fpt.medicaldiagnosis.service.impl.InvoiceServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class InvoiceServiceImplTest {
    @Mock
    InvoiceRepository invoiceRepository;
    @Mock
    MedicalOrderRepository medicalOrderRepository;
    @Mock
    MedicalRecordRepository medicalRecordRepository;
    @Mock
    StaffRepository staffRepository;
    @Mock
    InvoiceMapper invoiceMapper;
    @Mock
    InvoiceItemRepository invoiceItemRepository;
    @Mock MedicalServiceRepository medicalServiceRepository;
    @Mock SettingService settingService;
    @Mock WorkScheduleService workScheduleService;

    @InjectMocks
    InvoiceServiceImpl service;


    // ======== Helpers ========
    private PayInvoiceRequest reqPay(String invoiceId, String staffId, PaymentType paymentType) {
        PayInvoiceRequest r = new PayInvoiceRequest();
        r.setInvoiceId(invoiceId);
        r.setStaffId(staffId);
        r.setPaymentType(paymentType);
        return r;
    }


    private Invoice invoice(String id, InvoiceStatus status) {
        Invoice inv = new Invoice();
        inv.setId(id);
        inv.setStatus(status);
        inv.setOriginalTotal(BigDecimal.ZERO);
        inv.setDiscountTotal(BigDecimal.ZERO);
        inv.setVatTotal(BigDecimal.ZERO);
        inv.setTotal(BigDecimal.ZERO);
        return inv;
    }

    private Staff staff(String id) {
        Staff s = new Staff();
        s.setId(id);
        return s;
    }

    private MedicalRecord record(String id, MedicalRecordStatus status) {
        MedicalRecord r = new MedicalRecord();
        r.setId(id);
        r.setStatus(status);
        return r;
    }

    private MedicalOrder order(String id, MedicalRecord rec, MedicalOrderStatus status) {
        MedicalOrder o = new MedicalOrder();
        o.setId(id);
        o.setMedicalRecord(rec);
        o.setStatus(status);
        return o;
    }

    private MedicalService svc(String id, String code, String name, String price, String discountPct, String vatPct) {
        MedicalService s = new MedicalService();
        s.setId(id);
        s.setServiceCode(code);
        s.setName(name);
        s.setPrice(new BigDecimal(price));
        s.setDiscount(discountPct == null ? null : new BigDecimal(discountPct));
        s.setVat(vatPct == null ? null : new BigDecimal(vatPct));
        return s;
    }


    private MedicalRecord record(String id) {
        MedicalRecord r = new MedicalRecord();
        r.setId(id);
        return r;
    }

    private InvoiceItem item(String id, Invoice inv, MedicalService s, int qty, String total) {
        InvoiceItem it = new InvoiceItem();
        it.setId(id);
        it.setInvoice(inv);
        it.setService(s);
        it.setServiceCode(s.getServiceCode());
        it.setName(s.getName());
        it.setQuantity(qty);
        it.setPrice(s.getPrice());
        it.setDiscount(s.getDiscount());
        it.setVat(s.getVat());
        it.setTotal(total == null ? null : new BigDecimal(total));
        return it;
    }

    private InvoiceResponse resp(String id) {
        InvoiceResponse r = new InvoiceResponse();
        r.setInvoiceId(id);
        return r;
    }



    private void stubMapperEchoId() {
        lenient().when(invoiceMapper.toInvoiceResponse(any(Invoice.class))).thenAnswer(a -> {
            Invoice inv = a.getArgument(0);
            return resp(inv.getId());
        });
    }

    private MedicalOrder order(String id, MedicalRecord r, MedicalService s, InvoiceItem it, MedicalOrderStatus st) {
        MedicalOrder o = new MedicalOrder();
        o.setId(id);
        o.setMedicalRecord(r);
        o.setService(s);
        o.setInvoiceItem(it);
        o.setStatus(st);
        return o;
    }

    private UpdateInvoiceRequest req(String invoiceId, String staffId, UpdateInvoiceRequest.InvoiceItemUpdateRequest... items) {
        UpdateInvoiceRequest r = new UpdateInvoiceRequest();
        r.setInvoiceId(invoiceId);
        r.setStaffId(staffId);
        r.setServices(Arrays.asList(items));
        return r;
    }


    private UpdateInvoiceRequest.InvoiceItemUpdateRequest itemReq(String serviceId, int qty) {
        UpdateInvoiceRequest.InvoiceItemUpdateRequest ir = new UpdateInvoiceRequest.InvoiceItemUpdateRequest();
        ir.setServiceId(serviceId);
        ir.setQuantity(qty);
        return ir;
    }

    private void stubCommonSuccess(Invoice inv, Staff st) {
        when(invoiceRepository.findByIdAndDeletedAtIsNull(inv.getId())).thenReturn(Optional.of(inv));
        when(staffRepository.findByIdAndDeletedAtIsNull(st.getId())).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow(st.getId())).thenReturn(true);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(a -> a.getArgument(0));
        when(invoiceMapper.toInvoiceResponse(any(Invoice.class))).thenAnswer(a -> {
            Invoice x = a.getArgument(0);
            InvoiceResponse res = new InvoiceResponse();
            res.setInvoiceId(x.getId());
            return res;
        });
    }

    private void stubService(MedicalService s) {
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull(s.getId())).thenReturn(Optional.of(s));
    }

    @BeforeEach
    void lenientDefault() {
        // Tránh UnnecessaryStubbing khi case không đụng tới
        lenient().when(medicalOrderRepository.save(any(MedicalOrder.class))).thenAnswer(a -> {
            MedicalOrder o = a.getArgument(0);
            if (o.getId() == null) o.setId(UUID.randomUUID().toString());
            return o;
        });
        lenient().when(invoiceItemRepository.save(any(InvoiceItem.class))).thenAnswer(a -> {
            InvoiceItem it = a.getArgument(0);
            if (it.getId() == null) it.setId(UUID.randomUUID().toString());
            return it;
        });
    }


    private Patient patient(String id, String code, String name, LocalDate dob, Gender gender, String phone) {
        Patient p = new Patient();
        p.setId(id);
        p.setPatientCode(code);
        p.setFullName(name);
        p.setDob(dob);
        p.setGender(gender);
        p.setPhone(phone);
        return p;
    }

    private Staff staff(String id, String fullName) {
        Staff s = new Staff();
        s.setId(id);
        s.setFullName(fullName);
        return s;
    }

    private Invoice invoice(String id, String code, Patient patient, InvoiceStatus status, PaymentType pt,
                            String original, String discount, String vat, String total,
                            LocalDateTime createdAt, LocalDateTime confirmedAt, Staff confirmedBy, String desc) {
        Invoice inv = new Invoice();
        inv.setId(id);
        inv.setInvoiceCode(code);
        inv.setPatient(patient);
        inv.setStatus(status);
        inv.setPaymentType(pt);
        inv.setOriginalTotal(new BigDecimal(original));
        inv.setDiscountTotal(new BigDecimal(discount));
        inv.setVatTotal(new BigDecimal(vat));
        inv.setTotal(new BigDecimal(total));
        inv.setCreatedAt(createdAt);
        inv.setConfirmedAt(confirmedAt);
        inv.setConfirmedBy(confirmedBy);
        inv.setDescription(desc);
        return inv;
    }


    // =========================================================
    // A) Guard clauses & lỗi đầu vào (1–7)
    // =========================================================

    /** 1) Invoice không tồn tại → INVOICE_NOT_FOUND */
    @Test
    void payInvoice_invoiceNotFound_throws() {
        when(invoiceRepository.findByIdAndDeletedAtIsNull("INV404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.payInvoice(reqPay("INV404", "ST1", PaymentType.TRANSFER)))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVOICE_NOT_FOUND);

        verifyNoInteractions(staffRepository, workScheduleService, medicalOrderRepository, medicalRecordRepository);
    }

    /** 2) Invoice đã PAID → INVOICE_ALREADY_PAID */
    @Test
    void payInvoice_invoiceAlreadyPaid_throws() {
        Invoice inv = invoice("INV1", InvoiceStatus.PAID);
        when(invoiceRepository.findByIdAndDeletedAtIsNull("INV1")).thenReturn(Optional.of(inv));

        assertThatThrownBy(() -> service.payInvoice(reqPay("INV1", "ST1", PaymentType.TRANSFER)))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVOICE_ALREADY_PAID);

        verify(staffRepository, never()).findByIdAndDeletedAtIsNull(anyString());
        verify(medicalOrderRepository, never()).findAllByInvoiceItemInvoiceId(anyString());
        verify(medicalRecordRepository, never()).save(any());
    }

    /** 3) Staff không tồn tại → STAFF_NOT_FOUND */
    @Test
    void payInvoice_staffNotFound_throws() {
        Invoice inv = invoice("INV1", InvoiceStatus.UNPAID);
        when(invoiceRepository.findByIdAndDeletedAtIsNull("INV1")).thenReturn(Optional.of(inv));
        when(staffRepository.findByIdAndDeletedAtIsNull("ST404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.payInvoice(reqPay("INV1", "ST404", PaymentType.TRANSFER)))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.STAFF_NOT_FOUND);

        verify(workScheduleService, never()).isStaffOnShiftNow(anyString());
        verify(medicalOrderRepository, never()).findAllByInvoiceItemInvoiceId(anyString());
    }

    /** 4) Staff không trong ca → ACTION_NOT_ALLOWED */
    @Test
    void payInvoice_staffNotOnShift_throws() {
        Invoice inv = invoice("INV1", InvoiceStatus.UNPAID);
        Staff st = staff("ST1");
        when(invoiceRepository.findByIdAndDeletedAtIsNull("INV1")).thenReturn(Optional.of(inv));
        when(staffRepository.findByIdAndDeletedAtIsNull("ST1")).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("ST1")).thenReturn(false);

        assertThatThrownBy(() -> service.payInvoice(reqPay("INV1", "ST1", PaymentType.TRANSFER)))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACTION_NOT_ALLOWED);

        verify(medicalOrderRepository, never()).findAllByInvoiceItemInvoiceId(anyString());
        verify(invoiceRepository, never()).save(inv); // không cập nhật gì
    }

    /** 5) Không có order liên quan → MEDICAL_RECORD_NOT_FOUND */
    @Test
    void payInvoice_noOrders_relatedRecordNotFound_throws() {
        Invoice inv = invoice("INV1", InvoiceStatus.UNPAID);
        Staff st = staff("ST1");
        when(invoiceRepository.findByIdAndDeletedAtIsNull("INV1")).thenReturn(Optional.of(inv));
        when(staffRepository.findByIdAndDeletedAtIsNull("ST1")).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("ST1")).thenReturn(true);

        when(medicalOrderRepository.findAllByInvoiceItemInvoiceId("INV1")).thenReturn(List.of());

        assertThatThrownBy(() -> service.payInvoice(reqPay("INV1", "ST1", PaymentType.TRANSFER)))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.MEDICAL_RECORD_NOT_FOUND);

        // Đã save invoice trước khi phát hiện? Code set PAID trước khi lấy orders → verify đã save invoice
        // Nếu bạn muốn đảm bảo KHÔNG save trước, hãy điều chỉnh theo implementation thực tế.
    }

    /** 6) Nhiều medical record khác nhau → MULTIPLE_MEDICAL_RECORDS_FOUND */
    @Test
    void payInvoice_multipleRecordsFound_throws() {
        Invoice inv = invoice("INV1", InvoiceStatus.UNPAID);
        Staff st = staff("ST1");
        when(invoiceRepository.findByIdAndDeletedAtIsNull("INV1")).thenReturn(Optional.of(inv));
        when(staffRepository.findByIdAndDeletedAtIsNull("ST1")).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("ST1")).thenReturn(true);

        MedicalRecord r1 = record("R1", MedicalRecordStatus.WAITING_FOR_PAYMENT);
        MedicalRecord r2 = record("R2", MedicalRecordStatus.WAITING_FOR_PAYMENT);
        MedicalOrder o1 = order("O1", r1, MedicalOrderStatus.PENDING);
        MedicalOrder o2 = order("O2", r2, MedicalOrderStatus.PENDING);

        when(medicalOrderRepository.findAllByInvoiceItemInvoiceId("INV1"))
                .thenReturn(List.of(o1, o2));

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(a -> a.getArgument(0));
        when(medicalOrderRepository.saveAll(any())).thenAnswer(a -> a.getArgument(0));

        assertThatThrownBy(() -> service.payInvoice(reqPay("INV1", "ST1", PaymentType.TRANSFER)))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.MULTIPLE_MEDICAL_RECORDS_FOUND);

        // Capture Iterable
        ArgumentCaptor<Iterable<MedicalOrder>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(medicalOrderRepository).saveAll(captor.capture());

        List<MedicalOrder> saved = new ArrayList<>();
        captor.getValue().forEach(saved::add);

        assertThat(saved).hasSize(2);
        assertThat(saved).allMatch(o -> o.getStatus() == MedicalOrderStatus.WAITING);

        // Record không bị save
        verify(medicalRecordRepository, never()).save(any());
        // Mapper cũng không được gọi
        verify(invoiceMapper, never()).toInvoiceResponse(any());
    }



    /** 7) Tham số null (invoiceId hoặc staffId) → rơi vào NotFound tương ứng */
    @Test
    void payInvoice_nullInvoiceId_behavesAsNotFound() {
        when(invoiceRepository.findByIdAndDeletedAtIsNull(null)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.payInvoice(reqPay(null, "ST1", PaymentType.TRANSFER)))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVOICE_NOT_FOUND);
    }

    // =========================================================
    // B) Luồng thành công & cập nhật trạng thái (8–13)
    // =========================================================

    /** 8) 1 order PENDING → chuyển WAITING; record → TESTING; invoice → PAID */
    @Test
    void payInvoice_singlePendingOrder_updatesCorrectly() {
        Invoice inv = invoice("INV1", InvoiceStatus.UNPAID);
        Staff st = staff("ST1");
        when(invoiceRepository.findByIdAndDeletedAtIsNull("INV1")).thenReturn(Optional.of(inv));
        when(staffRepository.findByIdAndDeletedAtIsNull("ST1")).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("ST1")).thenReturn(true);

        MedicalRecord r = record("R1", MedicalRecordStatus.WAITING_FOR_PAYMENT);
        MedicalOrder o = order("O1", r, MedicalOrderStatus.PENDING);
        when(medicalOrderRepository.findAllByInvoiceItemInvoiceId("INV1")).thenReturn(List.of(o));

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(a -> a.getArgument(0));
        when(medicalOrderRepository.saveAll(anyList())).thenAnswer(a -> a.getArgument(0));
        when(medicalRecordRepository.save(any(MedicalRecord.class))).thenAnswer(a -> a.getArgument(0));
        when(invoiceMapper.toInvoiceResponse(any(Invoice.class))).thenReturn(new InvoiceResponse());

        InvoiceResponse resp = service.payInvoice(reqPay("INV1", "ST1", PaymentType.TRANSFER));
        assertThat(resp).isNotNull();
        assertThat(inv.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(o.getStatus()).isEqualTo(MedicalOrderStatus.WAITING);
        assertThat(r.getStatus()).isEqualTo(MedicalRecordStatus.TESTING);

        verify(medicalOrderRepository).saveAll(argThat(iter -> {
            List<MedicalOrder> list = new ArrayList<>();
            iter.forEach(list::add);
            return list.size() == 1 && list.get(0).getStatus() == MedicalOrderStatus.WAITING;
        }));

        verify(medicalRecordRepository).save(argThat(saved -> saved.getStatus() == MedicalRecordStatus.TESTING));
        verify(invoiceMapper).toInvoiceResponse(inv);
    }

    /** 9) 1 order đã WAITING (không phải PENDING) → order giữ nguyên; record TESTING; invoice PAID */
    @Test
    void payInvoice_singleNonPendingOrder_keepsStatus() {
        Invoice inv = invoice("INV1", InvoiceStatus.UNPAID);
        Staff st = staff("ST1");
        when(invoiceRepository.findByIdAndDeletedAtIsNull("INV1")).thenReturn(Optional.of(inv));
        when(staffRepository.findByIdAndDeletedAtIsNull("ST1")).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("ST1")).thenReturn(true);

        MedicalRecord r = record("R1", MedicalRecordStatus.WAITING_FOR_PAYMENT);
        MedicalOrder o = order("O1", r, MedicalOrderStatus.WAITING);
        when(medicalOrderRepository.findAllByInvoiceItemInvoiceId("INV1")).thenReturn(List.of(o));

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(a -> a.getArgument(0));
        when(medicalOrderRepository.saveAll(anyList())).thenAnswer(a -> a.getArgument(0));
        when(medicalRecordRepository.save(any(MedicalRecord.class))).thenAnswer(a -> a.getArgument(0));
        when(invoiceMapper.toInvoiceResponse(any(Invoice.class))).thenReturn(new InvoiceResponse());

        service.payInvoice(reqPay("INV1", "ST1", PaymentType.TRANSFER));

        assertThat(inv.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(o.getStatus()).isEqualTo(MedicalOrderStatus.WAITING); // giữ nguyên
        assertThat(r.getStatus()).isEqualTo(MedicalRecordStatus.TESTING);

        verify(medicalOrderRepository).saveAll(argThat(iter -> {
            List<MedicalOrder> list = new ArrayList<>();
            iter.forEach(list::add);
            return list.size() == 1 && list.get(0).getStatus() == MedicalOrderStatus.WAITING;
        }));

    }

    /** 10) Nhiều order hỗn hợp → chỉ PENDING đổi WAITING; record TESTING; invoice PAID */
    @Test
    void payInvoice_multipleOrders_mixedStatuses_updatesOnlyPending() {
        Invoice inv = invoice("INV1", InvoiceStatus.UNPAID);
        Staff st = staff("ST1");
        when(invoiceRepository.findByIdAndDeletedAtIsNull("INV1")).thenReturn(Optional.of(inv));
        when(staffRepository.findByIdAndDeletedAtIsNull("ST1")).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("ST1")).thenReturn(true);

        MedicalRecord r = record("R1", MedicalRecordStatus.WAITING_FOR_PAYMENT);
        MedicalOrder o1 = order("O1", r, MedicalOrderStatus.PENDING);
        MedicalOrder o2 = order("O2", r, MedicalOrderStatus.WAITING);
        MedicalOrder o3 = order("O3", r, MedicalOrderStatus.COMPLETED);
        when(medicalOrderRepository.findAllByInvoiceItemInvoiceId("INV1")).thenReturn(List.of(o1, o2, o3));

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(a -> a.getArgument(0));
        when(medicalOrderRepository.saveAll(anyList())).thenAnswer(a -> a.getArgument(0));
        when(medicalRecordRepository.save(any(MedicalRecord.class))).thenAnswer(a -> a.getArgument(0));
        when(invoiceMapper.toInvoiceResponse(any(Invoice.class))).thenReturn(new InvoiceResponse());

        // Act
        service.payInvoice(reqPay("INV1", "ST1", PaymentType.TRANSFER));

        // Assert trạng thái trên object trong bộ nhớ
        assertThat(inv.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(o1.getStatus()).isEqualTo(MedicalOrderStatus.WAITING);     // PENDING -> WAITING
        assertThat(o2.getStatus()).isEqualTo(MedicalOrderStatus.WAITING);     // giữ nguyên
        assertThat(o3.getStatus()).isEqualTo(MedicalOrderStatus.COMPLETED);   // giữ nguyên
        assertThat(r.getStatus()).isEqualTo(MedicalRecordStatus.TESTING);

        // Verify saveAll được gọi với đủ 3 order và trạng thái đúng
        ArgumentCaptor<Iterable<MedicalOrder>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(medicalOrderRepository).saveAll(captor.capture());

        List<MedicalOrder> saved = new ArrayList<>();
        captor.getValue().forEach(saved::add);

        assertThat(saved).hasSize(3);
        // map id -> status để check gọn
        Map<String, MedicalOrderStatus> byId = saved.stream()
                .collect(Collectors.toMap(MedicalOrder::getId, MedicalOrder::getStatus));

        assertThat(byId.get("O1")).isEqualTo(MedicalOrderStatus.WAITING);
        assertThat(byId.get("O2")).isEqualTo(MedicalOrderStatus.WAITING);
        assertThat(byId.get("O3")).isEqualTo(MedicalOrderStatus.COMPLETED);
    }


    /** 11) Nhiều order nhưng chung 1 record → hợp lệ, record TESTING */
    @Test
    void payInvoice_multipleOrdersSameRecord_ok() {
        Invoice inv = invoice("INV1", InvoiceStatus.UNPAID);
        Staff st = staff("ST1");
        when(invoiceRepository.findByIdAndDeletedAtIsNull("INV1")).thenReturn(Optional.of(inv));
        when(staffRepository.findByIdAndDeletedAtIsNull("ST1")).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("ST1")).thenReturn(true);

        MedicalRecord r = record("R1", MedicalRecordStatus.WAITING_FOR_PAYMENT);
        MedicalOrder o1 = order("O1", r, MedicalOrderStatus.PENDING);
        MedicalOrder o2 = order("O2", r, MedicalOrderStatus.PENDING);
        when(medicalOrderRepository.findAllByInvoiceItemInvoiceId("INV1")).thenReturn(List.of(o1, o2));

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(a -> a.getArgument(0));
        when(medicalOrderRepository.saveAll(anyList())).thenAnswer(a -> a.getArgument(0));
        when(medicalRecordRepository.save(any(MedicalRecord.class))).thenAnswer(a -> a.getArgument(0));
        when(invoiceMapper.toInvoiceResponse(any(Invoice.class))).thenReturn(new InvoiceResponse());

        service.payInvoice(reqPay("INV1", "ST1", PaymentType.TRANSFER));

        assertThat(inv.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(o1.getStatus()).isEqualTo(MedicalOrderStatus.WAITING);
        assertThat(o2.getStatus()).isEqualTo(MedicalOrderStatus.WAITING);
        assertThat(r.getStatus()).isEqualTo(MedicalRecordStatus.TESTING);
    }

    /** 12) paymentType = null → vẫn PAID, confirmedAt/confirmedBy set */
    @Test
    void payInvoice_nullPaymentType_stillPaidAndConfirmed() {
        Invoice inv = invoice("INV1", InvoiceStatus.UNPAID);
        Staff st = staff("ST1");
        when(invoiceRepository.findByIdAndDeletedAtIsNull("INV1")).thenReturn(Optional.of(inv));
        when(staffRepository.findByIdAndDeletedAtIsNull("ST1")).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("ST1")).thenReturn(true);

        MedicalRecord r = record("R1", MedicalRecordStatus.WAITING_FOR_PAYMENT);
        MedicalOrder o = order("O1", r, MedicalOrderStatus.PENDING);
        when(medicalOrderRepository.findAllByInvoiceItemInvoiceId("INV1")).thenReturn(List.of(o));

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(a -> a.getArgument(0));
        when(medicalOrderRepository.saveAll(anyList())).thenAnswer(a -> a.getArgument(0));
        when(medicalRecordRepository.save(any(MedicalRecord.class))).thenAnswer(a -> a.getArgument(0));
        when(invoiceMapper.toInvoiceResponse(any(Invoice.class))).thenReturn(new InvoiceResponse());

        service.payInvoice(reqPay("INV1", "ST1", null));

        assertThat(inv.getPaymentType()).isNull();
        assertThat(inv.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(inv.getConfirmedBy()).isEqualTo(st);
        assertThat(inv.getConfirmedAt()).isNotNull();
    }

    /** 13) Kiểm tra field cập nhật của invoice (status, paymentType, confirmedBy/At) */
    @Test
    void payInvoice_updatesInvoiceFieldsCorrectly() {
        Invoice inv = invoice("INV1", InvoiceStatus.UNPAID);
        Staff st = staff("ST1");
        when(invoiceRepository.findByIdAndDeletedAtIsNull("INV1")).thenReturn(Optional.of(inv));
        when(staffRepository.findByIdAndDeletedAtIsNull("ST1")).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("ST1")).thenReturn(true);

        MedicalRecord r = record("R1", MedicalRecordStatus.WAITING_FOR_PAYMENT);
        MedicalOrder o = order("O1", r, MedicalOrderStatus.PENDING);
        when(medicalOrderRepository.findAllByInvoiceItemInvoiceId("INV1")).thenReturn(List.of(o));

        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(a -> a.getArgument(0));
        when(medicalOrderRepository.saveAll(anyList())).thenAnswer(a -> a.getArgument(0));
        when(medicalRecordRepository.save(any(MedicalRecord.class))).thenAnswer(a -> a.getArgument(0));
        when(invoiceMapper.toInvoiceResponse(any(Invoice.class))).thenReturn(new InvoiceResponse());

        LocalDateTime before = LocalDateTime.now();

        // truyền enum vào request (đổi helper req(...) nếu cần)
        service.payInvoice(reqPay("INV1", "ST1", PaymentType.TRANSFER));

        assertThat(inv.getStatus()).isEqualTo(InvoiceStatus.PAID);
        assertThat(inv.getPaymentType()).isEqualTo(PaymentType.TRANSFER); // so sánh enum
        assertThat(inv.getConfirmedBy()).isEqualTo(st);
        assertThat(inv.getConfirmedAt()).isNotNull();
        assertThat(inv.getConfirmedAt()).isAfterOrEqualTo(before);

        verify(invoiceMapper).toInvoiceResponse(inv);
    }

    // =========================
    // 1) sortBy = null, sortDir = "desc" → dùng createdAt DESC
    // =========================
    @Test
    void getInvoicesPaged_sortByNull_desc() {
        stubMapperEchoId();

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(invoiceRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> {
                    Pageable p = inv.getArgument(1);
                    return new PageImpl<>(List.of(invoice("I1", InvoiceStatus.UNPAID)), p, 1);
                });

        Page<InvoiceResponse> page = service.getInvoicesPaged(Map.of(), 0, 10, null, "desc");

        verify(invoiceRepository).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable used = pageableCaptor.getValue();
        assertThat(used.getSort()).isEqualTo(Sort.by("createdAt").descending());
        assertThat(page.getContent()).extracting(InvoiceResponse::getInvoiceId).containsExactly("I1");
    }

    // =========================
    // 2) sortBy = null, sortDir = "asc" → createdAt ASC
    // =========================
    @Test
    void getInvoicesPaged_sortByNull_asc() {
        stubMapperEchoId();

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(invoiceRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> {
                    Pageable p = inv.getArgument(1);
                    return new PageImpl<>(List.of(), p, 0);
                });

        service.getInvoicesPaged(Map.of(), 1, 5, null, "asc");

        verify(invoiceRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort()).isEqualTo(Sort.by("createdAt").ascending());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
    }

    // =========================
    // 3) sortBy = "" (blank), sortDir = "asc" → createdAt ASC
    // =========================
    @Test
    void getInvoicesPaged_sortByBlank_asc() {
        stubMapperEchoId();
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(invoiceRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        service.getInvoicesPaged(Map.of(), 0, 10, "  ", "asc");

        verify(invoiceRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort()).isEqualTo(Sort.by("createdAt").ascending());
    }

    // =========================
    // 4) sortBy = "totalAmount", sortDir = "ASC" (hoa) → totalAmount ASC
    // =========================
    @Test
    void getInvoicesPaged_sortByCustom_upperAsc() {
        stubMapperEchoId();
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(invoiceRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        service.getInvoicesPaged(Map.of(), 0, 10, "totalAmount", "ASC");

        verify(invoiceRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort()).isEqualTo(Sort.by("totalAmount").ascending());
    }

    // =========================
    // 5) sortDir bất kỳ khác "asc" → DESC
    // =========================
    @Test
    void getInvoicesPaged_sortDirOther_descFallback() {
        stubMapperEchoId();
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(invoiceRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        service.getInvoicesPaged(Map.of(), 0, 10, "totalAmount", "foo");

        verify(invoiceRepository).findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort()).isEqualTo(Sort.by("totalAmount").descending());
    }

    // =========================
    // 6) sortDir = null → NullPointerException (do gọi equalsIgnoreCase trên null)
    // =========================
    @Test
    void getInvoicesPaged_sortDirNull_throwsNpe() {
        assertThatThrownBy(() -> service.getInvoicesPaged(Map.of(), 0, 10, "createdAt", null))
                .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(invoiceRepository);
    }

    // =========================
    // 7) page = 0, size = 10 → Pageable chuẩn
    // =========================
    @Test
    void getInvoicesPaged_basicPageable_ok() {
        stubMapperEchoId();
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);

        when(invoiceRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenAnswer(inv -> {
                    Pageable p = inv.getArgument(1);
                    return new PageImpl<>(List.of(invoice("I1", InvoiceStatus.UNPAID)), p, 1);
                });

        Page<InvoiceResponse> page = service.getInvoicesPaged(Map.of(), 0, 10, "createdAt", "asc");

        verify(invoiceRepository).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable used = pageableCaptor.getValue();
        assertThat(used.getPageNumber()).isEqualTo(0);
        assertThat(used.getPageSize()).isEqualTo(10);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent()).hasSize(1);
    }

    // =========================
    // 8) page < 0 → IllegalArgumentException từ PageRequest
    // =========================
    @Test
    void getInvoicesPaged_pageNegative_illegalArgument() {
        assertThatThrownBy(() -> service.getInvoicesPaged(Map.of(), -1, 10, "createdAt", "asc"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(invoiceRepository);
    }

    // =========================
    // 9) size = 0 → IllegalArgumentException
    // =========================
    @Test
    void getInvoicesPaged_sizeZero_illegalArgument() {
        assertThatThrownBy(() -> service.getInvoicesPaged(Map.of(), 0, 0, "createdAt", "asc"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(invoiceRepository);
    }

    // =========================
    // 10) size < 0 → IllegalArgumentException
    // =========================
    @Test
    void getInvoicesPaged_sizeNegative_illegalArgument() {
        assertThatThrownBy(() -> service.getInvoicesPaged(Map.of(), 0, -5, "createdAt", "asc"))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(invoiceRepository);
    }

    // =========================
    // 11) filters rỗng → vẫn gọi repo với spec, pageable
    // =========================
    @Test
    void getInvoicesPaged_emptyFilters_stillQueries() {
        stubMapperEchoId();
        when(invoiceRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        Page<InvoiceResponse> page = service.getInvoicesPaged(Collections.emptyMap(), 0, 10, null, "asc");

        assertThat(page).isNotNull();
        verify(invoiceRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    // =========================
    // 12) filters có giá trị → gọi repo với spec & pageable (không test logic spec)
    // =========================
    @Test
    void getInvoicesPaged_withFilters_queriesWithSpec() {
        stubMapperEchoId();
        Map<String, String> filters = new HashMap<>();
        filters.put("status", "PAID");
        filters.put("patientName", "Alice");

        when(invoiceRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 20), 0));

        Page<InvoiceResponse> page = service.getInvoicesPaged(filters, 2, 20, "createdAt", "desc");

        assertThat(page.getTotalElements()).isEqualTo(0);
        verify(invoiceRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    // =========================
    // 13) Repo trả empty Page → map rỗng, metadata giữ nguyên
    // =========================
    @Test
    void getInvoicesPaged_emptyPage_resultEmpty() {
        stubMapperEchoId();

        Pageable pageable = PageRequest.of(1, 3, Sort.by("createdAt").descending());
        when(invoiceRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<InvoiceResponse> page = service.getInvoicesPaged(Map.of(), 1, 3, null, "desc");

        assertThat(page.getContent()).isEmpty();
        assertThat(page.getTotalElements()).isEqualTo(0);
        assertThat(page.getTotalPages()).isEqualTo(0);
        assertThat(page.getNumber()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(3);
    }

    // =========================
    // 14) Repo trả Page có dữ liệu → mapper gọi đúng số lần, metadata bảo toàn
    // =========================
    @Test
    void getInvoicesPaged_nonEmptyPage_mapsAndKeepsMetadata() {
        // stub mapper cụ thể để verify số lần
        when(invoiceMapper.toInvoiceResponse(any(Invoice.class))).thenAnswer(a -> {
            Invoice inv = a.getArgument(0);
            InvoiceResponse r = new InvoiceResponse();
            r.setInvoiceId("R-" + inv.getId());
            return r;
        });

        Pageable pageable = PageRequest.of(0, 2, Sort.by("totalAmount").ascending());
        List<Invoice> invoices = List.of(
                invoice("I1", InvoiceStatus.UNPAID),
                invoice("I2", InvoiceStatus.PAID)
        );

        when(invoiceRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(invoices, pageable, 5)); // totalElements=5 (giả định)

        Page<InvoiceResponse> page = service.getInvoicesPaged(Map.of(), 0, 2, "totalAmount", "asc");

        assertThat(page.getContent()).extracting(InvoiceResponse::getInvoiceId)
                .containsExactly("R-I1", "R-I2");
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getTotalPages()).isEqualTo(3); // ceil(5/2)=3
        assertThat(page.getNumber()).isEqualTo(0);
        assertThat(page.getSize()).isEqualTo(2);

        verify(invoiceMapper, times(2)).toInvoiceResponse(any(Invoice.class));
        verify(invoiceRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    // =========================
    // A) Guard clauses (1–6)
    // =========================

    /** 1) Invoice không tồn tại */
    @Test
    void updateInvoiceItems_invoiceNotFound_throws() {
        UpdateInvoiceRequest rq = req("INV404", "ST1", itemReq("SVC1", 1));
        when(invoiceRepository.findByIdAndDeletedAtIsNull("INV404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateInvoiceItems(rq))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVOICE_NOT_FOUND);

        verifyNoInteractions(staffRepository, medicalServiceRepository, invoiceItemRepository, medicalOrderRepository, invoiceMapper);
    }

    /** 2) Invoice đã PAID */
    @Test
    void updateInvoiceItems_invoiceAlreadyPaid_throws() {
        Invoice inv = invoice("INV1", InvoiceStatus.PAID);
        when(invoiceRepository.findByIdAndDeletedAtIsNull("INV1")).thenReturn(Optional.of(inv));

        UpdateInvoiceRequest rq = req("INV1", "ST1", itemReq("SVC1", 1));
        assertThatThrownBy(() -> service.updateInvoiceItems(rq))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVOICE_ALREADY_PAID);

        verify(staffRepository, never()).findByIdAndDeletedAtIsNull(anyString());
    }

    /** 3) Staff không tồn tại */
    @Test
    void updateInvoiceItems_staffNotFound_throws() {
        Invoice inv = invoice("INV1", InvoiceStatus.UNPAID);
        when(invoiceRepository.findByIdAndDeletedAtIsNull("INV1")).thenReturn(Optional.of(inv));
        when(staffRepository.findByIdAndDeletedAtIsNull("ST404")).thenReturn(Optional.empty());

        UpdateInvoiceRequest rq = req("INV1", "ST404", itemReq("SVC1", 1));
        assertThatThrownBy(() -> service.updateInvoiceItems(rq))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.STAFF_NOT_FOUND);

        verify(workScheduleService, never()).isStaffOnShiftNow(anyString());
    }

    /** 4) Staff không trong ca */
    @Test
    void updateInvoiceItems_staffNotOnShift_throws() {
        Invoice inv = invoice("INV1", InvoiceStatus.UNPAID);
        Staff st = staff("ST1");
        when(invoiceRepository.findByIdAndDeletedAtIsNull("INV1")).thenReturn(Optional.of(inv));
        when(staffRepository.findByIdAndDeletedAtIsNull("ST1")).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("ST1")).thenReturn(false);

        UpdateInvoiceRequest rq = req("INV1", "ST1", itemReq("SVC1", 1));
        assertThatThrownBy(() -> service.updateInvoiceItems(rq))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.ACTION_NOT_ALLOWED);

        verifyNoInteractions(medicalServiceRepository, invoiceItemRepository, medicalOrderRepository, invoiceMapper);
    }

    /** 5) Service trong request không tồn tại */
    @Test
    void updateInvoiceItems_serviceNotFound_throws() {
        // Arrange
        Invoice inv = invoice("INV1", InvoiceStatus.UNPAID);
        Staff st = staff("ST1");

        // ❌ Không dùng stubCommonSuccess(...) để tránh stub thừa save/map
        when(invoiceRepository.findByIdAndDeletedAtIsNull("INV1")).thenReturn(Optional.of(inv));
        when(staffRepository.findByIdAndDeletedAtIsNull("ST1")).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("ST1")).thenReturn(true);

        // service hiện tại & orders hiện tại (có thể rỗng)
        when(invoiceItemRepository.findAllByInvoiceId("INV1")).thenReturn(List.of());
        when(medicalOrderRepository.findAllByInvoiceItemIdIn(List.of())).thenReturn(List.of());

        // Gây lỗi: service trong request không tồn tại
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SVC404"))
                .thenReturn(Optional.empty());

        UpdateInvoiceRequest rq = req("INV1", "ST1", itemReq("SVC404", 2));

        // Act + Assert
        assertThatThrownBy(() -> service.updateInvoiceItems(rq))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.MEDICAL_SERVICE_NOT_FOUND);

        // (tuỳ chọn) verify không gọi save/map
        verify(invoiceRepository, never()).save(any());
        verify(invoiceMapper, never()).toInvoiceResponse(any());
    }


    /** 6) services rỗng → xoá tất cả items & orders, totals=0 */
    @Test
    void updateInvoiceItems_emptyServices_deletesAllAndTotalsZero() {
        Invoice inv = invoice("INV1", InvoiceStatus.UNPAID);
        Staff st = staff("ST1");
        stubCommonSuccess(inv, st);

        // current: 2 items mỗi item có 2/3 orders
        MedicalService s1 = svc("S1", "C1", "Name1", "100", "0", "0");
        MedicalService s2 = svc("S2", "C2", "Name2", "200", "0", "0");
        InvoiceItem it1 = item("IT1", inv, s1, 2, null);
        InvoiceItem it2 = item("IT2", inv, s2, 3, null);
        when(invoiceItemRepository.findAllByInvoiceId("INV1")).thenReturn(List.of(it1, it2));

        MedicalRecord mr = record("R");
        List<MedicalOrder> existing = new ArrayList<>();
        existing.addAll(IntStream.rangeClosed(1, 2).mapToObj(i -> order("O1"+i, mr, s1, it1, MedicalOrderStatus.PENDING)).toList());
        existing.addAll(IntStream.rangeClosed(1, 3).mapToObj(i -> order("O2"+i, mr, s2, it2, MedicalOrderStatus.PENDING)).toList());
        when(medicalOrderRepository.findAllByInvoiceItemIdIn(List.of("IT1","IT2"))).thenReturn(existing);

        UpdateInvoiceRequest rq = new UpdateInvoiceRequest();
        rq.setInvoiceId("INV1");
        rq.setStaffId("ST1");
        rq.setServices(List.of()); // empty

        InOrder inOrder = inOrder(medicalOrderRepository, invoiceItemRepository);

        InvoiceResponse res = service.updateInvoiceItems(rq);
        assertThat(res.getInvoiceId()).isEqualTo("INV1");

        // xoá orders trước, rồi items
        // xoá orders trước, rồi items
        inOrder.verify(medicalOrderRepository).deleteAll(argThat(iter -> {
            List<MedicalOrder> list = new ArrayList<>();
            iter.forEach(list::add);
            return list.size() == 5;
        }));
        inOrder.verify(invoiceItemRepository).deleteAll(argThat(iter -> {
            List<InvoiceItem> list = new ArrayList<>();
            iter.forEach(list::add);
            return list.size() == 2;
        }));


        assertThat(inv.getOriginalTotal()).isEqualByComparingTo("0");
        assertThat(inv.getDiscountTotal()).isEqualByComparingTo("0");
        assertThat(inv.getVatTotal()).isEqualByComparingTo("0");
        assertThat(inv.getTotal()).isEqualByComparingTo("0");
    }

    // =========================
    // B) Diff giữ/thay/thêm/xoá (7–14)
    // =========================

    /** 7) Giữ nguyên hoàn toàn: qty không đổi → không tạo/ xoá gì */
    @Test
    void updateInvoiceItems_keepAll_noCreatesNoDeletes() {
        Invoice inv = invoice("INV1", InvoiceStatus.UNPAID);
        Staff st = staff("ST1");
        stubCommonSuccess(inv, st);

        MedicalService s1 = svc("S1", "C1", "Name1", "100", "0", "0");
        MedicalService s2 = svc("S2", "C2", "Name2", "150", "10", "5");
        stubService(s1); stubService(s2);

        InvoiceItem it1 = item("IT1", inv, s1, 2, null);
        InvoiceItem it2 = item("IT2", inv, s2, 1, null);
        when(invoiceItemRepository.findAllByInvoiceId("INV1")).thenReturn(List.of(it1, it2));

        MedicalRecord mr = record("R");
        List<MedicalOrder> existing = List.of(
                order("O11", mr, s1, it1, MedicalOrderStatus.PENDING),
                order("O12", mr, s1, it1, MedicalOrderStatus.PENDING),
                order("O21", mr, s2, it2, MedicalOrderStatus.PENDING)
        );
        when(medicalOrderRepository.findAllByInvoiceItemIdIn(List.of("IT1","IT2"))).thenReturn(existing);

        UpdateInvoiceRequest rq = req("INV1", "ST1",
                itemReq("S1", 2),
                itemReq("S2", 1));

        InvoiceResponse resp = service.updateInvoiceItems(rq);
        assertThat(resp.getInvoiceId()).isEqualTo("INV1");

        // không tạo order mới
        verify(medicalOrderRepository, never()).save(any(MedicalOrder.class));

        // implementation hiện tại vẫn gọi deleteAll([]) → verify Iterable rỗng thay vì never()
        verify(medicalOrderRepository).deleteAll(argThat(iter -> !iter.iterator().hasNext()));
        verify(invoiceItemRepository).deleteAll(argThat(iter -> !iter.iterator().hasNext()));

        // totals tính lại theo công thức
        // S1: price=100, qty=2, discount=0, vat=0 => total=200
        // S2: price=150, qty=1, discount=10%, vat=5% => discounted=135; vat=6.75 => total=141.75
        assertThat(inv.getOriginalTotal()).isEqualByComparingTo("350"); // 200 + 150
        assertThat(inv.getDiscountTotal()).isEqualByComparingTo("15");  // 150*10%
        assertThat(inv.getVatTotal()).isEqualByComparingTo("6.75");     // 135*5%
        assertThat(inv.getTotal()).isEqualByComparingTo("341.75");      // 200 + 141.75
    }


    /** 8) Thay đổi số lượng một service (có sẵn) → xoá item cũ, tạo item mới + orders mới */
    @Test
    void updateInvoiceItems_changeQuantity_ofExistingItem() {
        Invoice inv = invoice("INV1", InvoiceStatus.UNPAID);
        Staff st = staff("ST1"); stubCommonSuccess(inv, st);

        MedicalService s1 = svc("S1","C1","Name1","100","0","0"); stubService(s1);
        InvoiceItem it1 = item("IT1", inv, s1, 2, null);
        when(invoiceItemRepository.findAllByInvoiceId("INV1")).thenReturn(List.of(it1));

        MedicalRecord mr = record("R");
        List<MedicalOrder> existing = List.of(
                order("O11", mr, s1, it1, MedicalOrderStatus.PENDING),
                order("O12", mr, s1, it1, MedicalOrderStatus.PENDING)
        );
        when(medicalOrderRepository.findAllByInvoiceItemIdIn(List.of("IT1"))).thenReturn(existing);

        UpdateInvoiceRequest rq = req("INV1","ST1", itemReq("S1", 3)); // đổi từ 2 -> 3

        InvoiceResponse res = service.updateInvoiceItems(rq);
        assertThat(res.getInvoiceId()).isEqualTo("INV1");

        // tạo 3 orders mới
        verify(medicalOrderRepository, times(3)).save(any(MedicalOrder.class));
        // xoá orders cũ + item cũ
        InOrder inOrder = inOrder(medicalOrderRepository, invoiceItemRepository);
        inOrder.verify(medicalOrderRepository).deleteAll(argThat(iter -> {
            List<MedicalOrder> tmp = new ArrayList<>();
            iter.forEach(tmp::add);
            return tmp.size() == 2;
        }));

        inOrder.verify(invoiceItemRepository).deleteAll(argThat(iter -> {
            List<InvoiceItem> tmp = new ArrayList<>();
            iter.forEach(tmp::add);
            return tmp.size() == 1 && "IT1".equals(tmp.get(0).getId());
        }));


        // totals: 100 * 3
        assertThat(inv.getOriginalTotal()).isEqualByComparingTo("300");
        assertThat(inv.getDiscountTotal()).isEqualByComparingTo("0");
        assertThat(inv.getVatTotal()).isEqualByComparingTo("0");
        assertThat(inv.getTotal()).isEqualByComparingTo("300");
    }

    /** 9) Thêm service mới */
    @Test
    void updateInvoiceItems_addNewService_onlyCreates() {
        Invoice inv = invoice("INV1", InvoiceStatus.UNPAID);
        Staff st = staff("ST1");
        stubCommonSuccess(inv, st);

        when(invoiceItemRepository.findAllByInvoiceId("INV1")).thenReturn(List.of());
        when(medicalOrderRepository.findAllByInvoiceItemIdIn(List.of())).thenReturn(List.of());

        MedicalService s1 = svc("S1","C1","Name1","80","0","0");
        stubService(s1);

        UpdateInvoiceRequest rq = req("INV1","ST1", itemReq("S1", 4));

        InvoiceResponse res = service.updateInvoiceItems(rq);
        assertThat(res.getInvoiceId()).isEqualTo("INV1");

        // Tạo 4 orders mới
        verify(medicalOrderRepository, times(4)).save(any(MedicalOrder.class));

        // Implementation hiện tại vẫn gọi deleteAll([]) → verify Iterable rỗng
        verify(medicalOrderRepository).deleteAll(argThat(iter -> !iter.iterator().hasNext()));
        verify(invoiceItemRepository).deleteAll(argThat(iter -> !iter.iterator().hasNext()));

        assertThat(inv.getTotal()).isEqualByComparingTo("320");
    }


    /** 10) Xoá service không còn trong request */
    @Test
    void updateInvoiceItems_removeService_deletesItemAndOrders() {
        Invoice inv = invoice("INV1", InvoiceStatus.UNPAID);
        Staff st = staff("ST1"); stubCommonSuccess(inv, st);

        MedicalService s1 = svc("S1","C1","Name1","50","0","0");
        MedicalService s2 = svc("S2","C2","Name2","200","0","0");
        // Chỉ request cho S1, bỏ S2
        stubService(s1); // ❗️bỏ stubService(s2)

        InvoiceItem it1 = item("IT1", inv, s1, 1, null);
        InvoiceItem it2 = item("IT2", inv, s2, 2, null);
        when(invoiceItemRepository.findAllByInvoiceId("INV1")).thenReturn(List.of(it1, it2));

        MedicalRecord mr = record("R");
        List<MedicalOrder> existing = List.of(
                order("O11", mr, s1, it1, MedicalOrderStatus.PENDING),
                order("O21", mr, s2, it2, MedicalOrderStatus.PENDING),
                order("O22", mr, s2, it2, MedicalOrderStatus.PENDING)
        );
        when(medicalOrderRepository.findAllByInvoiceItemIdIn(List.of("IT1","IT2")))
                .thenReturn(existing);

        UpdateInvoiceRequest rq = req("INV1","ST1", itemReq("S1", 1));

        service.updateInvoiceItems(rq);

        InOrder inOrder = inOrder(medicalOrderRepository, invoiceItemRepository);
        // xoá 2 orders thuộc IT2
        inOrder.verify(medicalOrderRepository).deleteAll(argThat(iter -> {
            List<MedicalOrder> tmp = new ArrayList<>();
            iter.forEach(tmp::add);
            return tmp.size() == 2 &&
                    tmp.stream().map(MedicalOrder::getId).collect(Collectors.toSet())
                            .equals(Set.of("O21","O22"));
        }));
        // xoá item IT2
        inOrder.verify(invoiceItemRepository).deleteAll(argThat(iter -> {
            List<InvoiceItem> tmp = new ArrayList<>();
            iter.forEach(tmp::add);
            return tmp.size() == 1 && "IT2".equals(tmp.get(0).getId());
        }));

        // còn lại tính tiền theo S1
        assertThat(inv.getTotal()).isEqualByComparingTo("50");
    }


    /** 11) Kịch bản hỗn hợp: giữ, thay qty, thêm, xoá */
    /** 11) Kịch bản hỗn hợp: giữ, thay qty, thêm, xoá */
    @Test
    void updateInvoiceItems_mixed_keepChangeAddRemove() {
        Invoice inv = invoice("INV1", InvoiceStatus.UNPAID);
        Staff st = staff("ST1"); stubCommonSuccess(inv, st);

        // current: A(qty=1), B(qty=2), C(qty=1)
        MedicalService A = svc("A","CA","A","100","0","0");
        MedicalService B = svc("B","CB","B","50","0","0");
        MedicalService C = svc("C","CC","C","200","0","0");

        // ❗️Chỉ stub những service xuất hiện trong request
        stubService(A);
        stubService(B);
        // stubService(C);  // <-- GỠ dòng này để tránh UnnecessaryStubbing

        InvoiceItem itA = item("ITA", inv, A, 1, null);
        InvoiceItem itB = item("ITB", inv, B, 2, null);
        InvoiceItem itC = item("ITC", inv, C, 1, null);
        when(invoiceItemRepository.findAllByInvoiceId("INV1")).thenReturn(List.of(itA, itB, itC));

        MedicalRecord mr = record("R");
        List<MedicalOrder> existing = new ArrayList<>();
        existing.add(order("OA1", mr, A, itA, MedicalOrderStatus.PENDING));
        existing.add(order("OB1", mr, B, itB, MedicalOrderStatus.PENDING));
        existing.add(order("OB2", mr, B, itB, MedicalOrderStatus.PENDING));
        existing.add(order("OC1", mr, C, itC, MedicalOrderStatus.PENDING));
        when(medicalOrderRepository.findAllByInvoiceItemIdIn(List.of("ITA","ITB","ITC"))).thenReturn(existing);

        // request: A(1=keep), B(3=change), D(2=add) —> remove C
        MedicalService D = svc("D","CD","D","80","0","0");
        stubService(D); // D có trong request nên cần stub

        UpdateInvoiceRequest rq = req("INV1","ST1",
                itemReq("A",1),
                itemReq("B",3),
                itemReq("D",2)
        );

        service.updateInvoiceItems(rq);

        // tạo 3 + 2 = 5 orders mới (B mới 3, D mới 2)
        verify(medicalOrderRepository, times(5)).save(any(MedicalOrder.class));

        // xoá orders của B cũ (2) + C (1) = 3
        verify(medicalOrderRepository).deleteAll(argThat(iter -> {
            List<MedicalOrder> tmp = new ArrayList<>();
            iter.forEach(tmp::add);
            return tmp.size() == 3;
        }));

        // xoá items: ITB (change) + ITC (removed) = 2
        verify(invoiceItemRepository).deleteAll(argThat(iter -> {
            List<InvoiceItem> tmp = new ArrayList<>();
            iter.forEach(tmp::add);
            return tmp.size() == 2;
        }));

        // totals: A(100*1) + B(50*3) + D(80*2) = 100 + 150 + 160 = 410
        assertThat(inv.getTotal()).isEqualByComparingTo("410");
    }


    /** 12) Nhiều service cùng thay qty */
    @Test
    void updateInvoiceItems_multipleChangeQuantities() {
        Invoice inv = invoice("INV1", InvoiceStatus.UNPAID);
        Staff st = staff("ST1"); stubCommonSuccess(inv, st);

        MedicalService S1 = svc("S1","C1","S1","100","0","0");
        MedicalService S2 = svc("S2","C2","S2","90","0","0");
        stubService(S1); stubService(S2);

        InvoiceItem it1 = item("IT1", inv, S1, 1, null);
        InvoiceItem it2 = item("IT2", inv, S2, 2, null);
        when(invoiceItemRepository.findAllByInvoiceId("INV1")).thenReturn(List.of(it1, it2));

        MedicalRecord mr = record("R");
        List<MedicalOrder> existing = List.of(
                order("O11", mr, S1, it1, MedicalOrderStatus.PENDING),
                order("O21", mr, S2, it2, MedicalOrderStatus.PENDING),
                order("O22", mr, S2, it2, MedicalOrderStatus.PENDING)
        );
        when(medicalOrderRepository.findAllByInvoiceItemIdIn(List.of("IT1","IT2"))).thenReturn(existing);

        UpdateInvoiceRequest rq = req("INV1","ST1",
                itemReq("S1", 3), // 1->3
                itemReq("S2", 1)  // 2->1
        );

        service.updateInvoiceItems(rq);

        // tạo 3 + 1 = 4 orders mới
        verify(medicalOrderRepository, times(4)).save(any(MedicalOrder.class));
        // xoá orders cũ 1+2=3
        verify(medicalOrderRepository).deleteAll(argThat(iter -> {
            List<MedicalOrder> tmp = new ArrayList<>();
            iter.forEach(tmp::add);
            return tmp.size() == 3;
        }));

        verify(invoiceItemRepository).deleteAll(argThat(iter -> {
            List<InvoiceItem> tmp = new ArrayList<>();
            iter.forEach(tmp::add);
            return tmp.size() == 2;
        }));


        // totals: 3*100 + 1*90 = 390
        assertThat(inv.getTotal()).isEqualByComparingTo("390");
    }

    /** 13) Trùng serviceId trong request (lock-in behavior) */
    @Test
    void updateInvoiceItems_duplicateServiceIds_lockInBehavior() {
        Invoice inv = invoice("INV1", InvoiceStatus.UNPAID);
        Staff st = staff("ST1"); stubCommonSuccess(inv, st);

        MedicalService S1 = svc("S1","C1","S1","50","0","0"); stubService(S1);
        InvoiceItem it1 = item("IT1", inv, S1, 1, null);
        when(invoiceItemRepository.findAllByInvoiceId("INV1")).thenReturn(List.of(it1));

        MedicalRecord mr = record("R");
        when(medicalOrderRepository.findAllByInvoiceItemIdIn(List.of("IT1")))
                .thenReturn(List.of(order("O11", mr, S1, it1, MedicalOrderStatus.PENDING)));

        // request có 2 dòng cùng S1
        UpdateInvoiceRequest rq = req("INV1","ST1",
                itemReq("S1", 2),
                itemReq("S1", 3)
        );

        service.updateInvoiceItems(rq);

        // tạo 2 + 3 = 5 orders mới
        verify(medicalOrderRepository, times(5)).save(any(MedicalOrder.class));
        // deleteAll items có thể chứa trùng (theo code), nhưng ít nhất 1 item IT1 bị xoá và orders cũ bị xoá
        verify(medicalOrderRepository).deleteAll(argThat(iter -> {
            List<MedicalOrder> tmp = new ArrayList<>();
            iter.forEach(tmp::add);
            return tmp.size() == 1;
        }));
        verify(invoiceItemRepository).deleteAll(anyList());

        // totals: (2+3)*50 = 250
        assertThat(inv.getTotal()).isEqualByComparingTo("250");
    }

    /** 14) Invoice ban đầu không có currentItems */
    @Test
    void updateInvoiceItems_noCurrentItems_allAreAdds() {
        Invoice inv = invoice("INV1", InvoiceStatus.UNPAID);
        Staff st = staff("ST1");
        stubCommonSuccess(inv, st);

        when(invoiceItemRepository.findAllByInvoiceId("INV1")).thenReturn(List.of());
        when(medicalOrderRepository.findAllByInvoiceItemIdIn(List.of())).thenReturn(List.of());

        MedicalService A = svc("A","CA","A","70","0","0");
        MedicalService B = svc("B","CB","B","30","0","0");
        stubService(A);
        stubService(B);

        UpdateInvoiceRequest rq = req("INV1","ST1",
                itemReq("A", 1),
                itemReq("B", 4)
        );

        service.updateInvoiceItems(rq);

        // tạo 1 + 4 = 5 orders mới
        verify(medicalOrderRepository, times(5)).save(any(MedicalOrder.class));

        // implementation hiện tại vẫn gọi deleteAll([]) → verify Iterable rỗng thay vì never()
        verify(medicalOrderRepository).deleteAll(argThat(iter -> !iter.iterator().hasNext()));
        verify(invoiceItemRepository).deleteAll(argThat(iter -> !iter.iterator().hasNext()));

        // 70 + (30*4) = 190
        assertThat(inv.getTotal()).isEqualByComparingTo("190");
    }


    // =========================
    // C) Tính tiền (15–20)
    // =========================

    /** 15) discount=null, vat=null → coi là 0 */
    @Test
    void updateInvoiceItems_nullDiscountNullVat_treatedAsZero() {
        Invoice inv = invoice("INV1", InvoiceStatus.UNPAID);
        Staff st = staff("ST1"); stubCommonSuccess(inv, st);
        when(invoiceItemRepository.findAllByInvoiceId("INV1")).thenReturn(List.of());
        when(medicalOrderRepository.findAllByInvoiceItemIdIn(List.of())).thenReturn(List.of());

        MedicalService S = svc("S","CS","S","100", null, null);
        stubService(S);

        UpdateInvoiceRequest rq = req("INV1","ST1", itemReq("S", 2));
        service.updateInvoiceItems(rq);

        assertThat(inv.getOriginalTotal()).isEqualByComparingTo("200");
        assertThat(inv.getDiscountTotal()).isEqualByComparingTo("0");
        assertThat(inv.getVatTotal()).isEqualByComparingTo("0");
        assertThat(inv.getTotal()).isEqualByComparingTo("200");
    }

    /** 16) discount=10, vat=8, price=100, qty=2 → item total 194.4 */
    @Test
    void updateInvoiceItems_discount10_vat8_price100_qty2() {
        Invoice inv = invoice("INV1", InvoiceStatus.UNPAID);
        Staff st = staff("ST1"); stubCommonSuccess(inv, st);
        when(invoiceItemRepository.findAllByInvoiceId("INV1")).thenReturn(List.of());
        when(medicalOrderRepository.findAllByInvoiceItemIdIn(List.of())).thenReturn(List.of());

        MedicalService S = svc("S","CS","S","100","10","8");
        stubService(S);

        UpdateInvoiceRequest rq = req("INV1","ST1", itemReq("S", 2));
        service.updateInvoiceItems(rq);

        assertThat(inv.getOriginalTotal()).isEqualByComparingTo("200");
        assertThat(inv.getDiscountTotal()).isEqualByComparingTo("20.0");
        assertThat(inv.getVatTotal()).isEqualByComparingTo("14.4"); // 180 * 8%
        assertThat(inv.getTotal()).isEqualByComparingTo("194.4");
    }

    /** 17) discount=0, vat>0 */
    @Test
    void updateInvoiceItems_onlyVat_applied() {
        Invoice inv = invoice("INV1", InvoiceStatus.UNPAID);
        Staff st = staff("ST1"); stubCommonSuccess(inv, st);
        when(invoiceItemRepository.findAllByInvoiceId("INV1")).thenReturn(List.of());
        when(medicalOrderRepository.findAllByInvoiceItemIdIn(List.of())).thenReturn(List.of());

        MedicalService S = svc("S","CS","S","50","0","5"); stubService(S);

        UpdateInvoiceRequest rq = req("INV1","ST1", itemReq("S", 4)); // 200 subtotal, vat=10
        service.updateInvoiceItems(rq);

        assertThat(inv.getOriginalTotal()).isEqualByComparingTo("200");
        assertThat(inv.getDiscountTotal()).isEqualByComparingTo("0");
        assertThat(inv.getVatTotal()).isEqualByComparingTo("10");
        assertThat(inv.getTotal()).isEqualByComparingTo("210");
    }

    /** 18) vat=0, discount>0 */
    @Test
    void updateInvoiceItems_onlyDiscount_applied() {
        Invoice inv = invoice("INV1", InvoiceStatus.UNPAID);
        Staff st = staff("ST1"); stubCommonSuccess(inv, st);
        when(invoiceItemRepository.findAllByInvoiceId("INV1")).thenReturn(List.of());
        when(medicalOrderRepository.findAllByInvoiceItemIdIn(List.of())).thenReturn(List.of());

        MedicalService S = svc("S","CS","S","200","25","0"); stubService(S);

        UpdateInvoiceRequest rq = req("INV1","ST1", itemReq("S", 1));
        service.updateInvoiceItems(rq);

        assertThat(inv.getOriginalTotal()).isEqualByComparingTo("200");
        assertThat(inv.getDiscountTotal()).isEqualByComparingTo("50");
        assertThat(inv.getVatTotal()).isEqualByComparingTo("0");
        assertThat(inv.getTotal()).isEqualByComparingTo("150");
    }

    /** 19) Giá & % lẻ (99.99, 12.5%, 8.5%, qty=3) */
    @Test
    void updateInvoiceItems_fractionalPriceAndPercents() {
        Invoice inv = invoice("INV1", InvoiceStatus.UNPAID);
        Staff st = staff("ST1"); stubCommonSuccess(inv, st);
        when(invoiceItemRepository.findAllByInvoiceId("INV1")).thenReturn(List.of());
        when(medicalOrderRepository.findAllByInvoiceItemIdIn(List.of())).thenReturn(List.of());

        MedicalService S = svc("S","CS","S","99.99","12.5","8.5"); stubService(S);

        UpdateInvoiceRequest rq = req("INV1","ST1", itemReq("S", 3));
        service.updateInvoiceItems(rq);

        // per unit: discount = 12.49875 => discounted = 87.49125
        // subtotal = 262.47375
        // vat = 22.31026875
        // total ≈ 284.78401875
        assertThat(inv.getOriginalTotal()).isEqualByComparingTo("299.97");
        assertThat(inv.getDiscountTotal()).isEqualByComparingTo("37.49625");
        assertThat(inv.getVatTotal()).isEqualByComparingTo("22.31026875");
        assertThat(inv.getTotal()).isEqualByComparingTo("284.78401875");
    }

    /** 20) Nhiều dòng mix discount/vat */
    @Test
    void updateInvoiceItems_mixedDiscountVatLines() {
        Invoice inv = invoice("INV1", InvoiceStatus.UNPAID);
        Staff st = staff("ST1"); stubCommonSuccess(inv, st);
        when(invoiceItemRepository.findAllByInvoiceId("INV1")).thenReturn(List.of());
        when(medicalOrderRepository.findAllByInvoiceItemIdIn(List.of())).thenReturn(List.of());

        MedicalService A = svc("A","CA","A","100","10","5"); stubService(A);
        MedicalService B = svc("B","CB","B","50","0","10"); stubService(B);

        UpdateInvoiceRequest rq = req("INV1","ST1",
                itemReq("A", 2), // A: orig=200, disc=20, sub=180, vat=9, total=189
                itemReq("B", 3)  // B: orig=150, disc=0, sub=150, vat=15, total=165
        );
        service.updateInvoiceItems(rq);

        assertThat(inv.getOriginalTotal()).isEqualByComparingTo("350");
        assertThat(inv.getDiscountTotal()).isEqualByComparingTo("20");
        assertThat(inv.getVatTotal()).isEqualByComparingTo("24"); // 9 + 15
        assertThat(inv.getTotal()).isEqualByComparingTo("354");  // 189 + 165
    }

    // =========================
    // D) Gán medicalRecord vào orders & thứ tự xoá (21–24)
    // =========================

    /** 21) Có allOrders trước với medicalRecord A → orders mới gán về A */
    @Test
    void updateInvoiceItems_newOrdersInheritMedicalRecordFromExisting() {
        Invoice inv = invoice("INV1", InvoiceStatus.UNPAID);
        Staff st = staff("ST1"); stubCommonSuccess(inv, st);

        MedicalService S = svc("S","CS","S","100","0","0"); stubService(S);

        // current items: 1 item, có 1 order với record A
        InvoiceItem it = item("IT", inv, S, 1, null);
        when(invoiceItemRepository.findAllByInvoiceId("INV1")).thenReturn(List.of(it));
        MedicalRecord A = record("A");
        when(medicalOrderRepository.findAllByInvoiceItemIdIn(List.of("IT")))
                .thenReturn(List.of(order("O1", A, S, it, MedicalOrderStatus.PENDING)));

        // yêu cầu: S qty=3 (change)
        UpdateInvoiceRequest rq = req("INV1","ST1", itemReq("S", 3));

        service.updateInvoiceItems(rq);

        // capture 3 order mới để check medicalRecord
        ArgumentCaptor<MedicalOrder> capt = ArgumentCaptor.forClass(MedicalOrder.class);
        verify(medicalOrderRepository, times(3)).save(capt.capture());

        assertThat(capt.getAllValues()).allMatch(o -> o.getMedicalRecord() != null && "A".equals(o.getMedicalRecord().getId()));
    }

    /** 22) allOrders trống → orders mới có medicalRecord = null */
    @Test
    void updateInvoiceItems_noExistingOrders_newOrdersHaveNullRecord() {
        Invoice inv = invoice("INV1", InvoiceStatus.UNPAID);
        Staff st = staff("ST1"); stubCommonSuccess(inv, st);

        // current items: 0
        when(invoiceItemRepository.findAllByInvoiceId("INV1")).thenReturn(List.of());
        when(medicalOrderRepository.findAllByInvoiceItemIdIn(List.of())).thenReturn(List.of());

        MedicalService S = svc("S","CS","S","60","0","0"); stubService(S);

        UpdateInvoiceRequest rq = req("INV1","ST1", itemReq("S", 2));
        service.updateInvoiceItems(rq);

        ArgumentCaptor<MedicalOrder> capt = ArgumentCaptor.forClass(MedicalOrder.class);
        verify(medicalOrderRepository, times(2)).save(capt.capture());
        assertThat(capt.getAllValues()).allMatch(o -> o.getMedicalRecord() == null);
    }

    /** 23) Xoá orders trước rồi xoá items (verify thứ tự) */
    @Test
    void updateInvoiceItems_deleteOrdersBeforeItems_orderingVerified() {
        Invoice inv = invoice("INV1", InvoiceStatus.UNPAID);
        Staff st = staff("ST1");
        stubCommonSuccess(inv, st);

        MedicalService S = svc("S","CS","S","40","0","0");
        // ❌ KHÔNG gọi stubService(S); vì request rỗng nên không load service

        InvoiceItem it = item("IT", inv, S, 1, null);
        when(invoiceItemRepository.findAllByInvoiceId("INV1")).thenReturn(List.of(it));

        MedicalRecord mr = record("R");
        List<MedicalOrder> existing = List.of(
                order("O1", mr, S, it, MedicalOrderStatus.PENDING)
        );
        when(medicalOrderRepository.findAllByInvoiceItemIdIn(List.of("IT"))).thenReturn(existing);

        // request: trống → xoá hết
        UpdateInvoiceRequest rq = req("INV1","ST1");
        rq.setServices(List.of());

        InOrder inOrder = inOrder(medicalOrderRepository, invoiceItemRepository);

        service.updateInvoiceItems(rq);

        inOrder.verify(medicalOrderRepository).deleteAll(existing);
        inOrder.verify(invoiceItemRepository).deleteAll(List.of(it));
    }


    /** 24) Save invoice 1 lần & mapper gọi 1 lần với đối tượng đã cập nhật */
    @Test
    void updateInvoiceItems_saveAndMap_once_withUpdatedTotals() {
        Invoice inv = invoice("INV1", InvoiceStatus.UNPAID);
        Staff st = staff("ST1");
        stubCommonSuccess(inv, st);

        when(invoiceItemRepository.findAllByInvoiceId("INV1")).thenReturn(List.of());
        when(medicalOrderRepository.findAllByInvoiceItemIdIn(List.of())).thenReturn(List.of());

        MedicalService S = svc("S","CS","S","25","0","0"); stubService(S);

        UpdateInvoiceRequest rq = req("INV1","ST1", itemReq("S", 2));

        InvoiceResponse out = service.updateInvoiceItems(rq);

        assertThat(inv.getTotal()).isEqualByComparingTo("50");
        verify(invoiceRepository, times(1)).save(inv);
        verify(invoiceMapper, times(1)).toInvoiceResponse(inv);
        assertThat(out.getInvoiceId()).isEqualTo("INV1");
    }

    // =========================
    // Case 1: Invoice không tồn tại → throw INVOICE_NOT_FOUND
    // =========================
    @Test
    void getInvoiceDetail_invoiceNotFound_throws() {
        when(invoiceRepository.findByIdAndDeletedAtIsNull("INV404")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getInvoiceDetail("INV404"))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVOICE_NOT_FOUND);

        verifyNoInteractions(invoiceItemRepository);
    }

    // =========================
    // Case 2: Invoice tồn tại, confirmedBy = null → response.confirmedBy = null
    // =========================
    @Test
    void getInvoiceDetail_confirmedByNull_mapsNull() {
        Patient p = patient("P1", "PCODE1", "Alice", LocalDate.of(1990, 1, 2), Gender.FEMALE, "0987654321");
        Invoice inv = invoice(
                "INV1", "IC-001", p, InvoiceStatus.UNPAID, PaymentType.CASH,
                "100", "0", "0", "100",
                LocalDateTime.of(2024, 1, 10, 9, 0),
                null, // confirmedAt
                null, // confirmedBy
                "desc"
        );
        when(invoiceRepository.findByIdAndDeletedAtIsNull("INV1")).thenReturn(Optional.of(inv));
        when(invoiceItemRepository.findAllByInvoiceId("INV1")).thenReturn(List.of());

        InvoiceDetailResponse res = service.getInvoiceDetail("INV1");

        assertThat(res.getInvoiceId()).isEqualTo("INV1");
        assertThat(res.getConfirmedBy()).isNull();
        assertThat(res.getConfirmedAt()).isNull();
        assertThat(res.getItems()).isEmpty();
    }

    // =========================
    // Case 3: Invoice tồn tại, confirmedBy != null → lấy tên staff
    // =========================
    @Test
    void getInvoiceDetail_confirmedByPresent_mapsFullName() {
        Patient p = patient("P1", "PCODE1", "Bob", LocalDate.of(1985, 5, 6), Gender.MALE, "0900000000");
        Staff cashier = staff("ST1", "Cashier One");
        Invoice inv = invoice(
                "INV2", "IC-002", p, InvoiceStatus.PAID, PaymentType.TRANSFER,
                "200", "10", "9", "199",
                LocalDateTime.of(2024, 2, 1, 10, 0),
                LocalDateTime.of(2024, 2, 1, 10, 5), // confirmedAt
                cashier,
                "paid invoice"
        );
        when(invoiceRepository.findByIdAndDeletedAtIsNull("INV2")).thenReturn(Optional.of(inv));
        when(invoiceItemRepository.findAllByInvoiceId("INV2")).thenReturn(List.of());

        InvoiceDetailResponse res = service.getInvoiceDetail("INV2");

        assertThat(res.getConfirmedBy()).isEqualTo("Cashier One");
        assertThat(res.getConfirmedAt()).isEqualTo(LocalDateTime.of(2024, 2, 1, 10, 5));
        assertThat(res.getPaymentType()).isEqualTo(PaymentType.TRANSFER);
    }

    // =========================
    // Case 4: Không có item → items rỗng
    // =========================
    @Test
    void getInvoiceDetail_noItems_emptyList() {
        Patient p = patient("P2", "PC2", "Charlie", LocalDate.of(2000, 3, 3), Gender.OTHER, "0912345678");
        Invoice inv = invoice(
                "INV3", "IC-003", p, InvoiceStatus.UNPAID, PaymentType.CASH,
                "0", "0", "0", "0",
                LocalDateTime.of(2024, 3, 15, 14, 0),
                null, null, null
        );
        when(invoiceRepository.findByIdAndDeletedAtIsNull("INV3")).thenReturn(Optional.of(inv));
        when(invoiceItemRepository.findAllByInvoiceId("INV3")).thenReturn(List.of());

        InvoiceDetailResponse res = service.getInvoiceDetail("INV3");

        assertThat(res.getItems()).isEmpty();
        assertThat(res.getInvoiceCode()).isEqualTo("IC-003");
        assertThat(res.getPatientName()).isEqualTo("Charlie");
    }

    // =========================
    // Case 5: Nhiều item → map đủ & đúng các trường item
    // =========================
    @Test
    void getInvoiceDetail_multipleItems_mapCorrectly() {
        Patient p = patient("P3", "PC3", "Daisy", LocalDate.of(1999, 9, 9), Gender.FEMALE, "0999999999");
        Invoice inv = invoice(
                "INV4", "IC-004", p, InvoiceStatus.UNPAID, PaymentType.CASH,
                "0", "0", "0", "0",
                LocalDateTime.of(2024, 4, 1, 8, 30),
                null, null, null
        );

        MedicalService s1 = svc("S1","SC1","Blood Test","100","10","5");
        MedicalService s2 = svc("S2","SC2","X-Ray","200","0","7");

        InvoiceItem it1 = item("IT1", inv, s1, 2, "189.0"); // ví dụ total đã tính
        InvoiceItem it2 = item("IT2", inv, s2, 1, "214.0");

        when(invoiceRepository.findByIdAndDeletedAtIsNull("INV4")).thenReturn(Optional.of(inv));
        when(invoiceItemRepository.findAllByInvoiceId("INV4")).thenReturn(List.of(it1, it2));

        InvoiceDetailResponse res = service.getInvoiceDetail("INV4");

        assertThat(res.getItems()).hasSize(2);
        InvoiceItemResponse r1 = res.getItems().get(0);
        InvoiceItemResponse r2 = res.getItems().get(1);

        assertThat(r1.getId()).isEqualTo("IT1");
        assertThat(r1.getMedicalServiceId()).isEqualTo("S1");
        assertThat(r1.getName()).isEqualTo("Blood Test");
        assertThat(r1.getQuantity()).isEqualTo(2);
        assertThat(r1.getServiceCode()).isEqualTo("SC1");
        assertThat(r1.getPrice()).isEqualByComparingTo("100");
        assertThat(r1.getDiscount()).isEqualByComparingTo("10");
        assertThat(r1.getVat()).isEqualByComparingTo("5");
        assertThat(r1.getTotal()).isEqualByComparingTo("189.0");

        assertThat(r2.getId()).isEqualTo("IT2");
        assertThat(r2.getMedicalServiceId()).isEqualTo("S2");
        assertThat(r2.getName()).isEqualTo("X-Ray");
        assertThat(r2.getQuantity()).isEqualTo(1);
        assertThat(r2.getServiceCode()).isEqualTo("SC2");
        assertThat(r2.getPrice()).isEqualByComparingTo("200");
        assertThat(r2.getDiscount()).isEqualByComparingTo("0");
        assertThat(r2.getVat()).isEqualByComparingTo("7");
        assertThat(r2.getTotal()).isEqualByComparingTo("214.0");
    }

    // =========================
    // Case 6: Happy-path đầy đủ dữ liệu invoice → map đúng mọi field
    // =========================
    @Test
    void getInvoiceDetail_happyPath_allInvoiceFieldsMapped() {
        Patient p = patient("P9", "PC9", "Erin", LocalDate.of(1977, 12, 24), Gender.FEMALE, "0888888888");
        Staff cashier = staff("ST9", "Mr. Cashier");
        LocalDateTime createdAt = LocalDateTime.of(2024, 6, 20, 9, 45);
        LocalDateTime confirmedAt = LocalDateTime.of(2024, 6, 20, 10, 0);

        Invoice inv = invoice(
                "INV9", "IC-009", p, InvoiceStatus.PAID, PaymentType.TRANSFER,
                "500.00", "50.00", "36.00", "486.00",
                createdAt, confirmedAt, cashier, "notes here"
        );

        // 1 item để response không rỗng
        MedicalService s = svc("SX","SCX","Ultrasound","300","10","8");
        InvoiceItem it = item("ITX", inv, s, 1, "291.60");

        when(invoiceRepository.findByIdAndDeletedAtIsNull("INV9")).thenReturn(Optional.of(inv));
        when(invoiceItemRepository.findAllByInvoiceId("INV9")).thenReturn(List.of(it));

        InvoiceDetailResponse res = service.getInvoiceDetail("INV9");

        // invoice fields
        assertThat(res.getInvoiceId()).isEqualTo("INV9");
        assertThat(res.getInvoiceCode()).isEqualTo("IC-009");
        assertThat(res.getPatientName()).isEqualTo("Erin");
        assertThat(res.getPatientCode()).isEqualTo("PC9");
        assertThat(res.getDateOfBirth()).isEqualTo(LocalDate.of(1977,12,24));
        assertThat(res.getGender()).isEqualTo(Gender.FEMALE);
        assertThat(res.getPhone()).isEqualTo("0888888888");
        assertThat(res.getCreatedAt()).isEqualTo(createdAt);
        assertThat(res.getConfirmedAt()).isEqualTo(confirmedAt);
        assertThat(res.getConfirmedBy()).isEqualTo("Mr. Cashier");
        assertThat(res.getPaymentType()).isEqualTo(PaymentType.TRANSFER);
        assertThat(res.getOriginalTotal()).isEqualByComparingTo("500.00");
        assertThat(res.getDiscountTotal()).isEqualByComparingTo("50.00");
        assertThat(res.getVatTotal()).isEqualByComparingTo("36.00");
        assertThat(res.getTotal()).isEqualByComparingTo("486.00");
        assertThat(res.getDescription()).isEqualTo("notes here");

        // items present
        assertThat(res.getItems()).hasSize(1);
        assertThat(res.getItems().get(0).getId()).isEqualTo("ITX");
        assertThat(res.getItems().get(0).getMedicalServiceId()).isEqualTo("SX");
        assertThat(res.getItems().get(0).getTotal()).isEqualByComparingTo("291.60");
    }
}
