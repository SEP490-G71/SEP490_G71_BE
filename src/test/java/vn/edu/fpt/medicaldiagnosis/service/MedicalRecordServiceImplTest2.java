package vn.edu.fpt.medicaldiagnosis.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.fpt.medicaldiagnosis.dto.request.UpdateMedicalRecordRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.*;
import vn.edu.fpt.medicaldiagnosis.entity.*;
import vn.edu.fpt.medicaldiagnosis.enums.Gender;
import vn.edu.fpt.medicaldiagnosis.enums.MedicalOrderStatus;
import vn.edu.fpt.medicaldiagnosis.enums.MedicalRecordStatus;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.MedicalRecordMapper;
import vn.edu.fpt.medicaldiagnosis.mapper.SettingMapper;
import vn.edu.fpt.medicaldiagnosis.repository.*;
import vn.edu.fpt.medicaldiagnosis.service.impl.AccountServiceImpl;
import vn.edu.fpt.medicaldiagnosis.service.impl.MedicalRecordServiceImpl;
import vn.edu.fpt.medicaldiagnosis.service.impl.QueuePatientsServiceImpl;
import vn.edu.fpt.medicaldiagnosis.specification.MedicalRecordSpecification;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
public class MedicalRecordServiceImplTest2 {
    @Mock
    private MedicalRecordRepository medicalRecordRepository;
    @Mock
    private MedicalOrderRepository medicalOrderRepository;
    @Mock
    private MedicalResultRepository medicalResultRepository;

    @Mock
    private PatientRepository  patientRepository;

    @Mock
    private MedicalRecordMapper medicalRecordMapper;

    @Mock
    private RoomTransferHistoryRepository roomTransferHistoryRepository;

    @Mock
    private MedicalResultImageRepository medicalResultImageRepository;
    @Mock
    AccountRepository accountRepository;
    @Mock
    DepartmentRepository departmentRepository;
    @InjectMocks
    private MedicalRecordServiceImpl medicalRecordService;

    @InjectMocks
    private QueuePatientsServiceImpl queuePatientsService;

    @Mock
    private SettingRepository settingRepository; // repo mà SettingServiceImpl cần
    @Mock private SettingMapper settingMapper;
    @Mock private StaffRepository staffRepository;
    @Mock private WorkScheduleService workScheduleService;
    @Mock private AccountServiceImpl accountService;
    private MockedStatic<MedicalRecordSpecification> specStatic;
    @BeforeEach
    void openStatic() {
        specStatic = Mockito.mockStatic(MedicalRecordSpecification.class);
    }

    @AfterEach
    void closeStatic() {
        if (specStatic != null) specStatic.close();
    }

    // ===== Helpers dựng dữ liệu test tối thiểu =====
    private static Patient patient(String id, String code, String fullName, Gender gender, LocalDate dob) {
        Patient p = new Patient();
        p.setId(id);
        p.setPatientCode(code);
        p.setFullName(fullName);
        p.setGender(gender);
        p.setDob(dob);
        return p;
    }
    private static Staff staff(String id, String code, String fullName) {
        Staff s = new Staff();
        s.setId(id);
        s.setStaffCode(code);
        s.setFullName(fullName);
        return s;
    }
    private static AccountResponse accountResp(String id, String username, String... roleNames) {
        Set<RoleResponse> roles = Arrays.stream(roleNames)
                .map(rn -> RoleResponse.builder().name(rn).build())
                .collect(Collectors.toSet()); // ✅ đổi sang Set, không phải List
        return AccountResponse.builder()
                .id(id)
                .username(username)
                .roles(roles)
                .build();
    }

    // Tạo một MedicalRecord “đủ dữ liệu” dùng cho test PDF

    private static MedicalRecord record(String id, Staff creator) {
        MedicalRecord r = new MedicalRecord();
        r.setId(id);
        r.setCreatedBy(creator);
        return r;
    }
    private static MedicalRecord record(String id, MedicalRecordStatus status, QueuePatients v) {
        MedicalRecord r = new MedicalRecord();
        r.setId(id);
        r.setStatus(status);
        r.setVisit(v);
        return r;
    }


    private static MedicalOrder order(String id, String recordId) {
        MedicalOrder o = new MedicalOrder();
        o.setId(id);
        return o;
    }

    private static MedicalResult result(String id, String orderId, Staff completedBy) {
        MedicalResult res = new MedicalResult();
        res.setId(id);
        res.setCompletedBy(completedBy);
        return res;
    }

    // Entity Setting thật (khác SettingResponse)
    private static Setting settingEntity() {
        Setting s = new Setting();
        s.setHospitalName("FPT Hospital");
        s.setHospitalAddress("Hanoi");
        s.setHospitalPhone("0123456789");
        return s;
    }

    // Response trả ra từ mapper
    private static SettingResponse settingResponse() {
        return SettingResponse.builder()
                .hospitalName("FPT Hospital")
                .hospitalAddress("Hanoi")
                .hospitalPhone("0123456789")
                .build();
    }

    // Helpers cho phần Service-for-feedback
    private static MedicalOrder orderWithService(String orderId, String serviceId, String serviceName) {
        MedicalService sv = new MedicalService();
        sv.setId(serviceId);
        sv.setName(serviceName);
        MedicalOrder o = new MedicalOrder();
        o.setId(orderId);
        o.setService(sv);
        return o;
    }

    private static MedicalOrder orderWithNullService(String orderId) {
        MedicalOrder o = new MedicalOrder();
        o.setId(orderId);
        o.setService(null);
        return o;
    }

    private static MedicalOrder orderWithServiceIdNull(String orderId, String name) {
        MedicalService sv = new MedicalService();
        sv.setId(null);
        sv.setName(name);
        MedicalOrder o = new MedicalOrder();
        o.setId(orderId);
        o.setService(sv);
        return o;
    }

    private static MedicalOrder orderWithServiceNameNull(String orderId, String id) {
        MedicalService sv = new MedicalService();
        sv.setId(id);
        sv.setName(null);
        MedicalOrder o = new MedicalOrder();
        o.setId(orderId);
        o.setService(sv);
        return o;
    }

    private static Patient patient(String id, String fullName) {
        Patient p = new Patient();
        p.setId(id);
        p.setFullName(fullName);
        return p;
    }

    private static Staff staff(String id, String fullName) {
        Staff s = new Staff();
        s.setId(id);
        s.setFullName(fullName);
        return s;
    }

    private static MedicalRecord record(
            String id,
            String code,
            Staff createdBy,
            MedicalRecordStatus status,
            LocalDateTime  createdAt
    ) {
        MedicalRecord r = new MedicalRecord();
        r.setId(id);
        r.setMedicalRecordCode(code);
        r.setCreatedBy(createdBy);
        r.setStatus(status);
        r.setCreatedAt(createdAt);
        return r;
    }
    private static QueuePatients visit(String id) {
        QueuePatients v = new QueuePatients();
        v.setId(id);
        return v;
    }
    private static MedicalService svc(String id, String name, Department d) {
        MedicalService s = new MedicalService();
        s.setId(id);
        s.setName(name);
        s.setDepartment(d);
        return s;
    }
    private static MedicalOrder order(String id,
                                      MedicalRecord rec,
                                      MedicalService sv,
                                      MedicalOrderStatus st,
                                      LocalDateTime createdAt) {
        MedicalOrder o = new MedicalOrder();
        o.setId(id);
        o.setMedicalRecord(rec);
        o.setService(sv);
        o.setStatus(st);
        o.setCreatedAt(createdAt);
        return o;
    }
    // ===== Helpers cho SecurityContext =====
    private void setAuthUsername(String username) {
        SecurityContext sc = mock(SecurityContext.class);
        Authentication auth = mock(Authentication.class);
        when(sc.getAuthentication()).thenReturn(auth);
        when(auth.getName()).thenReturn(username);
        SecurityContextHolder.setContext(sc);
    }
    private void clearAuth() { SecurityContextHolder.clearContext(); }

    // ===== Helpers entity tối thiểu (chỉ thêm nếu bạn chưa có trong class) =====
    private static Account account(String id, String username) {
        Account a = new Account();
        a.setId(id);
        a.setUsername(username);
        return a;
    }
    private static Role role(String name) {
        Role r = new Role();
        r.setName(name);
        return r;
    }
    private static Department dept(String id) {
        Department d = new Department();
        d.setId(id);
        d.setName("Khoa A");
        return d;
    }
    private static Staff staffWithDept(String id, String code, String fullName, Department dept) {
        Staff s = new Staff();
        s.setId(id);
        s.setStaffCode(code);
        s.setFullName(fullName);
        s.setDepartment(dept);
        return s;
    }
    private static RoomTransferHistory rth(String id) {
        RoomTransferHistory h = new RoomTransferHistory();
        h.setId(id);
        h.setTransferTime(LocalDateTime.now());
        return h;
    }
    private static UpdateMedicalRecordRequest req(String summary, Boolean markFinal) {
        return UpdateMedicalRecordRequest.builder()
                .diagnosisText("DX") // @NotBlank – service không validate bean ở đây, nhưng set cho chắc
                .summary(summary)    // @NotBlank
                .markFinal(markFinal)
                .build();
    }

    @BeforeEach
    void beforeEach() {
        clearInvocations(medicalRecordRepository, medicalOrderRepository, medicalResultRepository);
    }

    private static MedicalRecord rec(String id, String code, LocalDateTime createdAt) {
        MedicalRecord r = new MedicalRecord();
        r.setId(id);
        r.setMedicalRecordCode(code);
        r.setCreatedAt(createdAt);
        return r;
    }

    private static MedicalRecordResponse resp(String id, String code) {
        return MedicalRecordResponse.builder().id(id).medicalRecordCode(code).build();
    }

    private Specification<MedicalRecord> stubSpec() {
        @SuppressWarnings("unchecked")
        Specification<MedicalRecord> spec = (root, query, cb) -> null;
        return spec;
    }

    // =========================================================
    // =============== getRelatedStaffsForFeedback =============
    // =========================================================

