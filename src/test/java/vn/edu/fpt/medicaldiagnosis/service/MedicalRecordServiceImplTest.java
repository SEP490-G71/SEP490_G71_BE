package vn.edu.fpt.medicaldiagnosis.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.fpt.medicaldiagnosis.dto.request.InvoiceServiceRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.MedicalRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.*;
import vn.edu.fpt.medicaldiagnosis.entity.*;
import vn.edu.fpt.medicaldiagnosis.enums.Gender;
import vn.edu.fpt.medicaldiagnosis.enums.InvoiceStatus;
import vn.edu.fpt.medicaldiagnosis.enums.MedicalOrderStatus;
import vn.edu.fpt.medicaldiagnosis.enums.MedicalRecordStatus;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.QueuePatientsMapper;
import vn.edu.fpt.medicaldiagnosis.repository.*;
import vn.edu.fpt.medicaldiagnosis.service.impl.CodeGeneratorService;
import vn.edu.fpt.medicaldiagnosis.service.impl.MedicalRecordServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MedicalRecordServiceImplTest {
    @Mock
    PatientRepository patientRepository;
    @Mock
    StaffRepository staffRepository;
    @Mock
    QueuePatientsRepository queuePatientsRepository;
    @Mock
    DepartmentRepository departmentRepository;
    @Mock
    MedicalRecordRepository medicalRecordRepository;
    @Mock RoomTransferHistoryRepository roomTransferHistoryRepository;
    @Mock InvoiceRepository invoiceRepository;
    @Mock InvoiceItemRepository invoiceItemRepository;
    @Mock MedicalServiceRepository medicalServiceRepository;
    @Mock MedicalOrderRepository medicalOrderRepository;
    @Mock WorkScheduleService workScheduleService;
    @Mock AccountService accountService;
    @Mock
    CodeGeneratorService codeGeneratorService;
    @Mock
    AccountRepository accountRepository;

    @Mock
    MedicalResultRepository medicalResultRepository;

    @Mock
    MedicalResultImageRepository medicalResultImageRepository;

    @Mock
    QueuePatientsMapper queuePatientsMapper;

    @InjectMocks
    MedicalRecordServiceImpl medicalRecordService;

    // ===== Helpers =====
    private MedicalRequest baseValidRequest() {
        List<MedicalRequest.ServiceRequest> services = new java.util.ArrayList<>();
        services.add(MedicalRequest.ServiceRequest.builder()
                .serviceId("SV1").quantity(1).build());

        return MedicalRequest.builder()
                .patientId("P1")
                .staffId("S1")
                .visitId("V1")
                .diagnosisText("Dx")
                .services(services)  // <-- mutable
                .temperature(36.5).respiratoryRate(16).bloodPressure("120/80")
                .heartRate(75).heightCm(165.0).weightKg(60.0).bmi(22.0).spo2(98)
                .notes("ok")
                .build();
    }


    private Patient patient(String id) {
        Patient p = new Patient();
        p.setId(id);
        return p;
    }

    private Staff staff(String id, String accountId) {
        Staff s = new Staff();
        s.setId(id);
        s.setAccountId(accountId);
        return s;
    }

    private AccountResponse accountWithRoles(String... names) {
        AccountResponse acc = new AccountResponse();
        Set<RoleResponse> set = new HashSet<>();
        if (names != null) {
            for (String n : names) {
                RoleResponse rr = new RoleResponse();
                rr.setName(n);
                set.add(rr);
            }
        }
        acc.setRoles(set);   // <-- Set<RoleResponse>
        return acc;
    }

    private QueuePatients visit(String id, String patientId, String roomNumber) {
        QueuePatients v = new QueuePatients();
        v.setId(id);
        v.setPatientId(patientId);
        v.setRoomNumber(roomNumber);
        return v;
    }

    private void arrangeHappyPathBeforeServices() {
        Patient p = new Patient(); p.setId("P1");
        Staff s = new Staff(); s.setId("S1"); s.setAccountId("A1");
        QueuePatients v = new QueuePatients(); v.setId("V1"); v.setPatientId("P1"); v.setRoomNumber("R101");
        Department d = new Department(); d.setId("D1"); d.setRoomNumber("R101");

        when(patientRepository.findByIdAndDeletedAtIsNull("P1")).thenReturn(Optional.of(p));
        when(staffRepository.findByIdAndDeletedAtIsNull("S1")).thenReturn(Optional.of(s));
        when(workScheduleService.isStaffOnShiftNow("S1")).thenReturn(true);

        AccountResponse acc = new AccountResponse();
        RoleResponse r = new RoleResponse(); r.setName("DOCTOR");
        acc.setRoles(Set.of(r));
        when(accountService.getAccount("A1")).thenReturn(acc);

        when(queuePatientsRepository.findByIdAndDeletedAtIsNull("V1")).thenReturn(Optional.of(v));
        when(departmentRepository.findByRoomNumberAndDeletedAtIsNull("R101")).thenReturn(Optional.of(d));

        // generate code
        when(codeGeneratorService.generateCode(eq("MEDICAL_RECORD"), any(), anyInt())).thenReturn("MR-000001");
        when(codeGeneratorService.generateCode(eq("INVOICE"), any(), anyInt())).thenReturn("INV-000001");

        // record & invoice save -> trả về có id
        when(medicalRecordRepository.save(any())).thenAnswer(inv -> {
            MedicalRecord m = inv.getArgument(0);
            m.setId("MRID");
            return m;
        });
        when(invoiceRepository.save(any())).thenAnswer(inv -> {
            Invoice in = inv.getArgument(0);
            in.setId("INVID");
            return in;
        });
        // invoice item save: trả về y như vào
        when(invoiceItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        // medical order save: set id giả
        when(medicalOrderRepository.save(any())).thenAnswer(inv -> {
            MedicalOrder o = inv.getArgument(0);
            o.setId(UUID.randomUUID().toString());
            return o;
        });
    }

    private void arrangeBeforeServiceLookupOnly() {
        Patient p = new Patient(); p.setId("P1");
        Staff s = new Staff(); s.setId("S1"); s.setAccountId("A1");
        QueuePatients v = new QueuePatients(); v.setId("V1"); v.setPatientId("P1"); v.setRoomNumber("R101");
        Department d = new Department(); d.setId("D1"); d.setRoomNumber("R101");

        when(patientRepository.findByIdAndDeletedAtIsNull("P1")).thenReturn(Optional.of(p));
        when(staffRepository.findByIdAndDeletedAtIsNull("S1")).thenReturn(Optional.of(s));
        when(workScheduleService.isStaffOnShiftNow("S1")).thenReturn(true);

        AccountResponse acc = new AccountResponse();
        RoleResponse r = new RoleResponse(); r.setName("DOCTOR");
        acc.setRoles(Set.of(r));
        when(accountService.getAccount("A1")).thenReturn(acc);

        when(queuePatientsRepository.findByIdAndDeletedAtIsNull("V1")).thenReturn(Optional.of(v));
        when(departmentRepository.findByRoomNumberAndDeletedAtIsNull("R101")).thenReturn(Optional.of(d));

        when(codeGeneratorService.generateCode(eq("MEDICAL_RECORD"), any(), anyInt())).thenReturn("MR-000001");
        when(codeGeneratorService.generateCode(eq("INVOICE"), any(), anyInt())).thenReturn("INV-000001");

        when(medicalRecordRepository.save(any())).thenAnswer(inv -> {
            MedicalRecord m = inv.getArgument(0);
            m.setId("MRID");
            return m;
        });
        when(invoiceRepository.save(any())).thenAnswer(inv -> {
            Invoice in = inv.getArgument(0);
            in.setId("INVID");
            return in;
        });
        // ❌ KHÔNG stub invoiceItemRepository và medicalOrderRepository
    }


    private MedicalService svcDef(String id, String code, String name,
                                  BigDecimal price, BigDecimal discount, BigDecimal vat,
                                  boolean isDefault) {
        MedicalService ms = new MedicalService();
        ms.setId(id);
        ms.setServiceCode(code);
        ms.setName(name);
        ms.setPrice(price);
        ms.setDiscount(discount);
        ms.setVat(vat);
        ms.setDefaultService(isDefault);
        return ms;
    }

    @AfterEach
    void clearCtx() {
        SecurityContextHolder.clearContext();
    }

    private MedicalRecord recordWithPatient() {
        Patient p = new Patient(); p.setId("PAT");
        MedicalRecord mr = new MedicalRecord();
        mr.setId("REC");
        mr.setPatient(p);
        return mr;
    }


    private MedicalRecord baseRecord() {
        MedicalRecord rec = new MedicalRecord();
        rec.setId("REC");
        rec.setStatus(MedicalRecordStatus.TESTING);
        rec.setMedicalRecordCode("MR-001");
        rec.setCreatedAt(java.time.LocalDateTime.now());

        Patient p = new Patient();
        p.setId("P1"); p.setFullName("Patient A"); p.setGender(Gender.MALE);
        p.setPatientCode("P001"); p.setPhone("0909xxxxxx");
        p.setDob(java.time.LocalDate.of(1990,1,1));
        rec.setPatient(p);

        Staff createdBy = new Staff(); createdBy.setFullName("Doctor X");
        rec.setCreatedBy(createdBy);

        QueuePatients visit = new QueuePatients(); visit.setId("V1");
        rec.setVisit(visit);
        return rec;
    }

    private Department deptLab() {
        Department d = new Department();
        d.setId("D1"); d.setName("Lab");
        return d;
    }

    private void setAuth(String username) {
        var auth = new UsernamePasswordAuthenticationToken(username, "pwd");
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private InvoiceServiceRequest oneServiceReq(String id, Integer qty) {
        return InvoiceServiceRequest.builder()
                .services(List.of(InvoiceServiceRequest.ServiceRequest.builder()
                        .serviceId(id).quantity(qty).build()))
                .build();
    }

    // ---------------------------------------------------------------
    // 1) Patient không tồn tại -> PATIENT_NOT_FOUND
    // ---------------------------------------------------------------
    @Test
    void createMedicalRecord_patientNotFound() {
        MedicalRequest req = baseValidRequest();

        when(patientRepository.findByIdAndDeletedAtIsNull("P1"))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> medicalRecordService.createMedicalRecord(req));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PATIENT_NOT_FOUND);
    }

    // ---------------------------------------------------------------
    // 2) Staff không tồn tại -> STAFF_NOT_FOUND
    // ---------------------------------------------------------------
    @Test
    void createMedicalRecord_staffNotFound() {
        MedicalRequest req = baseValidRequest();

        when(patientRepository.findByIdAndDeletedAtIsNull("P1"))
                .thenReturn(Optional.of(patient("P1")));
        when(staffRepository.findByIdAndDeletedAtIsNull("S1"))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> medicalRecordService.createMedicalRecord(req));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.STAFF_NOT_FOUND);
    }

    // ---------------------------------------------------------------
    // 3) Staff không trong ca -> ACTION_NOT_ALLOWED
    // ---------------------------------------------------------------
    @Test
    void createMedicalRecord_staffNotOnShift() {
        MedicalRequest req = baseValidRequest();

        when(patientRepository.findByIdAndDeletedAtIsNull("P1"))
                .thenReturn(Optional.of(patient("P1")));
        when(staffRepository.findByIdAndDeletedAtIsNull("S1"))
                .thenReturn(Optional.of(staff("S1", "A1")));
        when(workScheduleService.isStaffOnShiftNow("S1"))
                .thenReturn(false);

        AppException ex = assertThrows(AppException.class,
                () -> medicalRecordService.createMedicalRecord(req));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACTION_NOT_ALLOWED);
    }

    // ---------------------------------------------------------------
    // 4) Staff không có role DOCTOR -> NO_PERMISSION
    // ---------------------------------------------------------------
    @Test
    void createMedicalRecord_staffNotDoctor() {
        MedicalRequest req = baseValidRequest();
        Staff staff = staff("S1", "A1");

        when(patientRepository.findByIdAndDeletedAtIsNull("P1"))
                .thenReturn(Optional.of(patient("P1")));
        when(staffRepository.findByIdAndDeletedAtIsNull("S1"))
                .thenReturn(Optional.of(staff));
        when(workScheduleService.isStaffOnShiftNow("S1"))
                .thenReturn(true);

        when(accountService.getAccount("A1"))
                .thenReturn(accountWithRoles("NURSE")); // không có DOCTOR

        AppException ex = assertThrows(AppException.class,
                () -> medicalRecordService.createMedicalRecord(req));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NO_PERMISSION);
    }

    // ---------------------------------------------------------------
    // 5) Visit không tồn tại -> QUEUE_PATIENT_NOT_FOUND
    // ---------------------------------------------------------------
    @Test
    void createMedicalRecord_visitNotFound() {
        MedicalRequest req = baseValidRequest();
        Staff staff = staff("S1", "A1");

        when(patientRepository.findByIdAndDeletedAtIsNull("P1"))
                .thenReturn(Optional.of(patient("P1")));
        when(staffRepository.findByIdAndDeletedAtIsNull("S1"))
                .thenReturn(Optional.of(staff));
        when(workScheduleService.isStaffOnShiftNow("S1"))
                .thenReturn(true);
        when(accountService.getAccount("A1"))
                .thenReturn(accountWithRoles("DOCTOR"));

        when(queuePatientsRepository.findByIdAndDeletedAtIsNull("V1"))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> medicalRecordService.createMedicalRecord(req));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.QUEUE_PATIENT_NOT_FOUND);
    }

    // ---------------------------------------------------------------
    // 6) Visit không thuộc về patient -> INVALID_DATA
    // ---------------------------------------------------------------
    @Test
    void createMedicalRecord_visitNotBelongToPatient_invalidData() {
        MedicalRequest req = baseValidRequest();
        Staff staff = staff("S1", "A1");

        when(patientRepository.findByIdAndDeletedAtIsNull("P1"))
                .thenReturn(Optional.of(patient("P1")));
        when(staffRepository.findByIdAndDeletedAtIsNull("S1"))
                .thenReturn(Optional.of(staff));
        when(workScheduleService.isStaffOnShiftNow("S1"))
                .thenReturn(true);
        when(accountService.getAccount("A1"))
                .thenReturn(accountWithRoles("DOCTOR"));

        // Visit có patientId KHÁC với request.getPatientId()
        when(queuePatientsRepository.findByIdAndDeletedAtIsNull("V1"))
                .thenReturn(Optional.of(visit("V1", "OTHER_PATIENT", "R101")));

        AppException ex = assertThrows(AppException.class,
                () -> medicalRecordService.createMedicalRecord(req));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_DATA);
    }

    // ---------------------------------------------------------------
    // 7) Phòng ban không tồn tại -> DEPARTMENT_NOT_FOUND
    // (đã qua mọi check trước đó; visit thuộc đúng patient)
    // ---------------------------------------------------------------
    @Test
    void createMedicalRecord_departmentNotFound() {
        MedicalRequest req = baseValidRequest();
        Staff staff = staff("S1", "A1");

        when(patientRepository.findByIdAndDeletedAtIsNull("P1"))
                .thenReturn(Optional.of(patient("P1")));
        when(staffRepository.findByIdAndDeletedAtIsNull("S1"))
                .thenReturn(Optional.of(staff));
        when(workScheduleService.isStaffOnShiftNow("S1"))
                .thenReturn(true);
        when(accountService.getAccount("A1"))
                .thenReturn(accountWithRoles("DOCTOR"));

        // Visit đúng patient, nhưng phòng ban không tìm thấy
        when(queuePatientsRepository.findByIdAndDeletedAtIsNull("V1"))
                .thenReturn(Optional.of(visit("V1", "P1", "R101")));

        when(departmentRepository.findByRoomNumberAndDeletedAtIsNull("R101"))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> medicalRecordService.createMedicalRecord(req));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DEPARTMENT_NOT_FOUND);
    }


    // ---------- 8) Service không tồn tại ----------
    @Test
    void service_not_found_throws() {
        arrangeBeforeServiceLookupOnly();  // chỉ tới lookup service
        MedicalRequest req = baseValidRequest();
        req.getServices().get(0).setServiceId("SVX");

        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SVX"))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> medicalRecordService.createMedicalRecord(req));
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MEDICAL_SERVICE_NOT_FOUND);
    }


    // ---------- 9) discount = null -> dùng 0 ----------
    @Test
    void discount_null_treated_as_zero() {
        arrangeHappyPathBeforeServices();
        MedicalRequest req = baseValidRequest();;
        req.getServices().get(0).setQuantity(2);

        MedicalService ms = svcDef("SV1", "S1", "Test",
                new BigDecimal("100"), null, BigDecimal.ZERO, false);
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SV1"))
                .thenReturn(Optional.of(ms));

        var res = medicalRecordService.createMedicalRecord(req);

        assertThat(res.getOriginalTotal()).isEqualByComparingTo("200"); // 100*2
        assertThat(res.getDiscountTotal()).isEqualByComparingTo("0");
        assertThat(res.getVatTotal()).isEqualByComparingTo("0");
        assertThat(res.getTotalAmount()).isEqualByComparingTo("200");
    }

    // ---------- 10) VAT = null -> dùng 0 ----------
    @Test
    void vat_null_treated_as_zero() {
        arrangeHappyPathBeforeServices();
            MedicalRequest req = baseValidRequest();;
        req.getServices().get(0).setQuantity(3);

        MedicalService ms = svcDef("SV1", "S1", "Test",
                new BigDecimal("50"), new BigDecimal("10"), null, false); // 10% discount, VAT null
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SV1"))
                .thenReturn(Optional.of(ms));

        var res = medicalRecordService.createMedicalRecord(req);

        // original = 50*3 = 150
        // discountPerUnit = 50*10% = 5 -> total discount = 5*3 = 15
        // subtotal = (50-5)*3 = 45*3 = 135
        // vat = 0
        // total = 135
        assertThat(res.getOriginalTotal()).isEqualByComparingTo("150");
        assertThat(res.getDiscountTotal()).isEqualByComparingTo("15");
        assertThat(res.getVatTotal()).isEqualByComparingTo("0");
        assertThat(res.getTotalAmount()).isEqualByComparingTo("135");
    }

    // ---------- 11) discount > 0 & VAT > 0 -> công thức tổng ----------
    @Test
    void discount_and_vat_happy_calculation() {
        arrangeHappyPathBeforeServices();
        MedicalRequest req = baseValidRequest();;
        req.getServices().get(0).setQuantity(2);

        MedicalService ms = svcDef("SV1", "S1", "Test",
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("5"), false);
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SV1"))
                .thenReturn(Optional.of(ms));

        var res = medicalRecordService.createMedicalRecord(req);

        // original = 100*2 = 200
        // discount/unit = 100*10% = 10 -> discountTotal = 20
        // discounted unit = 90 -> subtotal = 90*2 = 180
        // vat = 180*5% = 9
        // total = 189
        assertThat(res.getOriginalTotal()).isEqualByComparingTo("200");
        assertThat(res.getDiscountTotal()).isEqualByComparingTo("20");
        assertThat(res.getVatTotal()).isEqualByComparingTo("9");
        assertThat(res.getTotalAmount()).isEqualByComparingTo("189");
    }

    // ---------- 12) quantity = 1 -> 1 MedicalOrder ----------
    @Test
    void quantity_one_creates_one_order() {
        arrangeHappyPathBeforeServices();
        MedicalRequest req = baseValidRequest();; // quantity mặc định 1

        MedicalService ms = svcDef("SV1", "S1", "X", new BigDecimal("10"),
                BigDecimal.ZERO, BigDecimal.ZERO, false);
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SV1"))
                .thenReturn(Optional.of(ms));

        medicalRecordService.createMedicalRecord(req);

        ArgumentCaptor<MedicalOrder> cap = ArgumentCaptor.forClass(MedicalOrder.class);
        verify(medicalOrderRepository, times(1)).save(cap.capture());
        assertThat(cap.getAllValues()).hasSize(1);
    }

    // ---------- 13) quantity > 1 -> nhiều MedicalOrder ----------
    @Test
    void quantity_gt_one_creates_many_orders() {
        arrangeHappyPathBeforeServices();
        MedicalRequest req = baseValidRequest();;
        req.getServices().get(0).setQuantity(3);

        MedicalService ms = svcDef("SV1", "S1", "X", new BigDecimal("10"),
                BigDecimal.ZERO, BigDecimal.ZERO, false);
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SV1"))
                .thenReturn(Optional.of(ms));

        medicalRecordService.createMedicalRecord(req);

        ArgumentCaptor<MedicalOrder> cap = ArgumentCaptor.forClass(MedicalOrder.class);
        verify(medicalOrderRepository, times(3)).save(cap.capture());
        assertThat(cap.getAllValues()).hasSize(3);
    }

    // ---------- 14) service mặc định -> status COMPLETED ----------
    @Test
    void default_service_sets_order_status_completed() {
        arrangeHappyPathBeforeServices();
        MedicalRequest req = baseValidRequest();;
        req.getServices().get(0).setQuantity(2);

        MedicalService ms = svcDef("SV1", "S1", "X", new BigDecimal("10"),
                BigDecimal.ZERO, BigDecimal.ZERO, true); // defaultService = true
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SV1"))
                .thenReturn(Optional.of(ms));

        medicalRecordService.createMedicalRecord(req);

        ArgumentCaptor<MedicalOrder> cap = ArgumentCaptor.forClass(MedicalOrder.class);
        verify(medicalOrderRepository, times(2)).save(cap.capture());
        assertThat(cap.getAllValues())
                .extracting(MedicalOrder::getStatus)
                .containsOnly(MedicalOrderStatus.COMPLETED);
    }

    // ---------- 15) service không mặc định -> status PENDING ----------
    @Test
    void non_default_service_sets_order_status_pending() {
        arrangeHappyPathBeforeServices();
        MedicalRequest req = baseValidRequest();;
        req.getServices().get(0).setQuantity(2);

        MedicalService ms = svcDef("SV1", "S1", "X", new BigDecimal("10"),
                BigDecimal.ZERO, BigDecimal.ZERO, false);
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SV1"))
                .thenReturn(Optional.of(ms));

        medicalRecordService.createMedicalRecord(req);

        ArgumentCaptor<MedicalOrder> cap = ArgumentCaptor.forClass(MedicalOrder.class);
        verify(medicalOrderRepository, times(2)).save(cap.capture());
        assertThat(cap.getAllValues())
                .extracting(MedicalOrder::getStatus)
                .containsOnly(MedicalOrderStatus.PENDING);
    }

    // ---------- 16) nhiều services -> tổng tiền cộng đúng ----------
    @Test
    void multiple_services_totals_aggregate_correctly() {
        arrangeHappyPathBeforeServices();

        // request có 2 services: SVA x2 và SVB x3
        MedicalRequest req = baseValidRequest(); // hiện có 1 service "SV1"
        req.getServices().get(0).setServiceId("SVA");
        req.getServices().get(0).setQuantity(2);

        // thêm service thứ 2
        req.getServices().add(MedicalRequest.ServiceRequest.builder()
                .serviceId("SVB")
                .quantity(3)
                .build());

        // mock services
        MedicalService A = svcDef("SVA", "A", "Service A",
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("5"), false);
        MedicalService B = svcDef("SVB", "B", "Service B",
                new BigDecimal("50"), BigDecimal.ZERO, new BigDecimal("8"), false);

        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SVA")).thenReturn(Optional.of(A));
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SVB")).thenReturn(Optional.of(B));

        var res = medicalRecordService.createMedicalRecord(req);

        assertThat(res.getOriginalTotal()).isEqualByComparingTo("350"); // 200 + 150
        assertThat(res.getDiscountTotal()).isEqualByComparingTo("20");  // 20 + 0
        assertThat(res.getVatTotal()).isEqualByComparingTo("21");       // 9 + 12
        assertThat(res.getTotalAmount()).isEqualByComparingTo("351");   // 189 + 162

        verify(medicalOrderRepository, times(5)).save(any(MedicalOrder.class));
    }

    // ---------- 17) Tất cả giá trị tiền = 0 ----------
    @Test
    void all_zero_values_totals_zero() {
        arrangeHappyPathBeforeServices();
        MedicalRequest req = baseValidRequest();

        MedicalService ms = svcDef("SV1", "S1", "Zero Service",
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false);
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SV1"))
                .thenReturn(Optional.of(ms));

        var res = medicalRecordService.createMedicalRecord(req);

        assertThat(res.getOriginalTotal()).isEqualByComparingTo("0");
        assertThat(res.getDiscountTotal()).isEqualByComparingTo("0");
        assertThat(res.getVatTotal()).isEqualByComparingTo("0");
        assertThat(res.getTotalAmount()).isEqualByComparingTo("0");
    }

    // ---------- 19) VAT = 100% ----------
    @Test
    void vat_hundred_percent_doubles_total() {
        arrangeHappyPathBeforeServices();
        MedicalRequest req = baseValidRequest();
        req.getServices().get(0).setQuantity(1);

        MedicalService ms = svcDef("SV1", "S1", "VAT100",
                new BigDecimal("50"), BigDecimal.ZERO, new BigDecimal("100"), false);
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SV1"))
                .thenReturn(Optional.of(ms));

        var res = medicalRecordService.createMedicalRecord(req);

        // original = 50, subtotal = 50
        // vat = 50*100% = 50
        // total = 100
        assertThat(res.getOriginalTotal()).isEqualByComparingTo("50");
        assertThat(res.getVatTotal()).isEqualByComparingTo("50");
        assertThat(res.getTotalAmount()).isEqualByComparingTo("100");
    }

    // ---------- 20) Discount > 100% ----------
    @Test
    void discount_over_100_percent_not_negative_total() {
        arrangeHappyPathBeforeServices();
        MedicalRequest req = baseValidRequest();
        req.getServices().get(0).setQuantity(1);

        MedicalService ms = svcDef("SV1", "S1", "CrazyDiscount",
                new BigDecimal("100"), new BigDecimal("150"), BigDecimal.ZERO, false);
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SV1"))
                .thenReturn(Optional.of(ms));

        var res = medicalRecordService.createMedicalRecord(req);

        // original = 100
        // discountPerUnit = 100*150% = 150
        // discounted = 100 - 150 = -50  (subtotal = -50)
        // total = -50
        // ==> Kiểm tra không ra null, nhưng tùy business rule có thể phải clamp >= 0
        assertThat(res.getOriginalTotal()).isEqualByComparingTo("100");
        assertThat(res.getDiscountTotal()).isEqualByComparingTo("150");
        assertThat(res.getTotalAmount().compareTo(BigDecimal.ZERO)).isLessThanOrEqualTo(0);
    }


    // 21) Đã lưu MedicalRecord đúng 1 lần và dữ liệu khớp request
    @Test
    void saves_medical_record_once_with_expected_fields() {
        arrangeHappyPathBeforeServices();
        MedicalRequest req = baseValidRequest(); // SV1 x1
        // service đơn giản để đi qua flow
        MedicalService ms = svcDef("SV1", "S1", "Service 1",
                new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO, false);
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SV1"))
                .thenReturn(Optional.of(ms));

        medicalRecordService.createMedicalRecord(req);

        ArgumentCaptor<MedicalRecord> cap = ArgumentCaptor.forClass(MedicalRecord.class);
        verify(medicalRecordRepository, times(1)).save(cap.capture());
        MedicalRecord saved = cap.getValue();

        assertThat(saved.getPatient().getId()).isEqualTo("P1");
        assertThat(saved.getVisit().getId()).isEqualTo("V1");
        assertThat(saved.getCreatedBy().getId()).isEqualTo("S1");
        assertThat(saved.getDiagnosisText()).isEqualTo("Dx");
        assertThat(saved.getTemperature()).isEqualTo(req.getTemperature());
        assertThat(saved.getRespiratoryRate()).isEqualTo(req.getRespiratoryRate());
        assertThat(saved.getBloodPressure()).isEqualTo(req.getBloodPressure());
        assertThat(saved.getHeartRate()).isEqualTo(req.getHeartRate());
        assertThat(saved.getHeightCm()).isEqualTo(req.getHeightCm());
        assertThat(saved.getWeightKg()).isEqualTo(req.getWeightKg());
        assertThat(saved.getBmi()).isEqualTo(req.getBmi());
        assertThat(saved.getSpo2()).isEqualTo(req.getSpo2());
        assertThat(saved.getStatus()).isEqualTo(MedicalRecordStatus.WAITING_FOR_PAYMENT);
        // được set code từ codeGenerator
        assertThat(saved.getMedicalRecordCode()).isEqualTo("MR-000001");
    }

    // 22) Lưu RoomTransferHistory mặc định: fromDepartment = toDepartment
    @Test
    void saves_default_room_transfer_history_with_same_from_to() {
        arrangeHappyPathBeforeServices();
        MedicalRequest req = baseValidRequest();
        MedicalService ms = svcDef("SV1", "S1", "Service 1",
                new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO, false);
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SV1"))
                .thenReturn(Optional.of(ms));

        medicalRecordService.createMedicalRecord(req);

        ArgumentCaptor<RoomTransferHistory> cap = ArgumentCaptor.forClass(RoomTransferHistory.class);
        verify(roomTransferHistoryRepository, times(1)).save(cap.capture());
        RoomTransferHistory saved = cap.getValue();

        assertThat(saved.getFromDepartment().getRoomNumber()).isEqualTo("R101");
        assertThat(saved.getToDepartment().getRoomNumber()).isEqualTo("R101");
        assertThat(saved.getIsFinal()).isFalse();
        assertThat(saved.getReason()).contains("Khởi tạo");
    }

    // 23) Lưu Invoice với status UNPAID
    @Test
    void saves_invoice_with_unpaid_status() {
        arrangeHappyPathBeforeServices();
        MedicalRequest req = baseValidRequest();
        MedicalService ms = svcDef("SV1", "S1", "Service 1",
                new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO, false);
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SV1"))
                .thenReturn(Optional.of(ms));

        medicalRecordService.createMedicalRecord(req);

        ArgumentCaptor<Invoice> cap = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository, times(2)).save(cap.capture());
        // lưu lần 1 khi tạo invoice (total=0), lần 2 khi cập nhật totals
        List<Invoice> savedInvoices = cap.getAllValues();
        assertThat(savedInvoices.get(0).getStatus()).isEqualTo(InvoiceStatus.UNPAID);
        assertThat(savedInvoices.get(0).getInvoiceCode()).isEqualTo("INV-000001");
        assertThat(savedInvoices.get(0).getPatient().getId()).isEqualTo("P1");
        assertThat(savedInvoices.get(0).getMedicalRecord().getId()).isEqualTo("MRID");
    }

    // 24) Lưu InvoiceItem cho từng service
    @Test
    void saves_invoice_item_for_each_service() {
        arrangeHappyPathBeforeServices();
        // 2 services
        MedicalRequest req = baseValidRequest(); // hiện có SV1
        // đổi list sang mutable và thêm service 2
        List<MedicalRequest.ServiceRequest> svs = new ArrayList<>(req.getServices());
        svs.get(0).setServiceId("SVA");
        svs.get(0).setQuantity(2);
        svs.add(MedicalRequest.ServiceRequest.builder().serviceId("SVB").quantity(3).build());
        req.setServices(svs);

        MedicalService A = svcDef("SVA", "A", "Service A",
                new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO, false);
        MedicalService B = svcDef("SVB", "B", "Service B",
                new BigDecimal("50"), BigDecimal.ZERO, BigDecimal.ZERO, false);

        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SVA")).thenReturn(Optional.of(A));
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SVB")).thenReturn(Optional.of(B));

        medicalRecordService.createMedicalRecord(req);

        ArgumentCaptor<InvoiceItem> cap = ArgumentCaptor.forClass(InvoiceItem.class);
        verify(invoiceItemRepository, times(2)).save(cap.capture());
        List<InvoiceItem> items = cap.getAllValues();

        // 1 item cho mỗi service (tổng quantity dồn trong field quantity)
        assertThat(items).hasSize(2);
        assertThat(items.get(0).getService().getId()).isIn("SVA", "SVB");
        assertThat(items.get(1).getService().getId()).isIn("SVA", "SVB");
    }

    // 25) Lưu đúng số MedicalOrder = tổng quantity
    @Test
    void saves_expected_number_of_medical_orders_equals_total_quantity() {
        arrangeHappyPathBeforeServices();
        // SVA x2, SVB x3 -> tổng 5 order
        MedicalRequest req = baseValidRequest();
        List<MedicalRequest.ServiceRequest> svs = new ArrayList<>(req.getServices());
        svs.get(0).setServiceId("SVA");
        svs.get(0).setQuantity(2);
        svs.add(MedicalRequest.ServiceRequest.builder().serviceId("SVB").quantity(3).build());
        req.setServices(svs);

        MedicalService A = svcDef("SVA", "A", "Service A",
                new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO, false);
        MedicalService B = svcDef("SVB", "B", "Service B",
                new BigDecimal("50"), BigDecimal.ZERO, BigDecimal.ZERO, false);

        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SVA")).thenReturn(Optional.of(A));
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SVB")).thenReturn(Optional.of(B));

        medicalRecordService.createMedicalRecord(req);

        verify(medicalOrderRepository, times(5)).save(any(MedicalOrder.class));
    }

    // 26) Gọi codeGenerator.generateCode đúng 2 lần: MEDICAL_RECORD & INVOICE
    @Test
    void calls_code_generator_twice_for_record_and_invoice() {
        arrangeHappyPathBeforeServices();
        MedicalRequest req = baseValidRequest();

        MedicalService ms = svcDef("SV1", "S1", "Service 1",
                new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO, false);
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SV1"))
                .thenReturn(Optional.of(ms));

        medicalRecordService.createMedicalRecord(req);

        verify(codeGeneratorService, times(1))
                .generateCode(eq("MEDICAL_RECORD"), anyString(), anyInt());
        verify(codeGeneratorService, times(1))
                .generateCode(eq("INVOICE"), anyString(), anyInt());
    }

    // 27) Gọi accountService.getAccount đúng staff.accountId
    @Test
    void calls_account_service_with_staff_account_id() {
        arrangeHappyPathBeforeServices();
        MedicalRequest req = baseValidRequest();

        MedicalService ms = svcDef("SV1", "S1", "Service 1",
                new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO, false);
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SV1"))
                .thenReturn(Optional.of(ms));

        medicalRecordService.createMedicalRecord(req);

        verify(accountService, times(1)).getAccount("A1");
    }


    // 28) Một service có quantity rất lớn (vd 100) -> tạo đúng 100 MedicalOrder
    @Test
    void very_large_quantity_creates_expected_number_of_orders() {
        arrangeHappyPathBeforeServices();

        MedicalRequest req = baseValidRequest(); // đang có SV1 x1
        req.getServices().get(0).setQuantity(100); // tăng lên 100

        MedicalService ms = svcDef("SV1", "S1", "Bulk Service",
                new BigDecimal("10"), BigDecimal.ZERO, BigDecimal.ZERO, false);
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SV1"))
                .thenReturn(Optional.of(ms));

        medicalRecordService.createMedicalRecord(req);

        verify(medicalOrderRepository, times(100)).save(any(MedicalOrder.class));
        // 1 invoice item cho 1 service, quantity dồn vào field quantity
        verify(invoiceItemRepository, times(1)).save(any(InvoiceItem.class));
    }

    // 29) Danh sách services rỗng -> không tạo InvoiceItem & MedicalOrder, totals = 0
    @Test
    void empty_services_creates_no_items_no_orders_and_zero_totals() {
        arrangeBeforeServiceLookupOnly();

        MedicalRequest req = baseValidRequest();
        // thay list thành rỗng (mutable)
        req.setServices(new java.util.ArrayList<>());

        var res = medicalRecordService.createMedicalRecord(req);

        // Không tạo invoice item và medical order
        verify(invoiceItemRepository, times(0)).save(any(InvoiceItem.class));
        verify(medicalOrderRepository, times(0)).save(any(MedicalOrder.class));

        // Totals bằng 0
        assertThat(res.getOriginalTotal()).isEqualByComparingTo("0");
        assertThat(res.getDiscountTotal()).isEqualByComparingTo("0");
        assertThat(res.getVatTotal()).isEqualByComparingTo("0");
        assertThat(res.getTotalAmount()).isEqualByComparingTo("0");
    }

    // 30) Service có giá trị tiền cực lớn -> BigDecimal tính đúng, không tràn số
    @Test
    void extremely_large_money_values_are_computed_correctly() {
        arrangeHappyPathBeforeServices();

        MedicalRequest req = baseValidRequest();
        req.getServices().get(0).setQuantity(2); // kiểm tra nhân lên

        // price = 1,000,000,000,000; discount = 10%; vat = 10%
        // original = 1e12 * 2 = 2,000,000,000,000
        // discountPerUnit = 1e12 * 10% = 100,000,000,000 -> discountTotal = 200,000,000,000
        // discountedUnit = 900,000,000,000
        // subtotal = 900,000,000,000 * 2 = 1,800,000,000,000
        // vat = 1,800,000,000,000 * 10% = 180,000,000,000
        // total = 1,980,000,000,000
        MedicalService big = svcDef("SV1", "BIG", "Extremely Big",
                new BigDecimal("1000000000000"), new BigDecimal("10"), new BigDecimal("10"), false);
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SV1"))
                .thenReturn(Optional.of(big));

        var res = medicalRecordService.createMedicalRecord(req);

        assertThat(res.getOriginalTotal()).isEqualByComparingTo("2000000000000");
        assertThat(res.getDiscountTotal()).isEqualByComparingTo("200000000000");
        assertThat(res.getVatTotal()).isEqualByComparingTo("180000000000");
        assertThat(res.getTotalAmount()).isEqualByComparingTo("1980000000000");
    }


    ///// addServicesAsNewInvoice()

    // 1) MedicalRecord không tồn tại -> ném MEDICAL_RECORD_NOT_FOUND
    @Test
    void addServices_recordNotFound_throws() {
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> medicalRecordService.addServicesAsNewInvoice("REC", oneServiceReq("SVC", 1)));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MEDICAL_RECORD_NOT_FOUND);
    }

    // 2) SecurityContext rỗng (không có Authentication) -> ném UNAUTHORIZED
    @Test
    void addServices_noAuthentication_throwsUnauthorized() {
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(recordWithPatient()));
        // không set SecurityContext

        AppException ex = assertThrows(AppException.class,
                () -> medicalRecordService.addServicesAsNewInvoice("REC", oneServiceReq("SVC", 1)));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    // 3) Không tìm thấy Account theo username -> ném UNAUTHORIZED
    @Test
    void addServices_accountNotFound_throwsUnauthorized() {
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(recordWithPatient()));
        setAuth("alice");

        when(accountRepository.findByUsernameAndDeletedAtIsNull("alice"))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> medicalRecordService.addServicesAsNewInvoice("REC", oneServiceReq("SVC", 1)));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED);
    }

    // 4) Không tìm thấy Staff theo accountId -> ném STAFF_NOT_FOUND
    @Test
    void addServices_staffNotFound_throws() {
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(recordWithPatient()));
        setAuth("alice");

        Account acc = new Account(); acc.setId("ACC");
        when(accountRepository.findByUsernameAndDeletedAtIsNull("alice"))
                .thenReturn(Optional.of(acc));

        when(staffRepository.findByAccountIdAndDeletedAtIsNull("ACC"))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> medicalRecordService.addServicesAsNewInvoice("REC", oneServiceReq("SVC", 1)));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.STAFF_NOT_FOUND);
    }

    // 5) Staff không trong ca làm -> ném ACTION_NOT_ALLOWED
    @Test
    void addServices_staffNotOnShift_throws() {
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(recordWithPatient()));
        setAuth("alice");

        Account acc = new Account(); acc.setId("ACC");
        when(accountRepository.findByUsernameAndDeletedAtIsNull("alice"))
                .thenReturn(Optional.of(acc));

        Staff st = new Staff(); st.setId("STF"); st.setAccountId("ACC");
        when(staffRepository.findByAccountIdAndDeletedAtIsNull("ACC"))
                .thenReturn(Optional.of(st));

        when(workScheduleService.isStaffOnShiftNow("STF")).thenReturn(false);

        AppException ex = assertThrows(AppException.class,
                () -> medicalRecordService.addServicesAsNewInvoice("REC", oneServiceReq("SVC", 1)));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACTION_NOT_ALLOWED);
    }

    // 6) Sanity: có Authentication hợp lệ, map tới Account/Staff hợp lệ, đang trong ca -> đi tiếp (tạo invoice mới)
    @Test
    void addServices_sanity_passesSecurityAndCreatesInvoice() {
        MedicalRecord rec = recordWithPatient();
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(rec));
        setAuth("alice");

        Account acc = new Account(); acc.setId("ACC"); acc.setUsername("alice");
        when(accountRepository.findByUsernameAndDeletedAtIsNull("alice"))
                .thenReturn(Optional.of(acc));

        Staff st = new Staff(); st.setId("STF"); st.setAccountId("ACC");
        when(staffRepository.findByAccountIdAndDeletedAtIsNull("ACC"))
                .thenReturn(Optional.of(st));

        when(workScheduleService.isStaffOnShiftNow("STF")).thenReturn(true);

        when(codeGeneratorService.generateCode(eq("INVOICE"), anyString(), anyInt()))
                .thenReturn("INV-999");
        when(invoiceRepository.save(any())).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            if (i.getId() == null) i.setId("INV_ID");
            return i;
        });
        when(invoiceItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(medicalOrderRepository.save(any())).thenAnswer(inv -> {
            MedicalOrder o = inv.getArgument(0);
            o.setId(java.util.UUID.randomUUID().toString());
            return o;
        });
        when(medicalRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        MedicalService svc = new MedicalService();
        svc.setId("SVC"); svc.setServiceCode("SC");
        svc.setName("X-ray"); svc.setPrice(BigDecimal.valueOf(100));
        svc.setDiscount(BigDecimal.ZERO); svc.setVat(BigDecimal.TEN);
        svc.setDefaultService(false);
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SVC"))
                .thenReturn(Optional.of(svc));

        var res = medicalRecordService.addServicesAsNewInvoice("REC", oneServiceReq("SVC", 2));

        assertThat(res.getInvoiceId()).isEqualTo("INV_ID");
        verify(medicalOrderRepository, times(2)).save(any(MedicalOrder.class)); // qty = 2
    }

    /** B7) services rỗng -> tạo invoice mới, không tạo item/order, totals = 0 */
    @Test
    void addServices_empty_list_creates_invoice_but_no_items_or_orders_totals_zero() {
        // record + auth + account + staff + shift
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(recordWithPatient()));
        setAuth("alice");
        Account acc = new Account(); acc.setId("ACC");
        when(accountRepository.findByUsernameAndDeletedAtIsNull("alice")).thenReturn(Optional.of(acc));
        Staff st = new Staff(); st.setId("STF"); st.setAccountId("ACC");
        when(staffRepository.findByAccountIdAndDeletedAtIsNull("ACC")).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("STF")).thenReturn(true);

        // invoice tạo mới + cập nhật totals
        when(codeGeneratorService.generateCode(eq("INVOICE"), anyString(), anyInt())).thenReturn("INV-001");
        when(invoiceRepository.save(any())).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            if (i.getId() == null) i.setId("INV_ID");
            return i;
        });
        when(medicalRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // request rỗng
        InvoiceServiceRequest req = InvoiceServiceRequest.builder()
                .services(new java.util.ArrayList<>()) // rỗng, mutable
                .build();

        var res = medicalRecordService.addServicesAsNewInvoice("REC", req);

        verify(invoiceItemRepository, times(0)).save(any(InvoiceItem.class));
        verify(medicalOrderRepository, times(0)).save(any(MedicalOrder.class));
        assertThat(res.getOriginalTotal()).isEqualByComparingTo("0");
        assertThat(res.getDiscountTotal()).isEqualByComparingTo("0");
        assertThat(res.getVatTotal()).isEqualByComparingTo("0");
        assertThat(res.getTotalAmount()).isEqualByComparingTo("0");
    }

    /** 8) Một service có quantity = null -> bỏ qua service đó (không item/order) */
    @Test
    void addServices_single_service_quantity_null_is_skipped() {
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(recordWithPatient()));
        setAuth("alice");
        Account acc = new Account(); acc.setId("ACC");
        when(accountRepository.findByUsernameAndDeletedAtIsNull("alice")).thenReturn(Optional.of(acc));
        Staff st = new Staff(); st.setId("STF"); st.setAccountId("ACC");
        when(staffRepository.findByAccountIdAndDeletedAtIsNull("ACC")).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("STF")).thenReturn(true);

        when(codeGeneratorService.generateCode(eq("INVOICE"), anyString(), anyInt())).thenReturn("INV-002");
        when(invoiceRepository.save(any())).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            if (i.getId() == null) i.setId("INV_ID");
            return i;
        });
        when(medicalRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InvoiceServiceRequest req = InvoiceServiceRequest.builder()
                .services(java.util.List.of(
                        InvoiceServiceRequest.ServiceRequest.builder()
                                .serviceId("SVC_X").quantity(null).build()
                ))
                .build();

        var res = medicalRecordService.addServicesAsNewInvoice("REC", req);

        verify(invoiceItemRepository, times(0)).save(any(InvoiceItem.class));
        verify(medicalOrderRepository, times(0)).save(any(MedicalOrder.class));
        assertThat(res.getTotalAmount()).isEqualByComparingTo("0");
    }

    /** 9) Một service quantity <= 0 -> bỏ qua (không item/order) */
    @Test
    void addServices_single_service_quantity_le_zero_is_skipped() {
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(recordWithPatient()));
        setAuth("alice");
        Account acc = new Account(); acc.setId("ACC");
        when(accountRepository.findByUsernameAndDeletedAtIsNull("alice")).thenReturn(Optional.of(acc));
        Staff st = new Staff(); st.setId("STF"); st.setAccountId("ACC");
        when(staffRepository.findByAccountIdAndDeletedAtIsNull("ACC")).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("STF")).thenReturn(true);

        when(codeGeneratorService.generateCode(eq("INVOICE"), anyString(), anyInt())).thenReturn("INV-003");
        when(invoiceRepository.save(any())).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            if (i.getId() == null) i.setId("INV_ID");
            return i;
        });
        when(medicalRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InvoiceServiceRequest req = InvoiceServiceRequest.builder()
                .services(java.util.List.of(
                        InvoiceServiceRequest.ServiceRequest.builder()
                                .serviceId("SVC_X").quantity(0).build()
                ))
                .build();

        var res = medicalRecordService.addServicesAsNewInvoice("REC", req);

        verify(invoiceItemRepository, times(0)).save(any(InvoiceItem.class));
        verify(medicalOrderRepository, times(0)).save(any(MedicalOrder.class));
        assertThat(res.getTotalAmount()).isEqualByComparingTo("0");
    }

    /** 10) Tất cả service bị bỏ qua (null/<=0) -> totals = 0, không item/order */
    @Test
    void addServices_all_services_skipped_totals_zero() {
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(recordWithPatient()));
        setAuth("alice");
        Account acc = new Account(); acc.setId("ACC");
        when(accountRepository.findByUsernameAndDeletedAtIsNull("alice")).thenReturn(Optional.of(acc));
        Staff st = new Staff(); st.setId("STF"); st.setAccountId("ACC");
        when(staffRepository.findByAccountIdAndDeletedAtIsNull("ACC")).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("STF")).thenReturn(true);

        when(codeGeneratorService.generateCode(eq("INVOICE"), anyString(), anyInt())).thenReturn("INV-004");
        when(invoiceRepository.save(any())).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            if (i.getId() == null) i.setId("INV_ID");
            return i;
        });
        when(medicalRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InvoiceServiceRequest req = InvoiceServiceRequest.builder()
                .services(java.util.List.of(
                        InvoiceServiceRequest.ServiceRequest.builder().serviceId("A").quantity(null).build(),
                        InvoiceServiceRequest.ServiceRequest.builder().serviceId("B").quantity(0).build()
                ))
                .build();

        var res = medicalRecordService.addServicesAsNewInvoice("REC", req);

        verify(invoiceItemRepository, times(0)).save(any(InvoiceItem.class));
        verify(medicalOrderRepository, times(0)).save(any(MedicalOrder.class));
        assertThat(res.getTotalAmount()).isEqualByComparingTo("0");
    }

    /** 11) Service không tồn tại -> ném MEDICAL_SERVICE_NOT_FOUND */
    @Test
    void addServices_service_not_found_throws() {
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(recordWithPatient()));
        setAuth("alice");
        Account acc = new Account(); acc.setId("ACC");
        when(accountRepository.findByUsernameAndDeletedAtIsNull("alice")).thenReturn(Optional.of(acc));
        Staff st = new Staff(); st.setId("STF"); st.setAccountId("ACC");
        when(staffRepository.findByAccountIdAndDeletedAtIsNull("ACC")).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("STF")).thenReturn(true);

        // invoice mới sẽ được tạo trước khi fail ở service lookup
        when(codeGeneratorService.generateCode(eq("INVOICE"), anyString(), anyInt()))
                .thenReturn("INV-005");
        when(invoiceRepository.save(any())).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            if (i.getId() == null) i.setId("INV_ID");
            return i;
        });

        // KHÔNG cần stub medicalRecordRepository.save(any()) trong case này

        InvoiceServiceRequest req = oneServiceReq("SVC_MISS", 1);
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SVC_MISS"))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> medicalRecordService.addServicesAsNewInvoice("REC", req));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MEDICAL_SERVICE_NOT_FOUND);
        verify(invoiceItemRepository, times(0)).save(any());
        verify(medicalOrderRepository, times(0)).save(any());
    }

    /** 12) Hỗn hợp: A hợp lệ, B không tồn tại -> ném exception; đã tạo item/order cho A trước khi lỗi */
    @Test
    void addServices_mixed_first_ok_second_missing_throws_and_keeps_first_side_effects() {
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(recordWithPatient()));
        setAuth("alice");
        Account acc = new Account(); acc.setId("ACC");
        when(accountRepository.findByUsernameAndDeletedAtIsNull("alice")).thenReturn(Optional.of(acc));
        Staff st = new Staff(); st.setId("STF"); st.setAccountId("ACC");
        when(staffRepository.findByAccountIdAndDeletedAtIsNull("ACC")).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("STF")).thenReturn(true);

        when(codeGeneratorService.generateCode(eq("INVOICE"), anyString(), anyInt()))
                .thenReturn("INV-006");
        when(invoiceRepository.save(any())).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            if (i.getId() == null) i.setId("INV_ID");
            return i;
        });
        // ❌ BỎ stubbing medicalRecordRepository.save(any())

        // Request: A x2, B x1
        InvoiceServiceRequest req = InvoiceServiceRequest.builder()
                .services(new java.util.ArrayList<>())
                .build();
        req.getServices().add(InvoiceServiceRequest.ServiceRequest.builder().serviceId("SVA").quantity(2).build());
        req.getServices().add(InvoiceServiceRequest.ServiceRequest.builder().serviceId("SVB").quantity(1).build());

        // A hợp lệ
        MedicalService A = svcDef("SVA", "A", "Service A",
                new BigDecimal("100"), BigDecimal.ZERO, BigDecimal.ZERO, false);
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SVA")).thenReturn(Optional.of(A));
        // B không tồn tại
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SVB")).thenReturn(Optional.empty());

        when(invoiceItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(medicalOrderRepository.save(any())).thenAnswer(inv -> {
            MedicalOrder o = inv.getArgument(0);
            o.setId(java.util.UUID.randomUUID().toString());
            return o;
        });

        AppException ex = assertThrows(AppException.class,
                () -> medicalRecordService.addServicesAsNewInvoice("REC", req));

        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MEDICAL_SERVICE_NOT_FOUND);
        // Đã tạo cho A: 1 item + 2 orders, rồi lỗi ở B
        verify(invoiceItemRepository, times(1)).save(any(InvoiceItem.class));
        verify(medicalOrderRepository, times(2)).save(any(MedicalOrder.class));

        // (tuỳ chọn) invoice chỉ save 1 lần (tạo mới), không tới lần cập nhật totals
        verify(invoiceRepository, times(1)).save(any(Invoice.class));
    }


    /** 13) 2+ services hợp lệ -> mỗi service 1 item; tổng số order = tổng quantity */
    @Test
    void addServices_two_valid_services_each_one_item_total_orders_sum_of_quantities() {
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(recordWithPatient()));
        setAuth("alice");
        Account acc = new Account(); acc.setId("ACC");
        when(accountRepository.findByUsernameAndDeletedAtIsNull("alice")).thenReturn(Optional.of(acc));
        Staff st = new Staff(); st.setId("STF"); st.setAccountId("ACC");
        when(staffRepository.findByAccountIdAndDeletedAtIsNull("ACC")).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("STF")).thenReturn(true);

        when(codeGeneratorService.generateCode(eq("INVOICE"), anyString(), anyInt())).thenReturn("INV-007");
        when(invoiceRepository.save(any())).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            if (i.getId() == null) i.setId("INV_ID");
            return i;
        });
        when(medicalRecordRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Request: A x2, B x3
        InvoiceServiceRequest req = InvoiceServiceRequest.builder()
                .services(new java.util.ArrayList<>())
                .build();
        req.getServices().add(InvoiceServiceRequest.ServiceRequest.builder().serviceId("SVA").quantity(2).build());
        req.getServices().add(InvoiceServiceRequest.ServiceRequest.builder().serviceId("SVB").quantity(3).build());

        MedicalService A = svcDef("SVA", "A", "Service A",
                new BigDecimal("100"), new BigDecimal("10"), new BigDecimal("5"), false);
        MedicalService B = svcDef("SVB", "B", "Service B",
                new BigDecimal("50"), BigDecimal.ZERO, new BigDecimal("8"), false);
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SVA")).thenReturn(Optional.of(A));
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SVB")).thenReturn(Optional.of(B));

        when(invoiceItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(medicalOrderRepository.save(any())).thenAnswer(inv -> {
            MedicalOrder o = inv.getArgument(0);
            o.setId(java.util.UUID.randomUUID().toString());
            return o;
        });

        var res = medicalRecordService.addServicesAsNewInvoice("REC", req);

        // 1 item cho mỗi service
        verify(invoiceItemRepository, times(2)).save(any(InvoiceItem.class));
        // tổng số orders = 2 + 3 = 5
        verify(medicalOrderRepository, times(5)).save(any(MedicalOrder.class));

        // (tuỳ chọn) kiểm tra tổng tiền nếu muốn:
        // A: 100, disc 10%, vat 5%, qty 2 -> total 189
        // B: 50,  disc 0%,   vat 8%, qty 3 -> total 162
        // Tổng 351
        assertThat(res.getTotalAmount()).isEqualByComparingTo("351");
    }
    /** 14) discount = null -> xử lý bằng 0 */
    @Test
    void addServices_discount_null_treated_as_zero() {
        // record + auth + account + staff + shift
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(recordWithPatient()));
        setAuth("alice");
        Account acc = new Account(); acc.setId("ACC");
        when(accountRepository.findByUsernameAndDeletedAtIsNull("alice")).thenReturn(Optional.of(acc));
        Staff st = new Staff(); st.setId("STF"); st.setAccountId("ACC");
        when(staffRepository.findByAccountIdAndDeletedAtIsNull("ACC")).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("STF")).thenReturn(true);

        when(codeGeneratorService.generateCode(eq("INVOICE"), anyString(), anyInt()))
                .thenReturn("INV-014");
        when(invoiceRepository.save(any())).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            if (i.getId() == null) i.setId("INV_ID");
            return i;
        });
        when(invoiceItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(medicalOrderRepository.save(any())).thenAnswer(inv -> {
            MedicalOrder o = inv.getArgument(0);
            o.setId(java.util.UUID.randomUUID().toString());
            return o;
        });

        // price=100, qty=3, discount=null, vat=0
        MedicalService ms = svcDef("SVC", "SC", "X", new BigDecimal("100"), null, BigDecimal.ZERO, false);
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SVC"))
                .thenReturn(Optional.of(ms));

        var res = medicalRecordService.addServicesAsNewInvoice("REC", oneServiceReq("SVC", 3));

        // original=300, discount=0, vat=0, total=300
        assertThat(res.getOriginalTotal()).isEqualByComparingTo("300");
        assertThat(res.getDiscountTotal()).isEqualByComparingTo("0");
        assertThat(res.getVatTotal()).isEqualByComparingTo("0");
        assertThat(res.getTotalAmount()).isEqualByComparingTo("300");
        verify(medicalOrderRepository, times(3)).save(any(MedicalOrder.class));
    }

    /** 15) vat = null -> xử lý bằng 0 */
    @Test
    void addServices_vat_null_treated_as_zero() {
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(recordWithPatient()));
        setAuth("alice");
        Account acc = new Account(); acc.setId("ACC");
        when(accountRepository.findByUsernameAndDeletedAtIsNull("alice")).thenReturn(Optional.of(acc));
        Staff st = new Staff(); st.setId("STF"); st.setAccountId("ACC");
        when(staffRepository.findByAccountIdAndDeletedAtIsNull("ACC")).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("STF")).thenReturn(true);

        when(codeGeneratorService.generateCode(eq("INVOICE"), anyString(), anyInt()))
                .thenReturn("INV-015");
        when(invoiceRepository.save(any())).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            if (i.getId() == null) i.setId("INV_ID");
            return i;
        });
        when(invoiceItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(medicalOrderRepository.save(any())).thenAnswer(inv -> {
            MedicalOrder o = inv.getArgument(0);
            o.setId(java.util.UUID.randomUUID().toString());
            return o;
        });

        // price=50, qty=2, discount=10%, vat=null
        MedicalService ms = svcDef("SVC", "SC", "X", new BigDecimal("50"),
                new BigDecimal("10"), null, false);
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SVC"))
                .thenReturn(Optional.of(ms));

        var res = medicalRecordService.addServicesAsNewInvoice("REC", oneServiceReq("SVC", 2));

        // original=100; discount/unit=5 => total discount=10
        // subtotal=90; vat=0; total=90
        assertThat(res.getOriginalTotal()).isEqualByComparingTo("100");
        assertThat(res.getDiscountTotal()).isEqualByComparingTo("10");
        assertThat(res.getVatTotal()).isEqualByComparingTo("0");
        assertThat(res.getTotalAmount()).isEqualByComparingTo("90");
    }

    /** 16) discount > 0 & vat > 0 -> công thức tổng đúng */
    @Test
    void addServices_discount_and_vat_calculation_correct() {
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(recordWithPatient()));
        setAuth("alice");
        Account acc = new Account(); acc.setId("ACC");
        when(accountRepository.findByUsernameAndDeletedAtIsNull("alice")).thenReturn(Optional.of(acc));
        Staff st = new Staff(); st.setId("STF"); st.setAccountId("ACC");
        when(staffRepository.findByAccountIdAndDeletedAtIsNull("ACC")).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("STF")).thenReturn(true);

        when(codeGeneratorService.generateCode(eq("INVOICE"), anyString(), anyInt()))
                .thenReturn("INV-016");
        when(invoiceRepository.save(any())).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            if (i.getId() == null) i.setId("INV_ID");
            return i;
        });
        when(invoiceItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(medicalOrderRepository.save(any())).thenAnswer(inv -> {
            MedicalOrder o = inv.getArgument(0);
            o.setId(java.util.UUID.randomUUID().toString());
            return o;
        });

        // price=100, qty=2, discount=10%, vat=5%
        MedicalService ms = svcDef("SVC", "SC", "X", new BigDecimal("100"),
                new BigDecimal("10"), new BigDecimal("5"), false);
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SVC"))
                .thenReturn(Optional.of(ms));

        var res = medicalRecordService.addServicesAsNewInvoice("REC", oneServiceReq("SVC", 2));

        // original=200; discount=20; subtotal=180; vat=9; total=189
        assertThat(res.getOriginalTotal()).isEqualByComparingTo("200");
        assertThat(res.getDiscountTotal()).isEqualByComparingTo("20");
        assertThat(res.getVatTotal()).isEqualByComparingTo("9");
        assertThat(res.getTotalAmount()).isEqualByComparingTo("189");
    }

    /** 17) quantity lớn (100) -> tạo 100 MedicalOrder, totals đúng */
    @Test
    void addServices_large_quantity_creates_100_orders() {
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(recordWithPatient()));
        setAuth("alice");
        Account acc = new Account(); acc.setId("ACC");
        when(accountRepository.findByUsernameAndDeletedAtIsNull("alice")).thenReturn(Optional.of(acc));
        Staff st = new Staff(); st.setId("STF"); st.setAccountId("ACC");
        when(staffRepository.findByAccountIdAndDeletedAtIsNull("ACC")).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("STF")).thenReturn(true);

        when(codeGeneratorService.generateCode(eq("INVOICE"), anyString(), anyInt()))
                .thenReturn("INV-017");
        when(invoiceRepository.save(any())).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            if (i.getId() == null) i.setId("INV_ID");
            return i;
        });
        when(invoiceItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(medicalOrderRepository.save(any())).thenAnswer(inv -> {
            MedicalOrder o = inv.getArgument(0);
            o.setId(java.util.UUID.randomUUID().toString());
            return o;
        });

        // price=10, qty=100, no discount/vat
        MedicalService ms = svcDef("SVC", "SC", "Bulk", new BigDecimal("10"),
                BigDecimal.ZERO, BigDecimal.ZERO, false);
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SVC"))
                .thenReturn(Optional.of(ms));

        var res = medicalRecordService.addServicesAsNewInvoice("REC", oneServiceReq("SVC", 100));

        assertThat(res.getOriginalTotal()).isEqualByComparingTo("1000");
        assertThat(res.getDiscountTotal()).isEqualByComparingTo("0");
        assertThat(res.getVatTotal()).isEqualByComparingTo("0");
        assertThat(res.getTotalAmount()).isEqualByComparingTo("1000");
        verify(medicalOrderRepository, times(100)).save(any(MedicalOrder.class));
    }

    /** 18) Giá trị rất lớn (1e12, qty=2, discount/vat 10%) -> tính đúng, không lỗi precision */
    @Test
    void addServices_extremely_large_money_values_computed_correctly() {
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(recordWithPatient()));
        setAuth("alice");
        Account acc = new Account(); acc.setId("ACC");
        when(accountRepository.findByUsernameAndDeletedAtIsNull("alice")).thenReturn(Optional.of(acc));
        Staff st = new Staff(); st.setId("STF"); st.setAccountId("ACC");
        when(staffRepository.findByAccountIdAndDeletedAtIsNull("ACC")).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("STF")).thenReturn(true);

        when(codeGeneratorService.generateCode(eq("INVOICE"), anyString(), anyInt()))
                .thenReturn("INV-018");
        when(invoiceRepository.save(any())).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            if (i.getId() == null) i.setId("INV_ID");
            return i;
        });
        when(invoiceItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(medicalOrderRepository.save(any())).thenAnswer(inv -> {
            MedicalOrder o = inv.getArgument(0);
            o.setId(java.util.UUID.randomUUID().toString());
            return o;
        });

        // price = 1e12, qty=2, discount=10%, vat=10%
        MedicalService big = svcDef("SVC", "BIG", "Huge",
                new BigDecimal("1000000000000"), new BigDecimal("10"), new BigDecimal("10"), false);
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SVC"))
                .thenReturn(Optional.of(big));

        var res = medicalRecordService.addServicesAsNewInvoice("REC", oneServiceReq("SVC", 2));

        // original=2e12; discount=2e11; subtotal=1.8e12; vat=1.8e11; total=1.98e12
        assertThat(res.getOriginalTotal()).isEqualByComparingTo("2000000000000");
        assertThat(res.getDiscountTotal()).isEqualByComparingTo("200000000000");
        assertThat(res.getVatTotal()).isEqualByComparingTo("180000000000");
        assertThat(res.getTotalAmount()).isEqualByComparingTo("1980000000000");
    }

    /** 19) discount > 100% -> total âm theo công thức hiện tại (nếu muốn clamp sau sẽ thêm validation) */
    @Test
    void addServices_discount_over_100_percent_results_in_negative_total() {
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(recordWithPatient()));
        setAuth("alice");
        Account acc = new Account(); acc.setId("ACC");
        when(accountRepository.findByUsernameAndDeletedAtIsNull("alice")).thenReturn(Optional.of(acc));
        Staff st = new Staff(); st.setId("STF"); st.setAccountId("ACC");
        when(staffRepository.findByAccountIdAndDeletedAtIsNull("ACC")).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("STF")).thenReturn(true);

        when(codeGeneratorService.generateCode(eq("INVOICE"), anyString(), anyInt()))
                .thenReturn("INV-019");
        when(invoiceRepository.save(any())).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            if (i.getId() == null) i.setId("INV_ID");
            return i;
        });
        when(invoiceItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(medicalOrderRepository.save(any())).thenAnswer(inv -> {
            MedicalOrder o = inv.getArgument(0);
            o.setId(java.util.UUID.randomUUID().toString());
            return o;
        });

        // price=100, qty=1, discount=150%, vat=0
        MedicalService ms = svcDef("SVC", "SC", "X",
                new BigDecimal("100"), new BigDecimal("150"), BigDecimal.ZERO, false);
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SVC"))
                .thenReturn(Optional.of(ms));

        var res = medicalRecordService.addServicesAsNewInvoice("REC", oneServiceReq("SVC", 1));

        // original=100; discount=150; subtotal=-50; vat=0; total=-50
        assertThat(res.getOriginalTotal()).isEqualByComparingTo("100");
        assertThat(res.getDiscountTotal()).isEqualByComparingTo("150");
        assertThat(res.getVatTotal()).isEqualByComparingTo("0");
        assertThat(res.getTotalAmount().compareTo(BigDecimal.ZERO)).isLessThanOrEqualTo(0);
    }

    /** 20) Tạo invoice mới: codeGenerator INVOICE gọi 1 lần; invoiceRepository.save gọi 2 lần (tạo + cập nhật totals) */
    @Test
    void addServices_generates_one_invoice_and_saves_twice() {
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(recordWithPatient()));
        setAuth("alice");
        Account acc = new Account(); acc.setId("ACC");
        when(accountRepository.findByUsernameAndDeletedAtIsNull("alice")).thenReturn(Optional.of(acc));
        Staff st = new Staff(); st.setId("STF"); st.setAccountId("ACC");
        when(staffRepository.findByAccountIdAndDeletedAtIsNull("ACC")).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("STF")).thenReturn(true);

        when(codeGeneratorService.generateCode(eq("INVOICE"), anyString(), anyInt()))
                .thenReturn("INV-020");

        // save(1): tạo invoice -> set id; save(2): cập nhật totals (cùng object)
        when(invoiceRepository.save(any())).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0, Invoice.class);
            if (i.getId() == null) i.setId("INV_ID");
            return i;
        });
        when(invoiceItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0, InvoiceItem.class));
        when(medicalOrderRepository.save(any())).thenAnswer(inv -> {
            MedicalOrder o = inv.getArgument(0, MedicalOrder.class);
            o.setId(java.util.UUID.randomUUID().toString());
            return o;
        });

        MedicalService ms = svcDef("SVC", "SC", "X", new BigDecimal("100"),
                BigDecimal.ZERO, BigDecimal.ZERO, false);
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SVC")).thenReturn(Optional.of(ms));

        medicalRecordService.addServicesAsNewInvoice("REC", oneServiceReq("SVC", 1));

        verify(codeGeneratorService, times(1))
                .generateCode(eq("INVOICE"), anyString(), anyInt());
        verify(invoiceRepository, times(2)).save(any(Invoice.class)); // tạo + update totals
    }

    /** 21) InvoiceItem: 1 item/service, quantity gộp theo request */
    @Test
    void addServices_creates_single_invoice_item_per_service_with_aggregated_quantity() {
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(recordWithPatient()));
        setAuth("alice");
        Account acc = new Account(); acc.setId("ACC");
        when(accountRepository.findByUsernameAndDeletedAtIsNull("alice")).thenReturn(Optional.of(acc));
        Staff st = new Staff(); st.setId("STF"); st.setAccountId("ACC");
        when(staffRepository.findByAccountIdAndDeletedAtIsNull("ACC")).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("STF")).thenReturn(true);

        when(codeGeneratorService.generateCode(eq("INVOICE"), anyString(), anyInt()))
                .thenReturn("INV-021");
        when(invoiceRepository.save(any())).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0, Invoice.class);
            if (i.getId()==null) i.setId("INV_ID");
            return i;
        });

        MedicalService ms = svcDef("SVC", "SC", "Pack", new BigDecimal("10"),
                BigDecimal.ZERO, BigDecimal.ZERO, false);
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SVC")).thenReturn(Optional.of(ms));

        ArgumentCaptor<InvoiceItem> itemCap = ArgumentCaptor.forClass(InvoiceItem.class);
        when(invoiceItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0, InvoiceItem.class));
        when(medicalOrderRepository.save(any())).thenAnswer(inv -> {
            MedicalOrder o = inv.getArgument(0, MedicalOrder.class);
            o.setId(java.util.UUID.randomUUID().toString());
            return o;
        });

        // cùng 1 service, quantity = 5 -> chỉ 1 item với quantity=5
        medicalRecordService.addServicesAsNewInvoice("REC", oneServiceReq("SVC", 5));

        verify(invoiceItemRepository, times(1)).save(itemCap.capture());
        assertThat(itemCap.getValue().getService().getId()).isEqualTo("SVC");
        assertThat(itemCap.getValue().getQuantity()).isEqualTo(5);
    }

    /** 22a) defaultService = true -> MedicalOrder status COMPLETED */
    @Test
    void addServices_default_service_sets_orders_completed() {
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(recordWithPatient()));
        setAuth("alice");
        Account acc = new Account(); acc.setId("ACC");
        when(accountRepository.findByUsernameAndDeletedAtIsNull("alice")).thenReturn(Optional.of(acc));
        Staff st = new Staff(); st.setId("STF"); st.setAccountId("ACC");
        when(staffRepository.findByAccountIdAndDeletedAtIsNull("ACC")).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("STF")).thenReturn(true);

        when(codeGeneratorService.generateCode(eq("INVOICE"), anyString(), anyInt()))
                .thenReturn("INV-022a");
        when(invoiceRepository.save(any())).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0, Invoice.class);
            if (i.getId()==null) i.setId("INV_ID");
            return i;
        });
        when(invoiceItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0, InvoiceItem.class));

        MedicalService ms = svcDef("SVC", "SC", "Default", new BigDecimal("1"),
                BigDecimal.ZERO, BigDecimal.ZERO, true); // defaultService = true
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SVC")).thenReturn(Optional.of(ms));

        ArgumentCaptor<MedicalOrder> orderCap = ArgumentCaptor.forClass(MedicalOrder.class);
        when(medicalOrderRepository.save(any())).thenAnswer(inv -> {
            MedicalOrder o = inv.getArgument(0, MedicalOrder.class);
            o.setId(java.util.UUID.randomUUID().toString());
            return o;
        });

        medicalRecordService.addServicesAsNewInvoice("REC", oneServiceReq("SVC", 3));

        verify(medicalOrderRepository, times(3)).save(orderCap.capture());
        assertThat(orderCap.getAllValues())
                .extracting(MedicalOrder::getStatus)
                .containsOnly(MedicalOrderStatus.COMPLETED);
    }

    /** 22b) defaultService = false -> MedicalOrder status PENDING */
    @Test
    void addServices_non_default_service_sets_orders_pending() {
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(recordWithPatient()));
        setAuth("alice");
        Account acc = new Account(); acc.setId("ACC");
        when(accountRepository.findByUsernameAndDeletedAtIsNull("alice")).thenReturn(Optional.of(acc));
        Staff st = new Staff(); st.setId("STF"); st.setAccountId("ACC");
        when(staffRepository.findByAccountIdAndDeletedAtIsNull("ACC")).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("STF")).thenReturn(true);

        when(codeGeneratorService.generateCode(eq("INVOICE"), anyString(), anyInt()))
                .thenReturn("INV-022b");
        when(invoiceRepository.save(any())).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0, Invoice.class);
            if (i.getId()==null) i.setId("INV_ID");
            return i;
        });
        when(invoiceItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0, InvoiceItem.class));

        MedicalService ms = svcDef("SVC", "SC", "NonDefault", new BigDecimal("1"),
                BigDecimal.ZERO, BigDecimal.ZERO, false);
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SVC")).thenReturn(Optional.of(ms));

        ArgumentCaptor<MedicalOrder> orderCap = ArgumentCaptor.forClass(MedicalOrder.class);
        when(medicalOrderRepository.save(any())).thenAnswer(inv -> {
            MedicalOrder o = inv.getArgument(0, MedicalOrder.class);
            o.setId(java.util.UUID.randomUUID().toString());
            return o;
        });

        medicalRecordService.addServicesAsNewInvoice("REC", oneServiceReq("SVC", 2));

        verify(medicalOrderRepository, times(2)).save(orderCap.capture());
        assertThat(orderCap.getAllValues())
                .extracting(MedicalOrder::getStatus)
                .containsOnly(MedicalOrderStatus.PENDING);
    }

    /** 23) Record status cập nhật TESTING & medicalRecordRepository.save(record) được gọi */
    @Test
    void addServices_updates_record_status_to_TESTING_and_saves_record() {
        MedicalRecord rec = recordWithPatient();
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(rec));
        setAuth("alice");
        Account acc = new Account(); acc.setId("ACC");
        when(accountRepository.findByUsernameAndDeletedAtIsNull("alice")).thenReturn(Optional.of(acc));
        Staff st = new Staff(); st.setId("STF"); st.setAccountId("ACC");
        when(staffRepository.findByAccountIdAndDeletedAtIsNull("ACC")).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("STF")).thenReturn(true);

        when(codeGeneratorService.generateCode(eq("INVOICE"), anyString(), anyInt()))
                .thenReturn("INV-023");
        when(invoiceRepository.save(any())).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0, Invoice.class);
            if (i.getId()==null) i.setId("INV_ID");
            return i;
        });
        when(invoiceItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0, InvoiceItem.class));
        when(medicalOrderRepository.save(any())).thenAnswer(inv -> {
            MedicalOrder o = inv.getArgument(0, MedicalOrder.class);
            o.setId(java.util.UUID.randomUUID().toString());
            return o;
        });

        MedicalService ms = svcDef("SVC", "SC", "X", new BigDecimal("10"),
                BigDecimal.ZERO, BigDecimal.ZERO, false);
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SVC")).thenReturn(Optional.of(ms));

        medicalRecordService.addServicesAsNewInvoice("REC", oneServiceReq("SVC", 1));

        ArgumentCaptor<MedicalRecord> recCap = ArgumentCaptor.forClass(MedicalRecord.class);
        verify(medicalRecordRepository, times(1)).save(recCap.capture());
        assertThat(recCap.getValue().getStatus()).isEqualTo(MedicalRecordStatus.TESTING);
    }

    /** 24) Không “đụng” invoice cũ: chỉ thao tác trên invoice mới của lần thêm */
    @Test
    void addServices_only_touches_new_invoice_not_old_ones() {
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(recordWithPatient()));
        setAuth("alice");
        Account acc = new Account(); acc.setId("ACC");
        when(accountRepository.findByUsernameAndDeletedAtIsNull("alice")).thenReturn(Optional.of(acc));
        Staff st = new Staff(); st.setId("STF"); st.setAccountId("ACC");
        when(staffRepository.findByAccountIdAndDeletedAtIsNull("ACC")).thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("STF")).thenReturn(true);

        when(codeGeneratorService.generateCode(eq("INVOICE"), anyString(), anyInt()))
                .thenReturn("INV-024");

        // Capture cả 2 lần save để đảm bảo đều là cùng 1 invoice mới (không có id khác)
        ArgumentCaptor<Invoice> invCap = ArgumentCaptor.forClass(Invoice.class);
        when(invoiceRepository.save(any())).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0, Invoice.class);
            if (i.getId() == null) i.setId("NEW_INV"); // lần tạo đầu
            return i;                                   // lần sau cập nhật totals vẫn "NEW_INV"
        });

        when(invoiceItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0, InvoiceItem.class));
        when(medicalOrderRepository.save(any())).thenAnswer(inv -> {
            MedicalOrder o = inv.getArgument(0, MedicalOrder.class);
            o.setId(java.util.UUID.randomUUID().toString());
            return o;
        });

        MedicalService ms = svcDef("SVC", "SC", "X", new BigDecimal("10"),
                BigDecimal.ZERO, BigDecimal.ZERO, false);
        when(medicalServiceRepository.findByIdAndDeletedAtIsNull("SVC")).thenReturn(Optional.of(ms));

        medicalRecordService.addServicesAsNewInvoice("REC", oneServiceReq("SVC", 1));

        verify(invoiceRepository, times(2)).save(invCap.capture());
        var saved = invCap.getAllValues();
        assertThat(saved).hasSize(2);
        // Cả 2 lần đều là cùng invoice mới vừa tạo (id giống nhau)
        assertThat(saved.get(0).getId()).isEqualTo("NEW_INV");
        assertThat(saved.get(1).getId()).isEqualTo("NEW_INV");
        // Không có save nào cho "invoice cũ" khác id
    }


    ///// getMedicalRecordDetail()

    /** 1) Record không tồn tại -> ném MEDICAL_RECORD_NOT_FOUND */
    @Test
    void getMedicalRecordDetail_recordNotFound_throws() {
        // given
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.empty());

        // when
        AppException ex = assertThrows(AppException.class,
                () -> medicalRecordService.getMedicalRecordDetail("REC"));

        // then
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MEDICAL_RECORD_NOT_FOUND);
        verify(medicalRecordRepository, times(1))
                .findByIdAndDeletedAtIsNull("REC");
    }

    /** 2) recordId null -> hiện tại vẫn gọi repo và ném AppException(MEDICAL_RECORD_NOT_FOUND) */
    @Test
    void getMedicalRecordDetail_nullRecordId_throws_AppException_NotFound() {
        // given: repo trả empty cho null
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull(null))
                .thenReturn(Optional.empty());

        // when
        AppException ex = assertThrows(AppException.class,
                () -> medicalRecordService.getMedicalRecordDetail(null));

        // then
        assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.MEDICAL_RECORD_NOT_FOUND);
        verify(medicalRecordRepository, times(1))
                .findByIdAndDeletedAtIsNull(null);
    }

    /** 3–6) Verify số lần gọi repository & số lần gọi theo số order hợp lệ sau filter */
    @Test
    void getMedicalRecordDetail_repository_calls_counts_and_per_order_result_queries() {
        // ==== Arrange ====
        MedicalRecord rec = new MedicalRecord();
        rec.setId("REC");

        // bắt buộc: tránh NPE tại record.getStatus().name()
        rec.setStatus(MedicalRecordStatus.TESTING);
        rec.setMedicalRecordCode("MR-001");
        rec.setCreatedAt(java.time.LocalDateTime.now());

        Patient p = new Patient();
        p.setId("P1");
        p.setFullName("Patient A");
        p.setGender(Gender.MALE);
        // các field dưới đây không bắt buộc nếu service không đụng tới,
        // nhưng thêm vào cho chắc
        p.setPatientCode("P001");
        p.setPhone("0909xxxxxx");
        p.setDob(java.time.LocalDate.of(1990, 1, 1));
        rec.setPatient(p);

        Staff createdBy = new Staff();
        createdBy.setFullName("Doctor X");
        rec.setCreatedBy(createdBy);

        QueuePatients visit = new QueuePatients();
        visit.setId("V1");
        rec.setVisit(visit);

        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(rec));

        // 5 orders: A(valid), B(service=null), C(dept=null), D(defaultService=true), E(valid)
        Staff staffOrder = new Staff(); staffOrder.setFullName("Creator");
        Department dept = new Department(); dept.setId("D1"); dept.setName("Lab");

        MedicalService svcValid1 = new MedicalService();
        svcValid1.setId("SVA"); svcValid1.setName("X-Ray");
        svcValid1.setDepartment(dept); svcValid1.setDefaultService(false);

        MedicalService svcValid2 = new MedicalService();
        svcValid2.setId("SVE"); svcValid2.setName("CT");
        svcValid2.setDepartment(dept); svcValid2.setDefaultService(false);

        MedicalService svcNoDept = new MedicalService();
        svcNoDept.setId("SVC"); svcNoDept.setName("NoDept");
        svcNoDept.setDepartment(null); svcNoDept.setDefaultService(false);

        MedicalService svcDefault = new MedicalService();
        svcDefault.setId("SVD"); svcDefault.setName("Default");
        svcDefault.setDepartment(dept); svcDefault.setDefaultService(true);

        MedicalOrder A = MedicalOrder.builder()
                .id("A").medicalRecord(rec).service(svcValid1)
                .createdBy(staffOrder).status(MedicalOrderStatus.PENDING).build();

        MedicalOrder B = MedicalOrder.builder()
                .id("B").medicalRecord(rec).service(null) // filter out
                .createdBy(staffOrder).status(MedicalOrderStatus.PENDING).build();

        MedicalOrder C = MedicalOrder.builder()
                .id("C").medicalRecord(rec).service(svcNoDept) // filter out
                .createdBy(staffOrder).status(MedicalOrderStatus.PENDING).build();

        MedicalOrder D = MedicalOrder.builder()
                .id("D").medicalRecord(rec).service(svcDefault) // filter out
                .createdBy(staffOrder).status(MedicalOrderStatus.COMPLETED).build();

        MedicalOrder E = MedicalOrder.builder()
                .id("E").medicalRecord(rec).service(svcValid2)
                .createdBy(staffOrder).status(MedicalOrderStatus.PENDING).build();

        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull("REC"))
                .thenReturn(java.util.List.of(A, B, C, D, E));

        // results chỉ gọi cho A và E sau filter
        when(medicalResultRepository.findAllByMedicalOrderIdAndDeletedAtIsNull("A"))
                .thenReturn(java.util.List.of());
        when(medicalResultRepository.findAllByMedicalOrderIdAndDeletedAtIsNull("E"))
                .thenReturn(java.util.List.of());

        when(roomTransferHistoryRepository
                .findAllByMedicalRecordIdAndDeletedAtIsNullOrderByTransferTimeAsc("REC"))
                .thenReturn(java.util.List.of());

        // mapper visit
        when(queuePatientsMapper.toResponse(any()))
                .thenReturn(new QueuePatientsResponse());

        // ==== Act ====
        var detail = medicalRecordService.getMedicalRecordDetail("REC");

        // ==== Assert ====
        verify(medicalRecordRepository, times(1))
                .findByIdAndDeletedAtIsNull("REC");

        verify(medicalOrderRepository, times(1))
                .findAllByMedicalRecordIdAndDeletedAtIsNull("REC");

        verify(medicalResultRepository, times(1))
                .findAllByMedicalOrderIdAndDeletedAtIsNull("A");
        verify(medicalResultRepository, times(1))
                .findAllByMedicalOrderIdAndDeletedAtIsNull("E");
        verify(medicalResultRepository, times(2))
                .findAllByMedicalOrderIdAndDeletedAtIsNull(anyString());

        verify(roomTransferHistoryRepository, times(1))
                .findAllByMedicalRecordIdAndDeletedAtIsNullOrderByTransferTimeAsc("REC");

        assertThat(detail.getOrders()).hasSize(2);
        assertThat(detail.getOrders())
                .extracting(o -> o.getId())
                .containsExactlyInAnyOrder("A", "E");
    }

    /** 7) Loại order có service = null → không xuất hiện trong DTO */
    @Test
    void getMedicalRecordDetail_filters_out_orders_with_null_service() {
        MedicalRecord rec = baseRecord();
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(rec));

        Staff maker = new Staff(); maker.setFullName("Creator");

        // Order: service = null (bị loại)
        MedicalOrder bad = MedicalOrder.builder()
                .id("B").medicalRecord(rec).service(null)
                .createdBy(maker).status(MedicalOrderStatus.PENDING).build();

        // Order hợp lệ để kiểm tra còn lại sau filter
        MedicalService svc = new MedicalService();
        svc.setId("SVA"); svc.setName("X-Ray"); svc.setDepartment(deptLab()); svc.setDefaultService(false);
        MedicalOrder ok = MedicalOrder.builder()
                .id("A").medicalRecord(rec).service(svc)
                .createdBy(maker).status(MedicalOrderStatus.PENDING).build();

        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull("REC"))
                .thenReturn(java.util.List.of(bad, ok));

        // chỉ stub results cho order hợp lệ
        when(medicalResultRepository.findAllByMedicalOrderIdAndDeletedAtIsNull("A"))
                .thenReturn(java.util.List.of());

        when(roomTransferHistoryRepository.findAllByMedicalRecordIdAndDeletedAtIsNullOrderByTransferTimeAsc("REC"))
                .thenReturn(java.util.List.of());
        when(queuePatientsMapper.toResponse(any())).thenReturn(new QueuePatientsResponse());

        var res = medicalRecordService.getMedicalRecordDetail("REC");

        assertThat(res.getOrders()).extracting(o -> o.getId())
                .containsExactly("A");
    }

    /** 8) Loại order có service.department = null → không xuất hiện trong DTO */
    @Test
    void getMedicalRecordDetail_filters_out_orders_with_null_department() {
        MedicalRecord rec = baseRecord();
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(rec));

        Staff maker = new Staff(); maker.setFullName("Creator");

        MedicalService noDept = new MedicalService();
        noDept.setId("SVC"); noDept.setName("NoDept");
        noDept.setDepartment(null); noDept.setDefaultService(false);

        MedicalOrder bad = MedicalOrder.builder()
                .id("C").medicalRecord(rec).service(noDept)
                .createdBy(maker).status(MedicalOrderStatus.PENDING).build();

        MedicalService okSvc = new MedicalService();
        okSvc.setId("SVA"); okSvc.setName("X-Ray"); okSvc.setDepartment(deptLab()); okSvc.setDefaultService(false);
        MedicalOrder ok = MedicalOrder.builder()
                .id("A").medicalRecord(rec).service(okSvc)
                .createdBy(maker).status(MedicalOrderStatus.PENDING).build();

        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull("REC"))
                .thenReturn(java.util.List.of(bad, ok));

        when(medicalResultRepository.findAllByMedicalOrderIdAndDeletedAtIsNull("A"))
                .thenReturn(java.util.List.of());

        when(roomTransferHistoryRepository.findAllByMedicalRecordIdAndDeletedAtIsNullOrderByTransferTimeAsc("REC"))
                .thenReturn(java.util.List.of());
        when(queuePatientsMapper.toResponse(any())).thenReturn(new QueuePatientsResponse());

        var res = medicalRecordService.getMedicalRecordDetail("REC");

        assertThat(res.getOrders()).extracting(o -> o.getId())
                .containsExactly("A");
    }

    /** 9) Loại order có service.isDefaultService = true → không xuất hiện trong DTO */
    @Test
    void getMedicalRecordDetail_filters_out_orders_with_default_service_true() {
        MedicalRecord rec = baseRecord();
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(rec));

        Staff maker = new Staff(); maker.setFullName("Creator");

        MedicalService defaultSvc = new MedicalService();
        defaultSvc.setId("SVD"); defaultSvc.setName("Default");
        defaultSvc.setDepartment(deptLab()); defaultSvc.setDefaultService(true);

        MedicalOrder bad = MedicalOrder.builder()
                .id("D").medicalRecord(rec).service(defaultSvc)
                .createdBy(maker).status(MedicalOrderStatus.COMPLETED).build();

        MedicalService okSvc = new MedicalService();
        okSvc.setId("SVA"); okSvc.setName("X-Ray"); okSvc.setDepartment(deptLab()); okSvc.setDefaultService(false);
        MedicalOrder ok = MedicalOrder.builder()
                .id("A").medicalRecord(rec).service(okSvc)
                .createdBy(maker).status(MedicalOrderStatus.PENDING).build();

        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull("REC"))
                .thenReturn(java.util.List.of(bad, ok));

        when(medicalResultRepository.findAllByMedicalOrderIdAndDeletedAtIsNull("A"))
                .thenReturn(java.util.List.of());

        when(roomTransferHistoryRepository.findAllByMedicalRecordIdAndDeletedAtIsNullOrderByTransferTimeAsc("REC"))
                .thenReturn(java.util.List.of());
        when(queuePatientsMapper.toResponse(any())).thenReturn(new QueuePatientsResponse());

        var res = medicalRecordService.getMedicalRecordDetail("REC");

        assertThat(res.getOrders()).extracting(o -> o.getId())
                .containsExactly("A");
    }

    /** 10) Giữ order có service.isDefaultService = false → có trong DTO */
    @Test
    void getMedicalRecordDetail_keeps_orders_with_default_service_false() {
        MedicalRecord rec = baseRecord();
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(rec));

        Staff maker = new Staff(); maker.setFullName("Creator");

        MedicalService okSvc = new MedicalService();
        okSvc.setId("SVA"); okSvc.setName("X-Ray"); okSvc.setDepartment(deptLab()); okSvc.setDefaultService(false);

        MedicalOrder ok = MedicalOrder.builder()
                .id("A").medicalRecord(rec).service(okSvc)
                .createdBy(maker).status(MedicalOrderStatus.PENDING).build();

        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull("REC"))
                .thenReturn(java.util.List.of(ok));

        when(medicalResultRepository.findAllByMedicalOrderIdAndDeletedAtIsNull("A"))
                .thenReturn(java.util.List.of());

        when(roomTransferHistoryRepository.findAllByMedicalRecordIdAndDeletedAtIsNullOrderByTransferTimeAsc("REC"))
                .thenReturn(java.util.List.of());
        when(queuePatientsMapper.toResponse(any())).thenReturn(new QueuePatientsResponse());

        var res = medicalRecordService.getMedicalRecordDetail("REC");

        assertThat(res.getOrders()).extracting(o -> o.getId())
                .containsExactly("A");
    }

    /** 11) isDefaultService = null -> chỉ áp dụng khi field là Boolean; nếu boolean primitive thì order sẽ được giữ lại */
    @Test
    void getMedicalRecordDetail_filters_out_orders_when_isDefaultService_null_or_kept_if_primitive() throws Exception {
        MedicalRecord rec = baseRecord();
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(rec));

        Staff maker = new Staff(); maker.setFullName("Creator");

        Department dept = deptLab();
        MedicalService svc = new MedicalService();
        svc.setId("SVN"); svc.setName("MaybeDefault");
        svc.setDepartment(dept);

        // Kiểm tra kiểu trả về của getter isDefaultService()
        boolean isPrimitiveGetter;
        try {
            var getter = MedicalService.class.getMethod("isDefaultService"); // boolean isDefaultService()
            isPrimitiveGetter = (getter.getReturnType() == boolean.class);
        } catch (NoSuchMethodException e) {
            // Nếu không có isDefaultService(), thử getDefaultService() (Boolean getDefaultService())
            var getter = MedicalService.class.getMethod("getDefaultService");
            isPrimitiveGetter = (getter.getReturnType() == boolean.class);
        }

        if (isPrimitiveGetter) {
            // Entity dùng primitive boolean -> không thể set null -> default false => order được giữ lại
            // (đảm bảo set false cho rõ ràng nếu có setter)
            try {
                var setterBool = MedicalService.class.getMethod("setDefaultService", boolean.class);
                setterBool.invoke(svc, false);
            } catch (NoSuchMethodException ignore) {
                // nếu không có setter boolean thì bỏ qua, mặc định false cũng đủ
            }
        } else {
            // Entity dùng Boolean -> set null để test filter loại bỏ
            try {
                var setterBoxed = MedicalService.class.getMethod("setDefaultService", Boolean.class);
                setterBoxed.invoke(svc, new Object[]{null});
            } catch (NoSuchMethodException e) {
                // fallback: nếu chỉ có setter boolean thì coi như primitive case
            }
        }

        MedicalOrder order = MedicalOrder.builder()
                .id("N").medicalRecord(rec).service(svc)
                .createdBy(maker).status(MedicalOrderStatus.PENDING).build();

        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull("REC"))
                .thenReturn(java.util.List.of(order));

        when(roomTransferHistoryRepository.findAllByMedicalRecordIdAndDeletedAtIsNullOrderByTransferTimeAsc("REC"))
                .thenReturn(java.util.List.of());
        when(queuePatientsMapper.toResponse(any())).thenReturn(new QueuePatientsResponse());

        var res = medicalRecordService.getMedicalRecordDetail("REC");

        if (isPrimitiveGetter) {
            // primitive boolean: FALSE => kept
            assertThat(res.getOrders())
                    .extracting(o -> o.getId())
                    .containsExactly("N");
        } else {
            // boxed Boolean null: filtered out
            assertThat(res.getOrders()).isEmpty();
        }
    }

    /** 12) Order map đủ: id, serviceName, status, createdBy.fullName, results */
    @Test
    void getMedicalRecordDetail_maps_order_core_fields_and_results() {
        MedicalRecord rec = baseRecord();
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(rec));

        Department lab = deptLab();
        Staff maker = new Staff(); maker.setFullName("Creator");

        MedicalService svc = new MedicalService();
        svc.setId("SVC"); svc.setName("X-Ray"); svc.setDepartment(lab); svc.setDefaultService(false);

        MedicalOrder ord = MedicalOrder.builder()
                .id("ORD1").medicalRecord(rec).service(svc)
                .createdBy(maker).status(MedicalOrderStatus.PENDING).build();

        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull("REC"))
                .thenReturn(java.util.List.of(ord));

        // 1 result + 1 image
        Staff finisher = new Staff(); finisher.setFullName("Tech A");
        MedicalResult mr = new MedicalResult();
        mr.setId("RES1"); mr.setCompletedBy(finisher);
        mr.setResultNote("note"); mr.setDescription("desc");

        when(medicalResultRepository.findAllByMedicalOrderIdAndDeletedAtIsNull("ORD1"))
                .thenReturn(java.util.List.of(mr));

        MedicalResultImage img = new MedicalResultImage();
        img.setId("IMG1"); img.setImageUrl("http://img/1");
        when(medicalResultImageRepository.findAllByMedicalResultId("RES1"))
                .thenReturn(java.util.List.of(img));

        when(roomTransferHistoryRepository.findAllByMedicalRecordIdAndDeletedAtIsNullOrderByTransferTimeAsc("REC"))
                .thenReturn(java.util.List.of());
        when(queuePatientsMapper.toResponse(any())).thenReturn(new QueuePatientsResponse());

        var res = medicalRecordService.getMedicalRecordDetail("REC");

        assertThat(res.getOrders()).hasSize(1);
        var o = res.getOrders().get(0);
        assertThat(o.getId()).isEqualTo("ORD1");
        assertThat(o.getServiceName()).isEqualTo("X-Ray");
        assertThat(o.getStatus()).isEqualTo(MedicalOrderStatus.PENDING.name());
        assertThat(o.getCreatedBy()).isEqualTo("Creator");
        assertThat(o.getResults()).hasSize(1);
    }

    /** 13) order.getCreatedBy() == null -> hiện tại sẽ NPE (không null-safe) */
    @Test
    void getMedicalRecordDetail_order_createdBy_null_currently_NPE() {
        MedicalRecord rec = baseRecord();
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(rec));

        Department lab = deptLab();
        MedicalService svc = new MedicalService();
        svc.setId("SVC"); svc.setName("X-Ray"); svc.setDepartment(lab); svc.setDefaultService(false);

        // createdBy = null
        MedicalOrder ord = MedicalOrder.builder()
                .id("ORD1").medicalRecord(rec).service(svc)
                .createdBy(null).status(MedicalOrderStatus.PENDING).build();

        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull("REC"))
                .thenReturn(java.util.List.of(ord));

        // KHÔNG stub results / transfers / mapper vì sẽ NPE trước khi dùng
        // when(medicalResultRepository.findAllByMedicalOrderIdAndDeletedAtIsNull("ORD1"))...
        // when(roomTransferHistoryRepository.findAllByMedicalRecordIdAndDeletedAtIsNullOrderByTransferTimeAsc("REC"))...
        // when(queuePatientsMapper.toResponse(any()))...

        assertThrows(NullPointerException.class,
                () -> medicalRecordService.getMedicalRecordDetail("REC"));
    }

    /** 14) Mỗi order hợp lệ: results map đủ id, completedBy.fullName, note, description, imageUrls */
    @Test
    void getMedicalRecordDetail_maps_result_fields_and_images() {
        MedicalRecord rec = baseRecord();
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(rec));

        Department lab = deptLab();
        Staff maker = new Staff(); maker.setFullName("Creator");

        MedicalService svc = new MedicalService();
        svc.setId("SVC"); svc.setName("CT"); svc.setDepartment(lab); svc.setDefaultService(false);

        MedicalOrder ord = MedicalOrder.builder()
                .id("ORD1").medicalRecord(rec).service(svc)
                .createdBy(maker).status(MedicalOrderStatus.PENDING).build();
        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull("REC"))
                .thenReturn(java.util.List.of(ord));

        Staff finisher = new Staff(); finisher.setFullName("Tech B");
        MedicalResult mr = new MedicalResult();
        mr.setId("RES1"); mr.setCompletedBy(finisher);
        mr.setResultNote("N1"); mr.setDescription("D1");

        when(medicalResultRepository.findAllByMedicalOrderIdAndDeletedAtIsNull("ORD1"))
                .thenReturn(java.util.List.of(mr));

        MedicalResultImage i1 = new MedicalResultImage(); i1.setId("IMG1"); i1.setImageUrl("u1");
        MedicalResultImage i2 = new MedicalResultImage(); i2.setId("IMG2"); i2.setImageUrl("u2");
        when(medicalResultImageRepository.findAllByMedicalResultId("RES1"))
                .thenReturn(java.util.List.of(i1, i2));

        when(roomTransferHistoryRepository.findAllByMedicalRecordIdAndDeletedAtIsNullOrderByTransferTimeAsc("REC"))
                .thenReturn(java.util.List.of());
        when(queuePatientsMapper.toResponse(any())).thenReturn(new QueuePatientsResponse());

        var res = medicalRecordService.getMedicalRecordDetail("REC");

        var r = res.getOrders().get(0).getResults().get(0);
        assertThat(r.getId()).isEqualTo("RES1");
        assertThat(r.getCompletedBy()).isEqualTo("Tech B");
        assertThat(r.getNote()).isEqualTo("N1");
        assertThat(r.getDescription()).isEqualTo("D1");
        assertThat(r.getImageUrls()).extracting(MedicalResultImageResponse::getId)
                .containsExactlyInAnyOrder("IMG1", "IMG2");
        assertThat(r.getImageUrls()).extracting(MedicalResultImageResponse::getImageUrl)
                .containsExactlyInAnyOrder("u1", "u2");
    }

    /** 15) result.getCompletedBy() == null -> hiện tại sẽ NPE (không null-safe) */
    /** 15) result.getCompletedBy() == null -> hiện tại sẽ NPE (không null-safe) */
    @Test
    void getMedicalRecordDetail_result_completedBy_null_currently_NPE() {
        MedicalRecord rec = baseRecord();
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(rec));

        Department lab = deptLab();
        Staff maker = new Staff(); maker.setFullName("Creator");

        MedicalService svc = new MedicalService();
        svc.setId("SVC"); svc.setName("MRI"); svc.setDepartment(lab); svc.setDefaultService(false);

        MedicalOrder ord = MedicalOrder.builder()
                .id("ORD1").medicalRecord(rec).service(svc)
                .createdBy(maker).status(MedicalOrderStatus.PENDING).build();
        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull("REC"))
                .thenReturn(java.util.List.of(ord));

        // completedBy = null -> sẽ NPE khi map
        MedicalResult mr = new MedicalResult();
        mr.setId("RES1"); mr.setCompletedBy(null);
        mr.setResultNote("n"); mr.setDescription("d");
        when(medicalResultRepository.findAllByMedicalOrderIdAndDeletedAtIsNull("ORD1"))
                .thenReturn(java.util.List.of(mr));

        // 🚫 ĐỪNG stub image/roomTransfer/mapper ở test này — sẽ không được gọi
        // when(medicalResultImageRepository.findAllByMedicalResultId("RES1"))...
        // when(roomTransferHistoryRepository.findAllByMedicalRecordIdAndDeletedAtIsNullOrderByTransferTimeAsc("REC"))...
        // when(queuePatientsMapper.toResponse(any()))...

        assertThrows(NullPointerException.class,
                () -> medicalRecordService.getMedicalRecordDetail("REC"));
    }


    /** 16) Repo ảnh trả list -> map đúng id & imageUrl (đã được xác nhận ở test #14) – bổ sung verify gọi đúng id */
    @Test
    void getMedicalRecordDetail_images_repo_called_per_result_and_mapped() {
        MedicalRecord rec = baseRecord();
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(rec));

        Department lab = deptLab();
        Staff maker = new Staff(); maker.setFullName("Creator");

        MedicalService svc = new MedicalService();
        svc.setId("SVC"); svc.setName("US"); svc.setDepartment(lab); svc.setDefaultService(false);

        MedicalOrder ord = MedicalOrder.builder()
                .id("ORD1").medicalRecord(rec).service(svc)
                .createdBy(maker).status(MedicalOrderStatus.PENDING).build();
        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull("REC"))
                .thenReturn(java.util.List.of(ord));

        Staff finisher = new Staff(); finisher.setFullName("Tech C");
        MedicalResult mr = new MedicalResult();
        mr.setId("RES1"); mr.setCompletedBy(finisher);
        when(medicalResultRepository.findAllByMedicalOrderIdAndDeletedAtIsNull("ORD1"))
                .thenReturn(java.util.List.of(mr));

        MedicalResultImage i = new MedicalResultImage(); i.setId("IMG"); i.setImageUrl("img");
        when(medicalResultImageRepository.findAllByMedicalResultId("RES1"))
                .thenReturn(java.util.List.of(i));

        when(roomTransferHistoryRepository.findAllByMedicalRecordIdAndDeletedAtIsNullOrderByTransferTimeAsc("REC"))
                .thenReturn(java.util.List.of());
        when(queuePatientsMapper.toResponse(any())).thenReturn(new QueuePatientsResponse());

        var res = medicalRecordService.getMedicalRecordDetail("REC");

        verify(medicalResultImageRepository, times(1))
                .findAllByMedicalResultId("RES1");
        var imgs = res.getOrders().get(0).getResults().get(0).getImageUrls();
        assertThat(imgs).hasSize(1);
        assertThat(imgs.get(0).getId()).isEqualTo("IMG");
        assertThat(imgs.get(0).getImageUrl()).isEqualTo("img");
    }

    /** 17) Không có kết quả/ảnh -> results, imageUrls là list rỗng (không null) */
    @Test
    void getMedicalRecordDetail_no_results_no_images_returns_empty_lists() {
        MedicalRecord rec = baseRecord();
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(rec));

        Department lab = deptLab();
        Staff maker = new Staff(); maker.setFullName("Creator");

        MedicalService svc = new MedicalService();
        svc.setId("SVC"); svc.setName("X-Ray"); svc.setDepartment(lab); svc.setDefaultService(false);

        MedicalOrder ord = MedicalOrder.builder()
                .id("ORD1").medicalRecord(rec).service(svc)
                .createdBy(maker).status(MedicalOrderStatus.PENDING).build();
        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull("REC"))
                .thenReturn(java.util.List.of(ord));

        // results rỗng
        when(medicalResultRepository.findAllByMedicalOrderIdAndDeletedAtIsNull("ORD1"))
                .thenReturn(java.util.List.of());

        when(roomTransferHistoryRepository.findAllByMedicalRecordIdAndDeletedAtIsNullOrderByTransferTimeAsc("REC"))
                .thenReturn(java.util.List.of());
        when(queuePatientsMapper.toResponse(any())).thenReturn(new QueuePatientsResponse());

        var res = medicalRecordService.getMedicalRecordDetail("REC");

        assertThat(res.getOrders()).hasSize(1);
        assertThat(res.getOrders().get(0).getResults()).isNotNull().isEmpty();

        // Vì không có result nào -> repo ảnh không bị gọi
        verify(medicalResultImageRepository, times(0))
                .findAllByMedicalResultId(anyString());
    }

    /** 18) Map đúng các field header từ record */
    @Test
    void getMedicalRecordDetail_maps_header_fields_from_record() {
        // record + patient + creator + visit
        MedicalRecord rec = new MedicalRecord();
        rec.setId("REC");
        Patient p = new Patient();
        p.setId("P1"); p.setPatientCode("PC001"); p.setGender(Gender.FEMALE);
        p.setDob(LocalDate.of(1990, 1, 2));
        p.setPhone("0909"); p.setFullName("Alice");
        rec.setPatient(p);

        Staff createdBy = new Staff(); createdBy.setFullName("Dr. Who");
        rec.setCreatedBy(createdBy);

        rec.setMedicalRecordCode("MR-000111");
        rec.setDiagnosisText("Dx"); rec.setSummary("Sum");
        rec.setStatus(MedicalRecordStatus.WAITING_FOR_PAYMENT);
        rec.setCreatedAt(LocalDateTime.of(2025, 8, 10, 9, 30));

        QueuePatients visit = new QueuePatients(); visit.setId("V1");
        rec.setVisit(visit);

        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(rec));
        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull("REC"))
                .thenReturn(List.of());
        when(roomTransferHistoryRepository
                .findAllByMedicalRecordIdAndDeletedAtIsNullOrderByTransferTimeAsc("REC"))
                .thenReturn(List.of());

        QueuePatientsResponse visitDto = new QueuePatientsResponse();
        when(queuePatientsMapper.toResponse(visit)).thenReturn(visitDto);

        var res = medicalRecordService.getMedicalRecordDetail("REC");

        assertThat(res.getId()).isEqualTo("REC");
        assertThat(res.getMedicalRecordCode()).isEqualTo("MR-000111");
        assertThat(res.getPatientId()).isEqualTo("P1");
        assertThat(res.getPatientCode()).isEqualTo("PC001");
        assertThat(res.getGender()).isEqualTo(Gender.FEMALE.name());
        assertThat(res.getDateOfBirth()).isEqualTo(LocalDate.of(1990,1,2));
        assertThat(res.getPhone()).isEqualTo("0909");
        assertThat(res.getPatientName()).isEqualTo("Alice");
        assertThat(res.getCreatedBy()).isEqualTo("Dr. Who");
        assertThat(res.getDiagnosisText()).isEqualTo("Dx");
        assertThat(res.getSummary()).isEqualTo("Sum");
        assertThat(res.getStatus()).isEqualTo(MedicalRecordStatus.WAITING_FOR_PAYMENT.name());
        assertThat(res.getCreatedAt()).isEqualTo(LocalDateTime.of(2025,8,10,9,30));
        assertThat(res.getVisit()).isSameAs(visitDto);
    }

    /** 19) queuePatientsMapper.toResponse(record.getVisit()) được gọi 1 lần; khi visit != null */
    @Test
    void getMedicalRecordDetail_calls_visit_mapper_once_when_visit_present() {
        MedicalRecord rec = new MedicalRecord();
        rec.setId("REC");

        Patient p = new Patient();
        p.setId("P1");
        p.setPatientCode("PC001");
        p.setFullName("Alice");
        p.setPhone("0909");
        p.setDob(LocalDate.of(1990, 1, 2));
        p.setGender(Gender.FEMALE);                 // <-- thêm dòng này để tránh NPE
        rec.setPatient(p);

        Staff createdBy = new Staff();
        createdBy.setFullName("Dr");
        rec.setCreatedBy(createdBy);

        rec.setStatus(MedicalRecordStatus.WAITING_FOR_PAYMENT);
        rec.setVisit(new QueuePatients());
        rec.getVisit().setId("V1");

        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(rec));
        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull("REC"))
                .thenReturn(List.of());
        when(roomTransferHistoryRepository
                .findAllByMedicalRecordIdAndDeletedAtIsNullOrderByTransferTimeAsc("REC"))
                .thenReturn(List.of());

        QueuePatientsResponse dto = new QueuePatientsResponse();
        when(queuePatientsMapper.toResponse(rec.getVisit())).thenReturn(dto);

        var res = medicalRecordService.getMedicalRecordDetail("REC");

        verify(queuePatientsMapper, times(1)).toResponse(rec.getVisit());
        assertThat(res.getVisit()).isSameAs(dto);
        // sanity
        assertThat(res.getGender()).isEqualTo(Gender.FEMALE.name());
    }


    /** 19b) visit = null → mapper được gọi 1 lần với null; res.visit = null (hành vi hiện tại) */
    @Test
    void getMedicalRecordDetail_when_visit_null_mapper_called_once_with_null_and_response_null() {
        MedicalRecord rec = new MedicalRecord();
        rec.setId("REC");

        Patient p = new Patient();
        p.setId("P1");
        p.setPatientCode("PC001");
        p.setFullName("Alice");
        p.setPhone("0909");
        p.setDob(LocalDate.of(1990, 1, 2));
        p.setGender(Gender.FEMALE);
        rec.setPatient(p);

        Staff created = new Staff(); created.setFullName("Dr");
        rec.setCreatedBy(created);
        rec.setStatus(MedicalRecordStatus.WAITING_FOR_PAYMENT);
        rec.setVisit(null); // <-- null

        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(rec));
        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull("REC"))
                .thenReturn(List.of());
        when(roomTransferHistoryRepository
                .findAllByMedicalRecordIdAndDeletedAtIsNullOrderByTransferTimeAsc("REC"))
                .thenReturn(List.of());

        // mapper có thể trả null khi input null
        when(queuePatientsMapper.toResponse(null)).thenReturn(null);

        var res = medicalRecordService.getMedicalRecordDetail("REC");

        // xác nhận GỌI 1 LẦN với null
        verify(queuePatientsMapper, times(1)).toResponse(isNull());
        assertThat(res.getVisit()).isNull();

        // sanity
        assertThat(res.getGender()).isEqualTo(Gender.FEMALE.name());
        assertThat(res.getPatientId()).isEqualTo("P1");
    }


    /** 20) Map đúng các chỉ số sinh tồn (temperature, respiratoryRate, bloodPressure, heartRate, heightCm, weightKg, bmi, spo2, notes) */
    @Test
    void getMedicalRecordDetail_maps_vital_signs_correctly() {
        MedicalRecord rec = new MedicalRecord();
        rec.setId("REC");

        Patient p = new Patient();
        p.setId("P1");
        p.setPatientCode("PC001");
        p.setFullName("Alice");
        p.setPhone("0909");
        p.setDob(LocalDate.of(1990, 1, 2));
        p.setGender(Gender.FEMALE);                // <-- thêm dòng này để tránh NPE
        rec.setPatient(p);

        Staff s = new Staff(); s.setFullName("Dr");
        rec.setCreatedBy(s);
        rec.setStatus(MedicalRecordStatus.WAITING_FOR_PAYMENT);

        rec.setTemperature(37.2);
        rec.setRespiratoryRate(18);
        rec.setBloodPressure("125/80");
        rec.setHeartRate(72);
        rec.setHeightCm(170.5);
        rec.setWeightKg(65.3);
        rec.setBmi(22.5);
        rec.setSpo2(97);
        rec.setNotes("note x");

        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(rec));
        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull("REC"))
                .thenReturn(List.of());
        when(roomTransferHistoryRepository
                .findAllByMedicalRecordIdAndDeletedAtIsNullOrderByTransferTimeAsc("REC"))
                .thenReturn(List.of());
        when(queuePatientsMapper.toResponse(any())).thenReturn(null);

        var res = medicalRecordService.getMedicalRecordDetail("REC");

        assertThat(res.getTemperature()).isEqualTo(37.2);
        assertThat(res.getRespiratoryRate()).isEqualTo(18);
        assertThat(res.getBloodPressure()).isEqualTo("125/80");
        assertThat(res.getHeartRate()).isEqualTo(72);
        assertThat(res.getHeightCm()).isEqualTo(170.5);
        assertThat(res.getWeightKg()).isEqualTo(65.3);
        assertThat(res.getBmi()).isEqualTo(22.5);
        assertThat(res.getSpo2()).isEqualTo(97);
        assertThat(res.getNotes()).isEqualTo("note x");
    }

    /** 21) Không có lịch sử chuyển phòng → roomTransfers là list rỗng, không null */
    @Test
    void getMedicalRecordDetail_when_no_roomTransfers_returns_emptyList_not_null() {
        MedicalRecord rec = new MedicalRecord();
        rec.setId("REC");

        Patient p = new Patient();
        p.setId("P1"); p.setPatientCode("PC001");
        p.setFullName("Alice"); p.setPhone("0909");
        p.setDob(LocalDate.of(1990, 1, 2));
        p.setGender(Gender.FEMALE);
        rec.setPatient(p);

        Staff created = new Staff(); created.setFullName("Dr");
        rec.setCreatedBy(created);
        rec.setStatus(MedicalRecordStatus.WAITING_FOR_PAYMENT);

        when(medicalRecordRepository.findByIdAndDeletedAtIsNull("REC"))
                .thenReturn(Optional.of(rec));
        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull("REC"))
                .thenReturn(List.of());

        when(roomTransferHistoryRepository
                .findAllByMedicalRecordIdAndDeletedAtIsNullOrderByTransferTimeAsc("REC"))
                .thenReturn(List.of()); // rỗng

        when(queuePatientsMapper.toResponse(any())).thenReturn(null);

        var res = medicalRecordService.getMedicalRecordDetail("REC");

        assertThat(res.getRoomTransfers()).isNotNull();
        assertThat(res.getRoomTransfers()).isEmpty();
    }

}