    // Test 1: Hồ sơ không tồn tại -> ném AppException MEDICAL_RECORD_NOT_FOUND
    @Test
    void staffs_recordNotFound_throwAppException() {
        String recordId = "REC_MISSING";
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull(recordId))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> medicalRecordService.getRelatedStaffsForFeedback(recordId));
        assertEquals(ErrorCode.MEDICAL_RECORD_NOT_FOUND, ex.getErrorCode());

        verify(medicalRecordRepository).findByIdAndDeletedAtIsNull(recordId);
        verifyNoMoreInteractions(medicalOrderRepository, medicalResultRepository);
    }

    // Test 2: Có createdBy, không có order -> trả về duy nhất creator với role "Người tạo hồ sơ"
    @Test
    void staffs_hasCreator_noOrders_returnOnlyCreator() {
        String recordId = "REC1";
        Staff creator = staff("S1", "C001", "Creator A");
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull(recordId))
                .thenReturn(Optional.of(record(recordId, creator)));
        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull(recordId))
                .thenReturn(List.of());

        List<MedicalStaffFeedbackResponse> out = medicalRecordService.getRelatedStaffsForFeedback(recordId);

        assertEquals(1, out.size());
        MedicalStaffFeedbackResponse r0 = out.get(0);
        assertEquals("S1", r0.getId());
        assertEquals("Creator A", r0.getFullName());
        assertEquals("C001", r0.getStaffCode());
        assertEquals("Người tạo hồ sơ", r0.getRole());

        verify(medicalOrderRepository).findAllByMedicalRecordIdAndDeletedAtIsNull(recordId);
        verifyNoInteractions(medicalResultRepository);
    }

    // Test 3: Không có createdBy, có result có completedBy -> chỉ thêm staff từ completedBy
    @Test
    void staffs_noCreator_hasOrdersAndResults_useCompletedByOnly() {
        String recordId = "REC2";
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull(recordId))
                .thenReturn(Optional.of(record(recordId, null)));

        MedicalOrder o1 = order("O1", recordId);
        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull(recordId))
                .thenReturn(List.of(o1));

        Staff tech = staff("S2", "T100", "Technician");
        when(medicalResultRepository.findAllByMedicalOrderIdAndDeletedAtIsNull("O1"))
                .thenReturn(List.of(result("R1", "O1", tech)));

        List<MedicalStaffFeedbackResponse> out = medicalRecordService.getRelatedStaffsForFeedback(recordId);

        assertEquals(1, out.size());
        assertEquals("S2", out.get(0).getId());
        assertEquals("Technician", out.get(0).getFullName());
        assertEquals("Người thực hiện dịch vụ", out.get(0).getRole());
    }

    // Test 4: Có createdBy và có completedBy khác -> cả hai xuất hiện, role tương ứng
    @Test
    void staffs_hasCreator_andDistinctCompletedBy_bothAppearWithCorrectRoles() {
        String recordId = "REC3";
        Staff creator = staff("S1", "C001", "Creator A");
        Staff performer = staff("S2", "T001", "Performer B");

        when(medicalRecordRepository.findByIdAndDeletedAtIsNull(recordId))
                .thenReturn(Optional.of(record(recordId, creator)));

        MedicalOrder o1 = order("O1", recordId);
        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull(recordId))
                .thenReturn(List.of(o1));
        when(medicalResultRepository.findAllByMedicalOrderIdAndDeletedAtIsNull("O1"))
                .thenReturn(List.of(result("R1", "O1", performer)));

        List<MedicalStaffFeedbackResponse> out = medicalRecordService.getRelatedStaffsForFeedback(recordId);

        assertEquals(2, out.size());
        assertEquals("S1", out.get(0).getId());
        assertEquals("Người tạo hồ sơ", out.get(0).getRole());
        assertEquals("S2", out.get(1).getId());
        assertEquals("Người thực hiện dịch vụ", out.get(1).getRole());
    }

    // Test 5: createdBy cũng là completedBy -> chỉ add 1 lần và giữ role creator (do add trước)
    @Test
    void staffs_creatorAlsoCompleted_onlyOneEntry_roleRemainsCreator() {
        String recordId = "REC4";
        Staff same = staff("S3", "X123", "Dr Same");
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull(recordId))
                .thenReturn(Optional.of(record(recordId, same)));

        MedicalOrder o1 = order("O1", recordId);
        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull(recordId))
                .thenReturn(List.of(o1));
        when(medicalResultRepository.findAllByMedicalOrderIdAndDeletedAtIsNull("O1"))
                .thenReturn(List.of(result("R1", "O1", same)));

        List<MedicalStaffFeedbackResponse> out = medicalRecordService.getRelatedStaffsForFeedback(recordId);

        assertEquals(1, out.size());
        assertEquals("S3", out.get(0).getId());
        assertEquals("Người tạo hồ sơ", out.get(0).getRole());
    }

    // Test 6: Nhiều order, nhiều result, có trùng completedBy -> không duplicate, thứ tự: creator trước
    @Test
    void staffs_multiOrders_multiResults_deduplicateAndKeepOrder() {
        String recordId = "REC5";
        Staff creator = staff("C1", "CR001", "Creator Z");
        Staff t1 = staff("S10", "T010", "Tech 1");
        Staff t2 = staff("S11", "T011", "Tech 2");
        Staff t3 = staff("S12", "T012", "Tech 3");

        when(medicalRecordRepository.findByIdAndDeletedAtIsNull(recordId))
                .thenReturn(Optional.of(record(recordId, creator)));

        MedicalOrder o1 = order("O1", recordId);
        MedicalOrder o2 = order("O2", recordId);
        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull(recordId))
                .thenReturn(List.of(o1, o2));

        when(medicalResultRepository.findAllByMedicalOrderIdAndDeletedAtIsNull("O1"))
                .thenReturn(List.of(
                        result("R1", "O1", t1),
                        result("R2", "O1", t2)
                ));
        when(medicalResultRepository.findAllByMedicalOrderIdAndDeletedAtIsNull("O2"))
                .thenReturn(List.of(
                        result("R3", "O2", t2), // duplicate
                        result("R4", "O2", t3)
                ));

        List<MedicalStaffFeedbackResponse> out = medicalRecordService.getRelatedStaffsForFeedback(recordId);

        assertEquals(4, out.size());
        assertEquals("C1", out.get(0).getId());
        assertEquals("S10", out.get(1).getId());
        assertEquals("S11", out.get(2).getId());
        assertEquals("S12", out.get(3).getId());

        assertEquals("Người tạo hồ sơ", out.get(0).getRole());
        assertEquals("Người thực hiện dịch vụ", out.get(1).getRole());
        assertEquals("Người thực hiện dịch vụ", out.get(2).getRole());
        assertEquals("Người thực hiện dịch vụ", out.get(3).getRole());
    }

    // Test 7: MedicalResult có completedBy = null -> bỏ qua
    @Test
    void staffs_nullCompletedBy_isIgnored() {
        String recordId = "REC6";
        Staff creator = staff("C9", "CR999", "Creator Q");

        when(medicalRecordRepository.findByIdAndDeletedAtIsNull(recordId))
                .thenReturn(Optional.of(record(recordId, creator)));

        MedicalOrder o1 = order("O1", recordId);
        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull(recordId))
                .thenReturn(List.of(o1));

        when(medicalResultRepository.findAllByMedicalOrderIdAndDeletedAtIsNull("O1"))
                .thenReturn(List.of(result("R1", "O1", null)));

        List<MedicalStaffFeedbackResponse> out = medicalRecordService.getRelatedStaffsForFeedback(recordId);

        assertEquals(1, out.size());
        assertEquals("C9", out.get(0).getId());
        assertEquals("Người tạo hồ sơ", out.get(0).getRole());
    }

    // Test 8: Không có creator và cũng không có order -> trả về danh sách rỗng
    @Test
    void staffs_noCreator_noOrders_returnEmptyList() {
        String recordId = "REC7";
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull(recordId))
                .thenReturn(Optional.of(record(recordId, null)));
        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull(recordId))
                .thenReturn(List.of());

        List<MedicalStaffFeedbackResponse> out = medicalRecordService.getRelatedStaffsForFeedback(recordId);

        assertTrue(out.isEmpty());
        verifyNoInteractions(medicalResultRepository);
    }

    // =========================================================
    // ============== getRelatedServicesForFeedback ============
    // =========================================================

    // Test 1: Record không tồn tại -> ném AppException MEDICAL_RECORD_NOT_FOUND
    @Test
    void services_recordNotFound_throwAppException() {
        String recordId = "REC_MISSING";
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull(recordId))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> medicalRecordService.getRelatedServicesForFeedback(recordId));
        assertEquals(ErrorCode.MEDICAL_RECORD_NOT_FOUND, ex.getErrorCode());
    }

    // Test 2: Record tồn tại nhưng không có order -> trả về list rỗng
    @Test
    void services_recordExists_noOrders_returnEmptyList() {
        String recordId = "REC1S";
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull(recordId))
                .thenReturn(Optional.of(record(recordId, null)));
        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull(recordId))
                .thenReturn(List.of());

        List<MedicalServiceForFeedbackResponse> out =
                medicalRecordService.getRelatedServicesForFeedback(recordId);

        assertNotNull(out);
        assertTrue(out.isEmpty());
    }

    // Test 3: Có 1 order hợp lệ -> map sang DTO với id và serviceName
    @Test
    void services_singleOrder_validService_mapToDto() {
        String recordId = "REC2S";
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull(recordId))
                .thenReturn(Optional.of(record(recordId, null)));
        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull(recordId))
                .thenReturn(List.of(orderWithService("O1", "S1", "Xét nghiệm máu")));

        List<MedicalServiceForFeedbackResponse> out =
                medicalRecordService.getRelatedServicesForFeedback(recordId);

        assertEquals(1, out.size());
        assertEquals("S1", out.get(0).getId());
        assertEquals("Xét nghiệm máu", out.get(0).getServiceName());
    }

    // Test 4: Nhiều order hợp lệ -> trả về nhiều DTO, giữ đúng thứ tự
    @Test
    void services_multipleOrders_validServices_keepOrder() {
        String recordId = "REC3S";
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull(recordId))
                .thenReturn(Optional.of(record(recordId, null)));
        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull(recordId))
                .thenReturn(List.of(
                        orderWithService("O1", "S1", "A"),
                        orderWithService("O2", "S2", "B"),
                        orderWithService("O3", "S3", "C")
                ));

        List<MedicalServiceForFeedbackResponse> out =
                medicalRecordService.getRelatedServicesForFeedback(recordId);

        assertEquals(3, out.size());
        assertEquals("S1", out.get(0).getId());
        assertEquals("S2", out.get(1).getId());
        assertEquals("S3", out.get(2).getId());
        assertEquals("A", out.get(0).getServiceName());
        assertEquals("B", out.get(1).getServiceName());
        assertEquals("C", out.get(2).getServiceName());
    }

    // Test 5: Trùng dịch vụ ở nhiều order -> không loại bỏ trùng, trả về nhiều DTO giống nhau
    @Test
    void services_duplicateServicesAcrossOrders_doNotFilterDuplicates() {
        String recordId = "REC4S";
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull(recordId))
                .thenReturn(Optional.of(record(recordId, null)));
        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull(recordId))
                .thenReturn(List.of(
                        orderWithService("O1", "S1", "Service A"),
                        orderWithService("O2", "S1", "Service A")
                ));

        List<MedicalServiceForFeedbackResponse> out =
                medicalRecordService.getRelatedServicesForFeedback(recordId);

        assertEquals(2, out.size());
        assertEquals("S1", out.get(0).getId());
        assertEquals("S1", out.get(1).getId());
        assertEquals("Service A", out.get(0).getServiceName());
        assertEquals("Service A", out.get(1).getServiceName());
    }

    // Test 6: Có order nhưng service = null -> hiện tại sẽ gây NullPointerException
    @Test
    void services_orderHasNullService_expectNpe() {
        String recordId = "REC5S";
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull(recordId))
                .thenReturn(Optional.of(record(recordId, null)));
        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull(recordId))
                .thenReturn(List.of(orderWithNullService("O1")));

        assertThrows(NullPointerException.class,
                () -> medicalRecordService.getRelatedServicesForFeedback(recordId));
    }

    // Test 7: Service có id hoặc name = null -> vẫn map ra DTO với field null
    @Test
    void services_serviceHasNullIdOrName_allowNullFields() {
        String recordId = "REC6S";
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull(recordId))
                .thenReturn(Optional.of(record(recordId, null)));
        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull(recordId))
                .thenReturn(List.of(
                        orderWithServiceIdNull("O1", "NAME_OK"),
                        orderWithServiceNameNull("O2", "ID_OK")
                ));

        List<MedicalServiceForFeedbackResponse> out =
                medicalRecordService.getRelatedServicesForFeedback(recordId);

        assertEquals(2, out.size());
        assertNull(out.get(0).getId());
        assertEquals("NAME_OK", out.get(0).getServiceName());
        assertEquals("ID_OK", out.get(1).getId());
        assertNull(out.get(1).getServiceName());
    }

    // Test 8: Repository trả về null thay vì list -> hiện tại sẽ gây NullPointerException
    @Test
    void services_orderRepositoryReturnsNull_expectNpe() {
        String recordId = "REC7S";
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull(recordId))
                .thenReturn(Optional.of(record(recordId, null)));
        when(medicalOrderRepository.findAllByMedicalRecordIdAndDeletedAtIsNull(recordId))
                .thenReturn(null);

        assertThrows(NullPointerException.class,
                () -> medicalRecordService.getRelatedServicesForFeedback(recordId));
    }



    // getMedicalRecordHistory


    // Case 1: Bệnh nhân không tồn tại -> ném AppException PATIENT_NOT_FOUND
    @Test
    void history_patientNotFound_throwAppException() {
        String patientId = "P_MISSING";
        when(patientRepository.findByIdAndDeletedAtIsNull(patientId))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> medicalRecordService.getMedicalRecordHistory(patientId));

        assertEquals(ErrorCode.PATIENT_NOT_FOUND, ex.getErrorCode());
        verify(patientRepository).findByIdAndDeletedAtIsNull(patientId);
        verifyNoInteractions(medicalRecordRepository);
    }


    // Case 2: Bệnh nhân tồn tại, không có hồ sơ -> trả về List<MedicalRecordResponse> rỗng
    @Test
    void history_patientExists_noRecords_returnEmptyList() {
        String patientId = "P1";
        when(patientRepository.findByIdAndDeletedAtIsNull(patientId))
                .thenReturn(Optional.of(patient(patientId, "Alice")));
        when(medicalRecordRepository.findAllByPatientIdAndDeletedAtIsNullOrderByCreatedAtDesc(patientId))
                .thenReturn(List.of());

        List<MedicalRecordResponse> out = medicalRecordService.getMedicalRecordHistory(patientId);

        assertNotNull(out);
        assertTrue(out.isEmpty());
    }

// Case 3: Một hồ sơ đầy đủ dữ liệu -> map đúng mọi trường (id, code, names, status, time)
    @Test
    void history_singleRecord_fullData_mapCorrectly() {
        String patientId = "P2";
        when(patientRepository.findByIdAndDeletedAtIsNull(patientId))
                .thenReturn(Optional.of(patient(patientId, "Alice")));

        Staff doctor = staff("D1", "Dr. Bob");
        LocalDateTime t = LocalDateTime.of(2025, 8, 10, 9, 15, 0);
        MedicalRecord rec = record("R1", "MR001", doctor, MedicalRecordStatus.WAITING_FOR_PAYMENT, t);

        when(medicalRecordRepository.findAllByPatientIdAndDeletedAtIsNullOrderByCreatedAtDesc(patientId))
                .thenReturn(List.of(rec));

        List<MedicalRecordResponse> out = medicalRecordService.getMedicalRecordHistory(patientId);

        assertEquals(1, out.size());
        MedicalRecordResponse r = out.get(0);
        assertEquals("R1", r.getId());
        assertEquals("MR001", r.getMedicalRecordCode());
        assertEquals("Alice", r.getPatientName());
        assertEquals("Dr. Bob", r.getDoctorName());
        assertEquals("WAITING_FOR_PAYMENT", r.getStatus());
        assertEquals(t, r.getCreatedAt());
    }


// Case 4: Nhiều hồ sơ (repo đã sort DESC) -> giữ nguyên thứ tự khi map sang response
    @Test
    void history_multipleRecords_keepRepositoryOrder() {
        String patientId = "P3";
        when(patientRepository.findByIdAndDeletedAtIsNull(patientId))
                .thenReturn(Optional.of(patient(patientId, "Alice")));

        Staff doctor = staff("D1", "Dr. Bob");
        MedicalRecord r1 = record("R_NEW", "MR010", doctor, MedicalRecordStatus.COMPLETED,
                LocalDateTime.of(2025, 8, 10, 8, 0, 0));
        MedicalRecord r2 = record("R_MID", "MR009", doctor, MedicalRecordStatus.RESULT_COMPLETED,
                LocalDateTime.of(2025, 8, 5, 8, 0, 0));
        MedicalRecord r3 = record("R_OLD", "MR008", doctor, MedicalRecordStatus.TESTING,
                LocalDateTime.of(2025, 8, 1, 8, 0, 0));

        // giả định repo đã trả theo DESC (mới -> cũ)
        when(medicalRecordRepository.findAllByPatientIdAndDeletedAtIsNullOrderByCreatedAtDesc(patientId))
                .thenReturn(List.of(r1, r2, r3));

        List<MedicalRecordResponse> out = medicalRecordService.getMedicalRecordHistory(patientId);

        assertEquals(3, out.size());
        assertEquals("R_NEW", out.get(0).getId());
        assertEquals("R_MID", out.get(1).getId());
        assertEquals("R_OLD", out.get(2).getId());
    }


    // Case 5: Record có createdBy = null -> theo code hiện tại sẽ NPE khi gọi getCreatedBy().getFullName()
    @Test
    void history_recordWithNullCreatedBy_expectNpe() {
        String patientId = "P4";
        when(patientRepository.findByIdAndDeletedAtIsNull(patientId))
                .thenReturn(Optional.of(patient(patientId, "Alice")));

        MedicalRecord rec = record("R1", "MR001", null, MedicalRecordStatus.WAITING_FOR_PAYMENT,
                LocalDateTime.of(2025, 8, 10, 9, 15, 0));

        when(medicalRecordRepository.findAllByPatientIdAndDeletedAtIsNullOrderByCreatedAtDesc(patientId))
                .thenReturn(List.of(rec));

        assertThrows(NullPointerException.class,
                () -> medicalRecordService.getMedicalRecordHistory(patientId));
    }


    // Case 6: Record có status = null -> theo code hiện tại sẽ NPE khi gọi record.getStatus().name()
    @Test
    void history_recordWithNullStatus_expectNpe() {
        String patientId = "P5";
        when(patientRepository.findByIdAndDeletedAtIsNull(patientId))
                .thenReturn(Optional.of(patient(patientId, "Alice")));

        Staff doctor = staff("D1", "Dr. Bob");
        MedicalRecord rec = record("R1", "MR001", null, MedicalRecordStatus.WAITING_FOR_PAYMENT,
                LocalDateTime.of(2025, 8, 10, 9, 15, 0));

        when(medicalRecordRepository.findAllByPatientIdAndDeletedAtIsNullOrderByCreatedAtDesc(patientId))
                .thenReturn(List.of(rec));

        assertThrows(NullPointerException.class,
                () -> medicalRecordService.getMedicalRecordHistory(patientId));
    }

    // Case 7: Repository trả về null thay vì List -> theo code hiện tại sẽ NPE ở records.stream()
    @Test
    void history_repositoryReturnsNull_expectNpe() {
        String patientId = "P6";
        when(patientRepository.findByIdAndDeletedAtIsNull(patientId))
                .thenReturn(Optional.of(patient(patientId, "Alice")));
        when(medicalRecordRepository.findAllByPatientIdAndDeletedAtIsNullOrderByCreatedAtDesc(patientId))
                .thenReturn(null); // edge case (không nên xảy ra, nhưng test để bộc lộ behavior hiện tại)

        assertThrows(NullPointerException.class,
                () -> medicalRecordService.getMedicalRecordHistory(patientId));
    }

    // Case 8: Field con bị null nhưng object tồn tại -> map ra giá trị null
    //         (createdBy != null, nhưng createdBy.fullName = null)
    @Test
    void history_createdByExists_butDoctorNameNull_mapNullDoctorName() {
        String patientId = "P7";
        when(patientRepository.findByIdAndDeletedAtIsNull(patientId))
                .thenReturn(Optional.of(patient(patientId, "Alice")));

        Staff doctor = staff("D1", null); // fullName null nhưng object tồn tại
        MedicalRecord rec = record(
                "R1",
                "MR001",
                doctor,  // <-- phải là doctor, không phải null
                MedicalRecordStatus.WAITING_FOR_PAYMENT,
                LocalDateTime.of(2025, 8, 10, 9, 15, 0)
        );

        when(medicalRecordRepository.findAllByPatientIdAndDeletedAtIsNullOrderByCreatedAtDesc(patientId))
                .thenReturn(List.of(rec));

        // Với code hiện tại: KHÔNG NPE vì createdBy != null,
        // doctorName sẽ trở thành null (map trực tiếp getFullName()).
        List<MedicalRecordResponse> out = medicalRecordService.getMedicalRecordHistory(patientId);

        assertEquals(1, out.size());
        assertNull(out.get(0).getDoctorName());
        assertEquals("Alice", out.get(0).getPatientName());
        assertEquals("MR001", out.get(0).getMedicalRecordCode());
    }

    // getMedicalRecordsPaged

    // =========================================================
    // 1) sortBy = null (fallback "createdAt"), sortDir = "asc"
    // =========================================================
    @Test
    void paged_sortByNull_sortAsc() {
        Map<String,String> filters = Map.of();
        Specification<MedicalRecord> spec = stubSpec();
        specStatic.when(() -> MedicalRecordSpecification.buildSpecification(filters)).thenReturn(spec);

        // stub repo page
        List<MedicalRecord> content = List.of(rec("R1","MR1", LocalDateTime.now()));
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(medicalRecordRepository.findAll(eq(spec), any(Pageable.class)))
                .thenReturn(new PageImpl<>(content, PageRequest.of(0, 10, Sort.by("createdAt").ascending()), 1));

        when(medicalRecordMapper.toMedicalRecordResponse(any())).thenAnswer(inv -> {
            MedicalRecord m = inv.getArgument(0);
            return resp(m.getId(), m.getMedicalRecordCode());
        });

        Page<MedicalRecordResponse> out = medicalRecordService.getMedicalRecordsPaged(filters, 0, 10, null, "asc");

        verify(medicalRecordRepository).findAll(eq(spec), pageableCaptor.capture());
        Pageable p = pageableCaptor.getValue();
        assertEquals(0, p.getPageNumber());
        assertEquals(10, p.getPageSize());
        assertEquals(Sort.Direction.ASC, p.getSort().getOrderFor("createdAt").getDirection());
        assertEquals(1, out.getTotalElements());
        assertEquals("R1", out.getContent().get(0).getId());
    }

    // =========================================================
    // 2) sortBy = "" (blank) -> "createdAt", sortDir = "desc"
    // =========================================================
    @Test
    void paged_sortByBlank_sortDesc() {
        Map<String,String> filters = Map.of("k","v");
        Specification<MedicalRecord> spec = stubSpec();
        specStatic.when(() -> MedicalRecordSpecification.buildSpecification(filters)).thenReturn(spec);

        when(medicalRecordRepository.findAll(eq(spec), any(Pageable.class)))
                .thenReturn(Page.empty());

        Page<MedicalRecordResponse> out = medicalRecordService.getMedicalRecordsPaged(filters, 1, 5, "   ", "desc");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(medicalRecordRepository).findAll(eq(spec), captor.capture());
        Pageable p = captor.getValue();
        assertEquals(1, p.getPageNumber());
        assertEquals(5, p.getPageSize());
        assertEquals(Sort.Direction.DESC, p.getSort().getOrderFor("createdAt").getDirection());
        assertTrue(out.isEmpty());
    }

    // =========================================================
    // 3) sortDir = "ASC" (case-insensitive) -> asc
    // =========================================================
    @Test
    void paged_sortDirAsc_caseInsensitive() {
        Map<String,String> filters = Map.of();
        Specification<MedicalRecord> spec = stubSpec();
        specStatic.when(() -> MedicalRecordSpecification.buildSpecification(filters)).thenReturn(spec);

        when(medicalRecordRepository.findAll(eq(spec), any(Pageable.class)))
                .thenReturn(Page.empty());

        medicalRecordService.getMedicalRecordsPaged(filters, 0, 20, "patientName", "ASC");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(medicalRecordRepository).findAll(eq(spec), captor.capture());
        Pageable p = captor.getValue();
        assertEquals(Sort.Direction.ASC, p.getSort().getOrderFor("patientName").getDirection());
    }

    // =========================================================
    // 4) sortDir = "DeSc" -> desc
    // =========================================================
    @Test
    void paged_sortDirDesc_caseInsensitive() {
        Map<String,String> filters = Map.of();
        Specification<MedicalRecord> spec = stubSpec();
        specStatic.when(() -> MedicalRecordSpecification.buildSpecification(filters)).thenReturn(spec);

        when(medicalRecordRepository.findAll(eq(spec), any(Pageable.class)))
                .thenReturn(Page.empty());

        medicalRecordService.getMedicalRecordsPaged(filters, 2, 15, "medicalRecordCode", "DeSc");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(medicalRecordRepository).findAll(eq(spec), captor.capture());
        Pageable p = captor.getValue();
        assertEquals(2, p.getPageNumber());
        assertEquals(15, p.getPageSize());
        assertEquals(Sort.Direction.DESC, p.getSort().getOrderFor("medicalRecordCode").getDirection());
    }

    // =========================================================
    // 5) sortDir giá trị lạ -> nhánh else => desc
    // =========================================================
    @Test
    void paged_sortDirUnknown_defaultsDesc() {
        Map<String,String> filters = Map.of();
        Specification<MedicalRecord> spec = stubSpec();
        specStatic.when(() -> MedicalRecordSpecification.buildSpecification(filters)).thenReturn(spec);

        when(medicalRecordRepository.findAll(eq(spec), any(Pageable.class)))
                .thenReturn(Page.empty());

        medicalRecordService.getMedicalRecordsPaged(filters, 0, 10, "createdAt", "weird");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(medicalRecordRepository).findAll(eq(spec), captor.capture());
        Pageable p = captor.getValue();
        assertEquals(Sort.Direction.DESC, p.getSort().getOrderFor("createdAt").getDirection());
    }

    // =========================================================
    // 6) filters rỗng -> spec build và repo trả page rỗng
    // =========================================================
    @Test
    void paged_emptyFilters_emptyPage() {
        Map<String,String> filters = Map.of();
        Specification<MedicalRecord> spec = stubSpec();
        specStatic.when(() -> MedicalRecordSpecification.buildSpecification(filters)).thenReturn(spec);

        when(medicalRecordRepository.findAll(eq(spec), any(Pageable.class)))
                .thenReturn(Page.empty(PageRequest.of(0, 10)));

        Page<MedicalRecordResponse> out = medicalRecordService.getMedicalRecordsPaged(filters, 0, 10, "createdAt", "asc");

        assertTrue(out.isEmpty());
    }

    // =========================================================
    // 7) filters có giá trị -> map đúng số phần tử
    // =========================================================
    @Test
    void paged_filtersApplied_mapAllElements() {
        Map<String,String> filters = Map.of("status","COMPLETED","doctor","Bob");
        Specification<MedicalRecord> spec = stubSpec();
        specStatic.when(() -> MedicalRecordSpecification.buildSpecification(filters)).thenReturn(spec);

        List<MedicalRecord> content = List.of(
                rec("R1","MR1", LocalDateTime.of(2025,1,1,8,0)),
                rec("R2","MR2", LocalDateTime.of(2025,1,2,8,0))
        );
        when(medicalRecordRepository.findAll(eq(spec), any(Pageable.class)))
                .thenReturn(new PageImpl<>(content));

        when(medicalRecordMapper.toMedicalRecordResponse(any()))
                .thenAnswer(inv -> {
                    MedicalRecord m = inv.getArgument(0);
                    return resp(m.getId(), m.getMedicalRecordCode());
                });

        Page<MedicalRecordResponse> out = medicalRecordService.getMedicalRecordsPaged(filters, 0, 2, "createdAt", "desc");

        assertEquals(2, out.getContent().size());
        assertEquals("R1", out.getContent().get(0).getId());
        assertEquals("R2", out.getContent().get(1).getId());
    }

    // =========================================================
    // 8) page = 0, size = 10 -> Pageable chính xác
    // =========================================================
    @Test
    void paged_page0_size10_pageableCorrect() {
        Map<String,String> filters = Map.of();
        Specification<MedicalRecord> spec = stubSpec();
        specStatic.when(() -> MedicalRecordSpecification.buildSpecification(filters)).thenReturn(spec);

        when(medicalRecordRepository.findAll(eq(spec), any(Pageable.class)))
                .thenReturn(Page.empty());

        medicalRecordService.getMedicalRecordsPaged(filters, 0, 10, "createdAt", "asc");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(medicalRecordRepository).findAll(eq(spec), captor.capture());
        Pageable p = captor.getValue();
        assertEquals(0, p.getPageNumber());
        assertEquals(10, p.getPageSize());
    }

    // =========================================================
    // 9) page > 0, size > 0 -> phân trang đúng
    // =========================================================
    @Test
    void paged_page2_size5_pageableCorrect() {
        Map<String,String> filters = Map.of();
        Specification<MedicalRecord> spec = stubSpec();
        specStatic.when(() -> MedicalRecordSpecification.buildSpecification(filters)).thenReturn(spec);

        when(medicalRecordRepository.findAll(eq(spec), any(Pageable.class)))
                .thenReturn(Page.empty());

        medicalRecordService.getMedicalRecordsPaged(filters, 2, 5, "createdAt", "desc");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(medicalRecordRepository).findAll(eq(spec), captor.capture());
        Pageable p = captor.getValue();
        assertEquals(2, p.getPageNumber());
        assertEquals(5, p.getPageSize());
    }

    // =========================================================
    // 10) sortBy field khác (vd "patient.fullName")
    // =========================================================
    @Test
    void paged_sortByOtherField() {
        Map<String,String> filters = Map.of();
        Specification<MedicalRecord> spec = stubSpec();
        specStatic.when(() -> MedicalRecordSpecification.buildSpecification(filters)).thenReturn(spec);

        when(medicalRecordRepository.findAll(eq(spec), any(Pageable.class)))
                .thenReturn(Page.empty());

        medicalRecordService.getMedicalRecordsPaged(filters, 1, 25, "patient.fullName", "asc");

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(medicalRecordRepository).findAll(eq(spec), captor.capture());
        Pageable p = captor.getValue();
        assertNotNull(p.getSort().getOrderFor("patient.fullName"));
        assertEquals(Sort.Direction.ASC, p.getSort().getOrderFor("patient.fullName").getDirection());
    }

    // =========================================================
    // 11) Repo trả Page có dữ liệu -> đảm bảo map giữ nguyên thứ tự
    // =========================================================
    @Test
    void paged_repositoryContent_orderPreservedAfterMap() {
        Map<String,String> filters = Map.of();
        Specification<MedicalRecord> spec = stubSpec();
        specStatic.when(() -> MedicalRecordSpecification.buildSpecification(filters)).thenReturn(spec);

        List<MedicalRecord> content = List.of(
                rec("R_A","A", LocalDateTime.of(2025,8,10,8,0)),
                rec("R_B","B", LocalDateTime.of(2025,8,9,8,0)),
                rec("R_C","C", LocalDateTime.of(2025,8,8,8,0))
        );
        when(medicalRecordRepository.findAll(eq(spec), any(Pageable.class)))
                .thenReturn(new PageImpl<>(content));

        when(medicalRecordMapper.toMedicalRecordResponse(any()))
                .thenAnswer(inv -> {
                    MedicalRecord m = inv.getArgument(0);
                    return resp(m.getId(), m.getMedicalRecordCode());
                });

        Page<MedicalRecordResponse> out = medicalRecordService.getMedicalRecordsPaged(filters, 0, 3, "createdAt", "desc");

        assertEquals(List.of("R_A","R_B","R_C"),
                out.map(MedicalRecordResponse::getId).getContent());
    }

    // =========================================================
    // 12) Mapper ném Exception -> propagate ra ngoài
    // =========================================================
    @Test
    void paged_mapperThrows_propagates() {
        Map<String,String> filters = Map.of();
        Specification<MedicalRecord> spec = stubSpec();
        specStatic.when(() -> MedicalRecordSpecification.buildSpecification(filters)).thenReturn(spec);

        List<MedicalRecord> content = List.of(rec("R_ERR","E", LocalDateTime.now()));
        when(medicalRecordRepository.findAll(eq(spec), any(Pageable.class)))
                .thenReturn(new PageImpl<>(content));

        when(medicalRecordMapper.toMedicalRecordResponse(any()))
                .thenThrow(new RuntimeException("map error"));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> medicalRecordService.getMedicalRecordsPaged(filters, 0, 10, "createdAt", "asc"));

        assertEquals("map error", ex.getMessage());
    }

    // updateMedicalRecord

    // 1) Record không tồn tại → AppException(MEDICAL_RECORD_NOT_FOUND)
    @Test
    void update_recordNotFound_throw_MEDICAL_RECORD_NOT_FOUND() {
        String recordId = "REC_MISS";
        // setAuthUsername("u1");  // ❌ bỏ đi – không cần

        when(medicalRecordRepository.findByIdAndDeletedAtIsNull(recordId))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> medicalRecordService.updateMedicalRecord(recordId, req("S", true)));
        assertEquals(ErrorCode.MEDICAL_RECORD_NOT_FOUND, ex.getErrorCode());

        verify(medicalRecordRepository).findByIdAndDeletedAtIsNull(recordId);
        verifyNoMoreInteractions(accountRepository, staffRepository, roomTransferHistoryRepository);
    }


    // 2) Account hiện tại không tồn tại → AppException(UNAUTHORIZED)
    @Test
    void update_accountMissing_throw_UNAUTHORIZED() {
        String recordId = "REC1";
        setAuthUsername("whoami");

        when(medicalRecordRepository.findByIdAndDeletedAtIsNull(recordId))
                .thenReturn(Optional.of(record(recordId, staff("S0","C0","X"))));
        when(accountRepository.findByUsernameAndDeletedAtIsNull("whoami"))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> medicalRecordService.updateMedicalRecord(recordId, req("S", true)));
        assertEquals(ErrorCode.UNAUTHORIZED, ex.getErrorCode());
    }

    // 3) Staff không tồn tại theo accountId → AppException(STAFF_NOT_FOUND)
    @Test
    void update_staffMissing_throw_STAFF_NOT_FOUND() {
        String recordId = "REC1";
        setAuthUsername("doctor");

        when(medicalRecordRepository.findByIdAndDeletedAtIsNull(recordId))
                .thenReturn(Optional.of(record(recordId, staff("S0","C0","X"))));
        Account acc = account("A1","doctor");
        when(accountRepository.findByUsernameAndDeletedAtIsNull("doctor"))
                .thenReturn(Optional.of(acc));
        when(staffRepository.findByAccountIdAndDeletedAtIsNull("A1"))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> medicalRecordService.updateMedicalRecord(recordId, req("S", true)));
        assertEquals(ErrorCode.STAFF_NOT_FOUND, ex.getErrorCode());
    }

    // 4) Staff không trong ca → AppException(ACTION_NOT_ALLOWED)
    @Test
    void update_staffNotOnShift_throw_ACTION_NOT_ALLOWED() {
        String recordId = "REC1";
        setAuthUsername("doctor");

        when(medicalRecordRepository.findByIdAndDeletedAtIsNull(recordId))
                .thenReturn(Optional.of(record(recordId, staff("S0","C0","X"))));
        Account acc = account("A2","doctor");
        when(accountRepository.findByUsernameAndDeletedAtIsNull("doctor"))
                .thenReturn(Optional.of(acc));
        Staff st = staffWithDept("S1","C001","Dr A", dept("DPT1"));
        when(staffRepository.findByAccountIdAndDeletedAtIsNull("A2"))
                .thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("S1")).thenReturn(false);

        AppException ex = assertThrows(AppException.class,
                () -> medicalRecordService.updateMedicalRecord(recordId, req("S", true)));
        assertEquals(ErrorCode.ACTION_NOT_ALLOWED, ex.getErrorCode());
    }

    // 5) Không có role DOCTOR → AppException(NO_PERMISSION)
    @Test
    void update_notDoctor_throw_NO_PERMISSION() {
        String recordId = "REC1";
        setAuthUsername("nurse");

        when(medicalRecordRepository.findByIdAndDeletedAtIsNull(recordId))
                .thenReturn(Optional.of(record(recordId, staff("S0","C0","X"))));
        Account acc = account("A3","nurse");
        acc.setRoles(Set.of(role("NURSE"))); // không có DOCTOR
        when(accountRepository.findByUsernameAndDeletedAtIsNull("nurse"))
                .thenReturn(Optional.of(acc));
        when(accountService.getAccount("A3"))
                .thenReturn(accountResp("A3","nurse","NURSE"));

        Staff st = staffWithDept("S2","C002","Nurse A", dept("DPT1"));
        when(staffRepository.findByAccountIdAndDeletedAtIsNull("A3"))
                .thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("S2")).thenReturn(true);

        AppException ex = assertThrows(AppException.class,
                () -> medicalRecordService.updateMedicalRecord(recordId, req("S", true)));
        assertEquals(ErrorCode.NO_PERMISSION, ex.getErrorCode());
    }

    // 6) Không có RoomTransferHistory phù hợp → AppException(ROOM_TRANSFER_NOT_FOUND)
    @Test
    void update_roomTransferMissing_throw_ROOM_TRANSFER_NOT_FOUND() {
        String recordId = "REC1";
        setAuthUsername("doctor");

        when(medicalRecordRepository.findByIdAndDeletedAtIsNull(recordId))
                .thenReturn(Optional.of(record(recordId, staff("S0","C0","X"))));

        Account acc = account("A4","doctor");
        acc.setRoles(Set.of(role("DOCTOR")));
        when(accountRepository.findByUsernameAndDeletedAtIsNull("doctor"))
                .thenReturn(Optional.of(acc));
        when(accountService.getAccount("A4"))
                .thenReturn(accountResp("A4","doctor","DOCTOR"));

        Staff st = staffWithDept("S3","C003","Dr B", dept("DPT9"));
        when(staffRepository.findByAccountIdAndDeletedAtIsNull("A4"))
                .thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("S3")).thenReturn(true);

        when(roomTransferHistoryRepository
                .findTopByMedicalRecordIdAndToDepartmentIdOrderByTransferTimeDesc(recordId, "DPT9"))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> medicalRecordService.updateMedicalRecord(recordId, req("S", true)));
        assertEquals(ErrorCode.ROOM_TRANSFER_NOT_FOUND, ex.getErrorCode());
    }

    // 7) staff.getDepartment() hoặc staff.getDepartment().getId() null → kỳ vọng NPE (behavior hiện tại)
    @Test
    void update_staffDepartmentNull_expectNpe() {
        String recordId = "REC1";
        setAuthUsername("doctor");

        when(medicalRecordRepository.findByIdAndDeletedAtIsNull(recordId))
                .thenReturn(Optional.of(record(recordId, staff("S0","C0","X"))));

        Account acc = account("A5","doctor");
        acc.setRoles(Set.of(role("DOCTOR")));
        when(accountRepository.findByUsernameAndDeletedAtIsNull("doctor"))
                .thenReturn(Optional.of(acc));

        when(accountService.getAccount("A5"))
                .thenReturn(accountResp("A5","doctor","DOCTOR"));

        // department = null
        Staff st = staffWithDept("S4","C004","Dr C", null);
        when(staffRepository.findByAccountIdAndDeletedAtIsNull("A5"))
                .thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("S4")).thenReturn(true);

        assertThrows(NullPointerException.class,
                () -> medicalRecordService.updateMedicalRecord(recordId, req("S", true)));
        verify(roomTransferHistoryRepository, never())
                .findTopByMedicalRecordIdAndToDepartmentIdOrderByTransferTimeDesc(any(), any());
    }

    // 8) SecurityContext không có Authentication/getName → NPE (edge-case)
    @Test
    void update_securityContextNoAuth_expectNpe() {
        String recordId = "REC1";
        clearAuth(); // không set auth

        when(medicalRecordRepository.findByIdAndDeletedAtIsNull(recordId))
                .thenReturn(Optional.of(record(recordId, staff("S0","C0","X"))));

        assertThrows(NullPointerException.class,
                () -> medicalRecordService.updateMedicalRecord(recordId, req("S", true)));
        verifyNoInteractions(accountRepository, staffRepository, workScheduleService);
    }

    // 9) getMedicalRecordDetail(...) ném RuntimeException → propagate
    @Test
    void update_getDetailThrows_propagates() {
        String recordId = "REC1";
        setAuthUsername("doctor");

        when(medicalRecordRepository.findByIdAndDeletedAtIsNull(recordId))
                .thenReturn(Optional.of(record(recordId, staff("S0","C0","X"))));

        Account acc = account("A6","doctor");
        acc.setRoles(Set.of(role("DOCTOR")));
        when(accountRepository.findByUsernameAndDeletedAtIsNull("doctor"))
                .thenReturn(Optional.of(acc));
        when(accountService.getAccount("A6"))
                .thenReturn(accountResp("A6","doctor","DOCTOR"));

        Staff st = staffWithDept("S5","C005","Dr D", dept("DPT7"));
        when(staffRepository.findByAccountIdAndDeletedAtIsNull("A6"))
                .thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("S5")).thenReturn(true);

        when(roomTransferHistoryRepository
                .findTopByMedicalRecordIdAndToDepartmentIdOrderByTransferTimeDesc(recordId, "DPT7"))
                .thenReturn(Optional.of(rth("H1")));

        when(roomTransferHistoryRepository.save(any(RoomTransferHistory.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(medicalRecordRepository.save(any(MedicalRecord.class)))
                .thenAnswer(i -> i.getArgument(0));

        // Spy từ instance đã được tiêm mock
        MedicalRecordServiceImpl spySvc = Mockito.spy(medicalRecordService);

        // Ném lỗi khi gọi getMedicalRecordDetail(...)
        Mockito.doThrow(new RuntimeException("detail fails"))
                .when(spySvc).getMedicalRecordDetail(recordId);
        // (hoặc .when(spySvc).getMedicalRecordDetail(anyString()); nếu muốn đơn giản)

        // Chỉ assert loại exception (tránh lệ thuộc message)
        assertThrows(RuntimeException.class,
                () -> spySvc.updateMedicalRecord(recordId, req("S", true)));

        // Xác nhận chắc chắn có gọi tới method cần stub
        verify(spySvc).getMedicalRecordDetail(recordId);
    }

    // =========================================================
// 10) Cập nhật tối thiểu: summary=null, markFinal=null
//     - history.setDoctor(staff) & save(history)
//     - record.summary KHÔNG đổi; các chỉ số giữ nguyên
//     - updatedAt set; save(record) & getMedicalRecordDetail() được gọi
// =========================================================
    @Test
    void update_minimalUpdate_onlyDoctorOnHistory_andUpdatedAt() {
        String recordId = "REC_MIN";
        setAuthUsername("doctor");

        // record gốc với một số chỉ số
        MedicalRecord original = record(recordId, staff("S0", "C0", "X"));
        original.setDiagnosisText("Dx0");
        original.setTemperature(37.0);
        original.setRespiratoryRate(16);
        original.setBloodPressure("120/80");
        original.setHeartRate(72);
        original.setHeightCm(170.0);
        original.setWeightKg(60.0);
        original.setBmi(20.8);
        original.setSpo2(99);
        original.setSummary("SUM0");

        when(medicalRecordRepository.findByIdAndDeletedAtIsNull(recordId))
                .thenReturn(Optional.of(original));

        Account acc = account("A10","doctor");
        acc.setRoles(Set.of(role("DOCTOR")));
        when(accountRepository.findByUsernameAndDeletedAtIsNull("doctor"))
                .thenReturn(Optional.of(acc));
        when(accountService.getAccount("A10"))
                .thenReturn(accountResp("A10","doctor","DOCTOR"));

        Staff st = staffWithDept("S10","C010","Dr X", dept("DPT10"));
        when(staffRepository.findByAccountIdAndDeletedAtIsNull("A10"))
                .thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("S10")).thenReturn(true);

        when(roomTransferHistoryRepository
                .findTopByMedicalRecordIdAndToDepartmentIdOrderByTransferTimeDesc(recordId, "DPT10"))
                .thenReturn(Optional.of(rth("H10")));

        ArgumentCaptor<RoomTransferHistory> hCap = ArgumentCaptor.forClass(RoomTransferHistory.class);
        when(roomTransferHistoryRepository.save(any(RoomTransferHistory.class)))
                .thenAnswer(i -> i.getArgument(0));

        ArgumentCaptor<MedicalRecord> rCap = ArgumentCaptor.forClass(MedicalRecord.class);
        when(medicalRecordRepository.save(any(MedicalRecord.class)))
                .thenAnswer(i -> i.getArgument(0));

        // ✅ Tạo spy từ instance đã @InjectMocks
        MedicalRecordServiceImpl spySvc = Mockito.spy(medicalRecordService);
        // Chặn getMedicalRecordDetail trên spy
        Mockito.doReturn(MedicalRecordDetailResponse.builder().id(recordId).build())
                .when(spySvc).getMedicalRecordDetail(eq(recordId));

        UpdateMedicalRecordRequest req = UpdateMedicalRecordRequest.builder()
                .summary(null).markFinal(null) // tối thiểu
                .build();

        // ❗ GỌI service QUA SPY
        spySvc.updateMedicalRecord(recordId, req);

        // verify history
        verify(roomTransferHistoryRepository).save(hCap.capture());
        assertEquals("S10", hCap.getValue().getDoctor().getId());

        // verify record
        verify(medicalRecordRepository).save(rCap.capture());
        MedicalRecord saved = rCap.getValue();
        assertEquals("SUM0", saved.getSummary());                 // không đổi
        assertEquals("Dx0", saved.getDiagnosisText());            // giữ nguyên
        assertEquals(37.0, saved.getTemperature());
        assertEquals(16, saved.getRespiratoryRate());
        assertEquals("120/80", saved.getBloodPressure());
        assertEquals(72, saved.getHeartRate());
        assertEquals(170.0, saved.getHeightCm());
        assertEquals(60.0, saved.getWeightKg());
        assertEquals(20.8, saved.getBmi());
        assertEquals(99, saved.getSpo2());
        assertNotNull(saved.getUpdatedAt());                      // đã set

        // ✅ verify gọi get detail trên spy
        verify(spySvc).getMedicalRecordDetail(recordId);
    }


    // =========================================================
// 11) summary có, markFinal=true -> history(conclusion,isFinal) set;
//     record.summary được cập nhật
// =========================================================
    @Test
    void update_summaryPresent_markFinalTrue_updatesHistoryAndRecordSummary() {
        String recordId = "REC11";
        setAuthUsername("doctor");

        MedicalRecord rec = record(recordId, staff("S0","C0","X"));
        rec.setSummary("OLD_SUM");

        when(medicalRecordRepository.findByIdAndDeletedAtIsNull(recordId))
                .thenReturn(Optional.of(rec));

        Account acc = account("A11","doctor");
        acc.setRoles(Set.of(role("DOCTOR")));
        when(accountRepository.findByUsernameAndDeletedAtIsNull("doctor"))
                .thenReturn(Optional.of(acc));
        when(accountService.getAccount("A11"))
                .thenReturn(accountResp("A11","doctor","DOCTOR"));

        Staff st = staffWithDept("S11","C011","Dr 11", dept("DPT11"));
        when(staffRepository.findByAccountIdAndDeletedAtIsNull("A11"))
                .thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("S11")).thenReturn(true);

        when(roomTransferHistoryRepository
                .findTopByMedicalRecordIdAndToDepartmentIdOrderByTransferTimeDesc(recordId, "DPT11"))
                .thenReturn(Optional.of(rth("H11")));

        ArgumentCaptor<RoomTransferHistory> hCap = ArgumentCaptor.forClass(RoomTransferHistory.class);
        when(roomTransferHistoryRepository.save(any(RoomTransferHistory.class)))
                .thenAnswer(i -> i.getArgument(0));

        ArgumentCaptor<MedicalRecord> rCap = ArgumentCaptor.forClass(MedicalRecord.class);
        when(medicalRecordRepository.save(any(MedicalRecord.class)))
                .thenAnswer(i -> i.getArgument(0));

        // ✅ Tạo spy và chặn getMedicalRecordDetail trên spy
        MedicalRecordServiceImpl spySvc = Mockito.spy(medicalRecordService);
        Mockito.doReturn(MedicalRecordDetailResponse.builder().id(recordId).build())
                .when(spySvc).getMedicalRecordDetail(eq(recordId));

        UpdateMedicalRecordRequest req = UpdateMedicalRecordRequest.builder()
                .summary("NEW_SUM")
                .markFinal(true)
                .build();

        // ❗ GỌI QUA SPY (đây là chỗ bạn đang gọi nhầm)
        spySvc.updateMedicalRecord(recordId, req);

        verify(roomTransferHistoryRepository).save(hCap.capture());
        assertEquals("NEW_SUM", hCap.getValue().getConclusionText());
        assertEquals(Boolean.TRUE, hCap.getValue().getIsFinal());

        verify(medicalRecordRepository).save(rCap.capture());
        assertEquals("NEW_SUM", rCap.getValue().getSummary());

        // (tuỳ chọn) đảm bảo có gọi get detail trên spy
        verify(spySvc).getMedicalRecordDetail(recordId);
    }


    // =========================================================
// 12) summary có, markFinal=false -> chỉ update history;
//     record.summary không đổi
// =========================================================
    @Test
    void update_summaryPresent_markFinalFalse_updatesHistoryOnly() {
        String recordId = "REC12";
        setAuthUsername("doctor");

        // record gốc với summary cũ
        MedicalRecord rec = record(recordId, staff("S0","C0","X"));
        rec.setSummary("KEEP_SUM");
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull(recordId))
                .thenReturn(Optional.of(rec));

        // account & role bác sĩ
        Account acc = account("A12","doctor");
        acc.setRoles(Set.of(role("DOCTOR")));
        when(accountRepository.findByUsernameAndDeletedAtIsNull("doctor"))
                .thenReturn(Optional.of(acc));
        when(accountService.getAccount("A12"))
                .thenReturn(accountResp("A12","doctor","DOCTOR"));

        // staff thuộc DPT12
        Staff st = staffWithDept("S12","C012","Dr 12", dept("DPT12"));
        when(staffRepository.findByAccountIdAndDeletedAtIsNull("A12"))
                .thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("S12")).thenReturn(true);

        // room history gần nhất
        when(roomTransferHistoryRepository
                .findTopByMedicalRecordIdAndToDepartmentIdOrderByTransferTimeDesc(recordId, "DPT12"))
                .thenReturn(Optional.of(rth("H12")));

        // capture khi save
        ArgumentCaptor<RoomTransferHistory> hCap = ArgumentCaptor.forClass(RoomTransferHistory.class);
        when(roomTransferHistoryRepository.save(any(RoomTransferHistory.class)))
                .thenAnswer(i -> i.getArgument(0));

        ArgumentCaptor<MedicalRecord> rCap = ArgumentCaptor.forClass(MedicalRecord.class);
        when(medicalRecordRepository.save(any(MedicalRecord.class)))
                .thenAnswer(i -> i.getArgument(0));

        // ✅ tạo spy từ service đã InjectMocks
        MedicalRecordServiceImpl spySvc = Mockito.spy(medicalRecordService);
        // chặn getMedicalRecordDetail để không chạy thật (tránh NPE mapper)
        Mockito.doReturn(MedicalRecordDetailResponse.builder().id(recordId).build())
                .when(spySvc).getMedicalRecordDetail(eq(recordId));

        UpdateMedicalRecordRequest req = UpdateMedicalRecordRequest.builder()
                .summary("HIST_SUM")
                .markFinal(false)
                .build();

        // ❗ gọi qua spy
        spySvc.updateMedicalRecord(recordId, req);

        // verify history
        verify(roomTransferHistoryRepository).save(hCap.capture());
        assertEquals("HIST_SUM", hCap.getValue().getConclusionText());
        assertEquals(Boolean.FALSE, hCap.getValue().getIsFinal());

        // verify record
        verify(medicalRecordRepository).save(rCap.capture());
        assertEquals("KEEP_SUM", rCap.getValue().getSummary()); // giữ nguyên summary
    }


    // =========================================================
// 13) summary=null, markFinal=true -> isFinal=true nhưng
//     KHÔNG update record.summary
// =========================================================
    @Test
    void update_summaryNull_markFinalTrue_finalizeHistoryOnly() {
        String recordId = "REC13";
        setAuthUsername("doctor");

        MedicalRecord rec = record(recordId, staff("S0","C0","X"));
        rec.setSummary("SUM_OLD");
        when(medicalRecordRepository.findByIdAndDeletedAtIsNull(recordId))
                .thenReturn(Optional.of(rec));

        Account acc = account("A13","doctor");
        acc.setRoles(Set.of(role("DOCTOR")));
        when(accountRepository.findByUsernameAndDeletedAtIsNull("doctor"))
                .thenReturn(Optional.of(acc));
        when(accountService.getAccount("A13"))
                .thenReturn(accountResp("A13","doctor","DOCTOR"));

        Staff st = staffWithDept("S13","C013","Dr 13", dept("DPT13"));
        when(staffRepository.findByAccountIdAndDeletedAtIsNull("A13"))
                .thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("S13")).thenReturn(true);

        when(roomTransferHistoryRepository
                .findTopByMedicalRecordIdAndToDepartmentIdOrderByTransferTimeDesc(recordId, "DPT13"))
                .thenReturn(Optional.of(rth("H13")));

        ArgumentCaptor<RoomTransferHistory> hCap = ArgumentCaptor.forClass(RoomTransferHistory.class);
        when(roomTransferHistoryRepository.save(any(RoomTransferHistory.class)))
                .thenAnswer(i -> i.getArgument(0));

        ArgumentCaptor<MedicalRecord> rCap = ArgumentCaptor.forClass(MedicalRecord.class);
        when(medicalRecordRepository.save(any(MedicalRecord.class)))
                .thenAnswer(i -> i.getArgument(0));

        // 🔧 Tạo spy từ instance đã @InjectMocks để chặn getMedicalRecordDetail(...)
        MedicalRecordServiceImpl spySvc = Mockito.spy(medicalRecordService);
        Mockito.doReturn(MedicalRecordDetailResponse.builder().id(recordId).build())
                .when(spySvc).getMedicalRecordDetail(recordId);

        UpdateMedicalRecordRequest req = UpdateMedicalRecordRequest.builder()
                .summary(null)
                .markFinal(true)
                .build();

        // 🔧 Gọi qua spy (không gọi instance thật)
        spySvc.updateMedicalRecord(recordId, req);

        verify(roomTransferHistoryRepository).save(hCap.capture());
        assertEquals(Boolean.TRUE, hCap.getValue().getIsFinal());

        verify(medicalRecordRepository).save(rCap.capture());
        assertEquals("SUM_OLD", rCap.getValue().getSummary()); // không đổi

        // (tuỳ thích) đảm bảo hàm detail đã được gọi
        verify(spySvc).getMedicalRecordDetail(recordId);
    }


    // =========================================================
// 14) Cập nhật toàn bộ chỉ số
// =========================================================
    @Test
    void update_updateAllVitals_andSummary() {
        String recordId = "REC14";
        setAuthUsername("doctor");

        MedicalRecord rec = record(recordId, staff("S0","C0","X"));
        // set giá trị cũ khác biệt để so sánh
        rec.setDiagnosisText("OLD_DX");
        rec.setTemperature(36.5);
        rec.setRespiratoryRate(12);
        rec.setBloodPressure("110/70");
        rec.setHeartRate(60);
        rec.setHeightCm(160.0);
        rec.setWeightKg(50.0);
        rec.setBmi(19.5);
        rec.setSpo2(95);
        rec.setSummary("OLD_SUM");

        when(medicalRecordRepository.findByIdAndDeletedAtIsNull(recordId))
                .thenReturn(Optional.of(rec));

        Account acc = account("A14","doctor");
        acc.setRoles(Set.of(role("DOCTOR")));
        when(accountRepository.findByUsernameAndDeletedAtIsNull("doctor"))
                .thenReturn(Optional.of(acc));
        when(accountService.getAccount("A14"))
                .thenReturn(accountResp("A14","doctor","DOCTOR"));

        Staff st = staffWithDept("S14","C014","Dr 14", dept("DPT14"));
        when(staffRepository.findByAccountIdAndDeletedAtIsNull("A14"))
                .thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("S14")).thenReturn(true);

        when(roomTransferHistoryRepository
                .findTopByMedicalRecordIdAndToDepartmentIdOrderByTransferTimeDesc(recordId, "DPT14"))
                .thenReturn(Optional.of(rth("H14")));

        ArgumentCaptor<MedicalRecord> rCap = ArgumentCaptor.forClass(MedicalRecord.class);
        when(medicalRecordRepository.save(any(MedicalRecord.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(roomTransferHistoryRepository.save(any(RoomTransferHistory.class)))
                .thenAnswer(i -> i.getArgument(0));

        // 🔹 Tạo spy từ service đã @InjectMocks
        MedicalRecordServiceImpl spySvc = Mockito.spy(medicalRecordService);
        // 🔹 Chặn getMedicalRecordDetail trên spy để không chạy thật (tránh NPE mapper)
        Mockito.doReturn(MedicalRecordDetailResponse.builder().id(recordId).build())
                .when(spySvc).getMedicalRecordDetail(eq(recordId));

        UpdateMedicalRecordRequest req = UpdateMedicalRecordRequest.builder()
                .diagnosisText("NEW_DX")
                .temperature(38.2)
                .respiratoryRate(20)
                .bloodPressure("130/85")
                .heartRate(88)
                .heightCm(171.5)
                .weightKg(68.2)
                .bmi(23.2)
                .spo2(98)
                .notes("UPDATED")
                .summary("FIN_SUM")
                .markFinal(true)
                .build();

        // 🔹 Gọi QUA SPY (không gọi trực tiếp medicalRecordService)
        spySvc.updateMedicalRecord(recordId, req);

        verify(medicalRecordRepository, atLeastOnce()).save(rCap.capture());
        MedicalRecord saved = rCap.getValue();
        assertEquals("NEW_DX", saved.getDiagnosisText());
        assertEquals(38.2, saved.getTemperature());
        assertEquals(20, saved.getRespiratoryRate());
        assertEquals("130/85", saved.getBloodPressure());
        assertEquals(88, saved.getHeartRate());
        assertEquals(171.5, saved.getHeightCm());
        assertEquals(68.2, saved.getWeightKg());
        assertEquals(23.2, saved.getBmi());
        assertEquals(98, saved.getSpo2());
        assertEquals("UPDATED", saved.getNotes());
        assertEquals("FIN_SUM", saved.getSummary());
        assertNotNull(saved.getUpdatedAt());

        // (tuỳ bạn có muốn verify spy gọi getMedicalRecordDetail hay không)
        verify(spySvc).getMedicalRecordDetail(recordId);
    }


    // =========================================================
// 15) Cập nhật một phần chỉ số (chỉ temperature & spo2 thay đổi)
// =========================================================
    @Test
    void update_partialVitalsOnly_otherFieldsUnchanged() {
        String recordId = "REC15";
        setAuthUsername("doctor");

        MedicalRecord rec = record(recordId, staff("S0","C0","X"));
        rec.setDiagnosisText("DX");
        rec.setTemperature(36.7);
        rec.setRespiratoryRate(14);
        rec.setBloodPressure("118/76");
        rec.setHeartRate(64);
        rec.setHeightCm(168.0);
        rec.setWeightKg(58.0);
        rec.setBmi(20.6);
        rec.setSpo2(96);
        rec.setSummary("SUM");

        when(medicalRecordRepository.findByIdAndDeletedAtIsNull(recordId))
                .thenReturn(Optional.of(rec));

        Account acc = account("A15","doctor");
        acc.setRoles(Set.of(role("DOCTOR")));
        when(accountRepository.findByUsernameAndDeletedAtIsNull("doctor"))
                .thenReturn(Optional.of(acc));
        when(accountService.getAccount("A15"))
                .thenReturn(accountResp("A15","doctor","DOCTOR"));

        Staff st = staffWithDept("S15","C015","Dr 15", dept("DPT15"));
        when(staffRepository.findByAccountIdAndDeletedAtIsNull("A15"))
                .thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("S15")).thenReturn(true);

        when(roomTransferHistoryRepository
                .findTopByMedicalRecordIdAndToDepartmentIdOrderByTransferTimeDesc(recordId, "DPT15"))
                .thenReturn(Optional.of(rth("H15")));

        ArgumentCaptor<MedicalRecord> rCap = ArgumentCaptor.forClass(MedicalRecord.class);
        when(medicalRecordRepository.save(any(MedicalRecord.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(roomTransferHistoryRepository.save(any(RoomTransferHistory.class)))
                .thenAnswer(i -> i.getArgument(0));

        // 🔧 Tạo spy từ service đã @InjectMocks và chặn getMedicalRecordDetail(...)
        MedicalRecordServiceImpl spySvc = Mockito.spy(medicalRecordService);
        Mockito.doReturn(MedicalRecordDetailResponse.builder().id(recordId).build())
                .when(spySvc).getMedicalRecordDetail(eq(recordId));

        UpdateMedicalRecordRequest req = UpdateMedicalRecordRequest.builder()
                .temperature(39.0)  // chỉ đổi 2 field này
                .spo2(99)
                .build();

        // Gọi qua spy (không gọi instance thật)
        spySvc.updateMedicalRecord(recordId, req);

        verify(medicalRecordRepository, atLeastOnce()).save(rCap.capture());
        MedicalRecord saved = rCap.getValue();
        assertEquals(39.0, saved.getTemperature());
        assertEquals(99, saved.getSpo2());

        // các field khác giữ nguyên
        assertEquals("DX", saved.getDiagnosisText());
        assertEquals(14, saved.getRespiratoryRate());
        assertEquals("118/76", saved.getBloodPressure());
        assertEquals(64, saved.getHeartRate());
        assertEquals(168.0, saved.getHeightCm());
        assertEquals(58.0, saved.getWeightKg());
        assertEquals(20.6, saved.getBmi());
        assertEquals("SUM", saved.getSummary());

        // (tuỳ chọn) xác nhận đã gọi getMedicalRecordDetail
        verify(spySvc).getMedicalRecordDetail(recordId);
    }


    // =========================================================
// 16) request.departmentId bị bỏ qua -> luôn dùng staff.department.id
//     để lấy RoomTransferHistory
// =========================================================
    @Test
    void update_requestDepartmentIdIgnored_useStaffDepartmentId() {
        String recordId = "REC16";
        setAuthUsername("doctor");

        when(medicalRecordRepository.findByIdAndDeletedAtIsNull(recordId))
                .thenReturn(Optional.of(record(recordId, staff("S0","C0","X"))));

        Account acc = account("A16","doctor");
        acc.setRoles(Set.of(role("DOCTOR")));
        when(accountRepository.findByUsernameAndDeletedAtIsNull("doctor"))
                .thenReturn(Optional.of(acc));
        when(accountService.getAccount("A16"))
                .thenReturn(accountResp("A16","doctor","DOCTOR"));

        // staff ở DPT_REAL, request sẽ gửi DPT_FAKE
        Staff st = staffWithDept("S16","C016","Dr 16", dept("DPT_REAL"));
        when(staffRepository.findByAccountIdAndDeletedAtIsNull("A16"))
                .thenReturn(Optional.of(st));
        when(workScheduleService.isStaffOnShiftNow("S16")).thenReturn(true);

        when(roomTransferHistoryRepository
                .findTopByMedicalRecordIdAndToDepartmentIdOrderByTransferTimeDesc(recordId, "DPT_REAL"))
                .thenReturn(Optional.of(rth("H16")));

        when(roomTransferHistoryRepository.save(any(RoomTransferHistory.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(medicalRecordRepository.save(any(MedicalRecord.class)))
                .thenAnswer(i -> i.getArgument(0));

        // 🔧 Tạo spy và chặn getMedicalRecordDetail để tránh gọi thật
        MedicalRecordServiceImpl spySvc = Mockito.spy(medicalRecordService);
        Mockito.doReturn(MedicalRecordDetailResponse.builder().id(recordId).build())
                .when(spySvc).getMedicalRecordDetail(eq(recordId));

        UpdateMedicalRecordRequest req = UpdateMedicalRecordRequest.builder()
                .departmentId("DPT_FAKE")  // phải bị bỏ qua
                .summary("S")
                .markFinal(false)
                .build();

        // Gọi QUA spy
        spySvc.updateMedicalRecord(recordId, req);

        // verify truy vấn bằng staff.department.id (DPT_REAL), KHÔNG dùng DPT_FAKE
        verify(roomTransferHistoryRepository)
                .findTopByMedicalRecordIdAndToDepartmentIdOrderByTransferTimeDesc(recordId, "DPT_REAL");
        verify(roomTransferHistoryRepository, never())
                .findTopByMedicalRecordIdAndToDepartmentIdOrderByTransferTimeDesc(eq(recordId), eq("DPT_FAKE"));

        // (tuỳ chọn) đảm bảo getMedicalRecordDetail đã được gọi
        verify(spySvc).getMedicalRecordDetail(recordId);
    }


    // 1) Department không tồn tại
    @Test
    void getOrders_departmentNotFound_throwAppException() {
        String depId = "DPT_X";
        when(departmentRepository.findByIdAndDeletedAtIsNull(depId))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class,
                () -> medicalRecordService.getOrdersByDepartment(depId));
        assertEquals(ErrorCode.DEPARTMENT_NOT_FOUND, ex.getErrorCode());

        verify(departmentRepository).findByIdAndDeletedAtIsNull(depId);
        verifyNoInteractions(medicalOrderRepository);
    }

    // 2) Không có order nào
    @Test
    void getOrders_noOrders_returnEmptyList() {
        String depId = "DPT1";
        when(departmentRepository.findByIdAndDeletedAtIsNull(depId))
                .thenReturn(Optional.of(dept(depId)));
        when(medicalOrderRepository
                .findAllByService_Department_IdAndStatusNotAndDeletedAtIsNull(depId, MedicalOrderStatus.PENDING))
                .thenReturn(List.of());

        List<MedicalRecordOrderResponse> out = medicalRecordService.getOrdersByDepartment(depId);
        assertNotNull(out);
        assertTrue(out.isEmpty());

        verify(medicalOrderRepository)
                .findAllByService_Department_IdAndStatusNotAndDeletedAtIsNull(depId, MedicalOrderStatus.PENDING);
    }

    // 3) 1 order hợp lệ -> map đúng trường
    @Test
    void getOrders_singleOrder_valid_mapsFields() {
        String depId = "DPT2";
        Department d = dept(depId);
        Patient p = new Patient(); p.setId("P1"); p.setFullName("Alice");
        MedicalRecord mr = new MedicalRecord();
        mr.setId("MR1");
        mr.setMedicalRecordCode("HSBA001");
        mr.setPatient(p);

        MedicalService sv = new MedicalService();
        sv.setId("SV1"); sv.setName("Siêu âm"); sv.setDepartment(d);

        MedicalOrder od = new MedicalOrder();
        od.setId("O1");
        od.setMedicalRecord(mr);
        od.setService(sv);
        od.setStatus(MedicalOrderStatus.WAITING);
        od.setCreatedAt(LocalDateTime.of(2025,8,10,9,0));

        when(departmentRepository.findByIdAndDeletedAtIsNull(depId)).thenReturn(Optional.of(d));
        when(medicalOrderRepository
                .findAllByService_Department_IdAndStatusNotAndDeletedAtIsNull(depId, MedicalOrderStatus.PENDING))
                .thenReturn(List.of(od));

        List<MedicalRecordOrderResponse> out = medicalRecordService.getOrdersByDepartment(depId);
        assertEquals(1, out.size());
        MedicalRecordOrderResponse r = out.get(0);
        assertEquals("O1", r.getOrderId());
        assertEquals("MR1", r.getMedicalRecordId());
        assertEquals("HSBA001", r.getMedicalRecordCode());
        assertEquals("Alice", r.getPatientName());
        assertEquals("Siêu âm", r.getServiceName());
        assertEquals(MedicalOrderStatus.WAITING, r.getStatus());
        assertEquals(LocalDateTime.of(2025,8,10,9,0), r.getCreatedAt());
    }

    // 4) Nhiều order hợp lệ -> giữ thứ tự
    @Test
    void getOrders_multipleOrders_keepOrder() {
        String depId = "DPT3";
        Department d = dept(depId);

        Patient p = new Patient(); p.setId("P2"); p.setFullName("Bob");
        MedicalRecord mr = new MedicalRecord();
        mr.setId("MR2");
        mr.setMedicalRecordCode("HSBA002");
        mr.setPatient(p);

        MedicalService sv = new MedicalService();
        sv.setId("SV2"); sv.setName("XN máu"); sv.setDepartment(d);

        MedicalOrder o1 = new MedicalOrder();
        o1.setId("O1"); o1.setMedicalRecord(mr); o1.setService(sv);
        o1.setStatus(MedicalOrderStatus.WAITING);
        o1.setCreatedAt(LocalDateTime.of(2025,8,10,8,0));

        MedicalOrder o2 = new MedicalOrder();
        o2.setId("O2"); o2.setMedicalRecord(mr); o2.setService(sv);
        o2.setStatus(MedicalOrderStatus.COMPLETED);
        o2.setCreatedAt(LocalDateTime.of(2025,8,10,9,0));

        when(departmentRepository.findByIdAndDeletedAtIsNull(depId)).thenReturn(Optional.of(d));
        when(medicalOrderRepository
                .findAllByService_Department_IdAndStatusNotAndDeletedAtIsNull(depId, MedicalOrderStatus.PENDING))
                .thenReturn(List.of(o1, o2));

        List<MedicalRecordOrderResponse> out = medicalRecordService.getOrdersByDepartment(depId);
        assertEquals(List.of("O1", "O2"),
                out.stream().map(MedicalRecordOrderResponse::getOrderId).toList());
    }

    // 5) Không có PENDING trong output
    @Test
    void getOrders_noPendingStatusInOutput() {
        String depId = "DPT4";
        Department d = dept(depId);
        when(departmentRepository.findByIdAndDeletedAtIsNull(depId)).thenReturn(Optional.of(d));

        Patient p = new Patient(); p.setId("P3"); p.setFullName("Cindy");
        MedicalRecord mr = new MedicalRecord();
        mr.setId("MR3");
        mr.setMedicalRecordCode("HSBA003");
        mr.setPatient(p);

        MedicalService sv = new MedicalService();
        sv.setId("SV3"); sv.setName("XN nước tiểu"); sv.setDepartment(d);

        MedicalOrder ok = new MedicalOrder();
        ok.setId("O_OK"); ok.setMedicalRecord(mr); ok.setService(sv);
        ok.setStatus(MedicalOrderStatus.COMPLETED);
        ok.setCreatedAt(LocalDateTime.now());

        when(medicalOrderRepository
                .findAllByService_Department_IdAndStatusNotAndDeletedAtIsNull(depId, MedicalOrderStatus.PENDING))
                .thenReturn(List.of(ok));

        List<MedicalRecordOrderResponse> out = medicalRecordService.getOrdersByDepartment(depId);
        assertTrue(out.stream().noneMatch(r -> r.getStatus() == MedicalOrderStatus.PENDING));
    }

    // 6) Order.status = null -> response.status = null
    @Test
    void getOrders_orderStatusNull_mapsNull() {
        String depId = "DPT5";
        Department d = dept(depId);

        Patient p = new Patient(); p.setId("P4"); p.setFullName("Dan");
        MedicalRecord mr = new MedicalRecord();
        mr.setId("MR4");
        mr.setMedicalRecordCode("HSBA004");
        mr.setPatient(p);

        MedicalService sv = new MedicalService();
        sv.setId("SV4"); sv.setName("Dịch vụ A"); sv.setDepartment(d);

        MedicalOrder o = new MedicalOrder();
        o.setId("O_NULL"); o.setMedicalRecord(mr); o.setService(sv);
        o.setStatus(null);
        o.setCreatedAt(LocalDateTime.now());

        when(departmentRepository.findByIdAndDeletedAtIsNull(depId)).thenReturn(Optional.of(d));
        when(medicalOrderRepository
                .findAllByService_Department_IdAndStatusNotAndDeletedAtIsNull(depId, MedicalOrderStatus.PENDING))
                .thenReturn(List.of(o));

        List<MedicalRecordOrderResponse> out = medicalRecordService.getOrdersByDepartment(depId);
        assertNull(out.get(0).getStatus());
    }

    // 7) Order.service = null -> NPE
    @Test
    void getOrders_orderServiceNull_expectNpe() {
        String depId = "DPT6";
        Department d = dept(depId);

        Patient p = new Patient(); p.setId("P5"); p.setFullName("Eva");
        MedicalRecord mr = new MedicalRecord();
        mr.setId("MR5");
        mr.setMedicalRecordCode("HSBA005");
        mr.setPatient(p);

        MedicalOrder o = new MedicalOrder();
        o.setId("O_SNULL"); o.setMedicalRecord(mr); o.setService(null);
        o.setStatus(MedicalOrderStatus.WAITING);
        o.setCreatedAt(LocalDateTime.now());

        when(departmentRepository.findByIdAndDeletedAtIsNull(depId)).thenReturn(Optional.of(d));
        when(medicalOrderRepository
                .findAllByService_Department_IdAndStatusNotAndDeletedAtIsNull(depId, MedicalOrderStatus.PENDING))
                .thenReturn(List.of(o));

        assertThrows(NullPointerException.class, () -> medicalRecordService.getOrdersByDepartment(depId));
    }

    // 8) Order.medicalRecord = null -> NPE
    @Test
    void getOrders_orderRecordNull_expectNpe() {
        String depId = "DPT7";
        Department d = dept(depId);

        MedicalService sv = new MedicalService();
        sv.setId("SV7"); sv.setName("DV"); sv.setDepartment(d);

        MedicalOrder o = new MedicalOrder();
        o.setId("O_RNULL"); o.setMedicalRecord(null); o.setService(sv);
        o.setStatus(MedicalOrderStatus.WAITING);
        o.setCreatedAt(LocalDateTime.now());

        when(departmentRepository.findByIdAndDeletedAtIsNull(depId)).thenReturn(Optional.of(d));
        when(medicalOrderRepository
                .findAllByService_Department_IdAndStatusNotAndDeletedAtIsNull(depId, MedicalOrderStatus.PENDING))
                .thenReturn(List.of(o));

        assertThrows(NullPointerException.class, () -> medicalRecordService.getOrdersByDepartment(depId));
    }

    // 9) record.patient = null -> NPE
    @Test
    void getOrders_recordPatientNull_expectNpe() {
        String depId = "DPT8";
        Department d = dept(depId);

        MedicalRecord mr = new MedicalRecord();
        mr.setId("MR8");
        mr.setMedicalRecordCode("HSBA008");
        mr.setPatient(null);

        MedicalService sv = new MedicalService();
        sv.setId("SV8"); sv.setName("DV8"); sv.setDepartment(d);

        MedicalOrder o = new MedicalOrder();
        o.setId("O_PNULL"); o.setMedicalRecord(mr); o.setService(sv);
        o.setStatus(MedicalOrderStatus.WAITING);
        o.setCreatedAt(LocalDateTime.now());

        when(departmentRepository.findByIdAndDeletedAtIsNull(depId)).thenReturn(Optional.of(d));
        when(medicalOrderRepository
                .findAllByService_Department_IdAndStatusNotAndDeletedAtIsNull(depId, MedicalOrderStatus.PENDING))
                .thenReturn(List.of(o));

        assertThrows(NullPointerException.class, () -> medicalRecordService.getOrdersByDepartment(depId));
    }

    // 10) patient.fullName = null -> patientName null
    @Test
    void getOrders_patientFullNameNull_mapsNullPatientName() {
        String depId = "DPT9";
        Department d = dept(depId);

        Patient p = new Patient(); p.setId("P9"); p.setFullName(null);
        MedicalRecord mr = new MedicalRecord();
        mr.setId("MR9");
        mr.setMedicalRecordCode("HSBA009");
        mr.setPatient(p);

        MedicalService sv = new MedicalService();
        sv.setId("SV9"); sv.setName("DV9"); sv.setDepartment(d);

        MedicalOrder o = new MedicalOrder();
        o.setId("O_FNNULL"); o.setMedicalRecord(mr); o.setService(sv);
        o.setStatus(MedicalOrderStatus.COMPLETED);
        o.setCreatedAt(LocalDateTime.now());

        when(departmentRepository.findByIdAndDeletedAtIsNull(depId)).thenReturn(Optional.of(d));
        when(medicalOrderRepository
                .findAllByService_Department_IdAndStatusNotAndDeletedAtIsNull(depId, MedicalOrderStatus.PENDING))
                .thenReturn(List.of(o));

        List<MedicalRecordOrderResponse> out = medicalRecordService.getOrdersByDepartment(depId);
        assertEquals(1, out.size());
        assertNull(out.get(0).getPatientName());
    }

    // 11) Repo trả null -> NPE
    @Test
    void getOrders_repoReturnsNull_expectNpe() {
        String depId = "DPT10";
        when(departmentRepository.findByIdAndDeletedAtIsNull(depId)).thenReturn(Optional.of(dept(depId)));
        when(medicalOrderRepository
                .findAllByService_Department_IdAndStatusNotAndDeletedAtIsNull(depId, MedicalOrderStatus.PENDING))
                .thenReturn(null);

        assertThrows(NullPointerException.class, () -> medicalRecordService.getOrdersByDepartment(depId));
    }

    // 12) Verify tương tác repo
    @Test
    void getOrders_verifyRepositoryInteractions() {
        String depId = "DPT11";

        // NOT FOUND -> không gọi order repo
        when(departmentRepository.findByIdAndDeletedAtIsNull(depId)).thenReturn(Optional.empty());
        assertThrows(AppException.class, () -> medicalRecordService.getOrdersByDepartment(depId));
        verify(departmentRepository).findByIdAndDeletedAtIsNull(depId);
        verifyNoInteractions(medicalOrderRepository);

        // FOUND -> gọi đúng tham số
        reset(departmentRepository, medicalOrderRepository);
        when(departmentRepository.findByIdAndDeletedAtIsNull(depId)).thenReturn(Optional.of(dept(depId)));
        when(medicalOrderRepository
                .findAllByService_Department_IdAndStatusNotAndDeletedAtIsNull(depId, MedicalOrderStatus.PENDING))
                .thenReturn(List.of());

        medicalRecordService.getOrdersByDepartment(depId);

        verify(departmentRepository).findByIdAndDeletedAtIsNull(depId);
        verify(medicalOrderRepository)
                .findAllByService_Department_IdAndStatusNotAndDeletedAtIsNull(depId, MedicalOrderStatus.PENDING);
    }
}
