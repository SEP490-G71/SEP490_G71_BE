package vn.edu.fpt.medicaldiagnosis.service;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import vn.edu.fpt.medicaldiagnosis.config.CallbackRegistry;
import vn.edu.fpt.medicaldiagnosis.dto.request.QueuePatientsRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.QueuePatientsResponse;
import vn.edu.fpt.medicaldiagnosis.entity.*;
import vn.edu.fpt.medicaldiagnosis.enums.DepartmentType;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.mapper.QueuePatientsMapper;
import vn.edu.fpt.medicaldiagnosis.repository.*;
import vn.edu.fpt.medicaldiagnosis.service.impl.QueuePatientsServiceImpl;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class QueuePatientsServiceTest {

    @Mock
    private DailyQueueService dailyQueueService;

    @InjectMocks
    private QueuePatientsServiceImpl queuePatientsService;

    @Mock
    private DepartmentRepository departmentRepository;

    @Mock
    private SpecializationRepository specializationRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private QueuePatientsRepository queuePatientsRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private StaffRepository staffRepository;

    @Mock
    private DailyQueueRepository dailyQueueRepository;

    @Mock
    private WorkScheduleService workScheduleService;

    @Mock
    private CallbackRegistry callbackRegistry;

    @Mock
    private QueuePatientsMapper queuePatientsMapper;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @Test
    public void testCreateQueuePatients_Success() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("101");
        request.setRegisteredTime(LocalDateTime.now());

        Specialization specialization = new Specialization();
        specialization.setId("1");

        Patient patient = new Patient();
        patient.setId("1");

        // Create mock Department object
        Department department = new Department();
        department.setId("1");
        department.setName("Consultation Room");
        department.setSpecialization(specialization);

        // Create mock Staff object and assign it to the department
        Staff staff = new Staff();
        staff.setId("staff1");
        staff.setDepartment(department);

        // Create mock Account and associate with Staff
        Account account = new Account();
        account.setId("account1");
        account.setUsername("username");

        // Mock SecurityContext and Authentication
        try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class)) {
            mocked.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("username");

            // Mock repository calls
            when(specializationRepository.findById("1")).thenReturn(Optional.of(specialization));
            when(patientRepository.findByIdAndDeletedAtIsNull("1")).thenReturn(Optional.of(patient));
            when(departmentRepository.findByTypeAndSpecializationId("CONSULTATION", "1")).thenReturn(Optional.of(department));
            when(queuePatientsRepository.countActiveVisits("queue1", "1")).thenReturn(0);

            // Mock account and staff repository
            when(accountRepository.findByUsernameAndDeletedAtIsNull("username")).thenReturn(Optional.of(account));
            when(staffRepository.findByAccountIdAndDeletedAtIsNull(account.getId())).thenReturn(Optional.of(staff));

            // Mock work schedule service to indicate staff is on shift
            when(workScheduleService.isStaffOnShiftNow(staff.getId())).thenReturn(true);

            // Mock countAvailableRoomsBySpecialization to return 1 (room is available)
            when(departmentRepository.countAvailableRoomsBySpecialization("1")).thenReturn(1);
            when(departmentRepository.findByTypeAndRoomNumberAndSpecializationId("CONSULTATION", "101", "1"))
                    .thenReturn(Optional.of(new Department()));

            // Mock dailyQueueService to return a valid queueId for today
            when(dailyQueueService.getActiveQueueIdForToday()).thenReturn("queue1");

            // Create QueuePatient mock object
            QueuePatients queuePatient = new QueuePatients();
            queuePatient.setQueueId("queue1");
            queuePatient.setPatientId("1");
            queuePatient.setRoomNumber("101");
            queuePatient.setSpecialization(specialization);

            // Mock repository save
            when(queuePatientsRepository.save(any(QueuePatients.class))).thenReturn(queuePatient);

            // Create mock response
            QueuePatientsResponse response = new QueuePatientsResponse();
            response.setQueueId("queue1");  // Set expected value
            when(queuePatientsMapper.toResponse(queuePatient)).thenReturn(response);

            // Call the service method
            QueuePatientsResponse result = queuePatientsService.createQueuePatients(request);
            log.info("Successfully created QueuePatient with queueId: {}", result.getQueueId());
            // Assert the results
            assertNotNull(result);
            assertEquals("queue1", result.getQueueId());  // Ensure the queueId matches
            verify(callbackRegistry).register("1");
        }
    }

    @Test(expected = AppException.class)
    public void testCreateQueuePatients_SpecializationNotFound() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("999");  // Invalid specialization ID
        request.setRoomNumber("101");
        request.setRegisteredTime(LocalDateTime.now());

        // Mock repository calls
        when(specializationRepository.findById("999")).thenReturn(Optional.empty());

        // Call the service method (expecting an exception)
        queuePatientsService.createQueuePatients(request);
    }

    @Test(expected = AppException.class)
    public void testCreateQueuePatients_InvalidRoomForSpecialization() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("999");  // Invalid room number
        request.setRegisteredTime(LocalDateTime.now());

        // Mock repository calls
        when(departmentRepository.findByTypeAndSpecializationId("CONSULTATION", "1"))
                .thenReturn(Optional.empty());  // No matching room for the given specialization

        // Call the service method (expecting an exception)
        queuePatientsService.createQueuePatients(request);
    }

    @Test(expected = AppException.class)
    public void testCreateQueuePatients_RoomOverloaded() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("101");
        request.setRegisteredTime(LocalDateTime.now());

        // Mock department repository to simulate overloaded room
        when(departmentRepository.countAvailableRoomsBySpecialization("1")).thenReturn(0);  // No available rooms

        // Call the service method (expecting an exception)
        queuePatientsService.createQueuePatients(request);
    }

    @Test(expected = AppException.class)
    public void testCreateQueuePatients_AlreadyInQueue() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("101");
        request.setRegisteredTime(LocalDateTime.now());

        // Mock patient repository to simulate patient already in the queue
        when(queuePatientsRepository.countActiveVisits("queue1", "1")).thenReturn(1);  // Patient already in queue

        // Call the service method (expecting an exception)
        queuePatientsService.createQueuePatients(request);
    }

    @Test
    public void testCreateQueuePatients_PriorityForFutureDate() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("101");
        request.setRegisteredTime(LocalDateTime.now().plusDays(1));  // Future date

        Specialization specialization = new Specialization();
        specialization.setId("1");

        Patient patient = new Patient();
        patient.setId("1");

        // Mock department data (2 rooms available)
        Department room101 = new Department();
        room101.setId("1");
        room101.setName("Consultation Room 101");
        room101.setSpecialization(specialization);

        Department room102 = new Department();
        room102.setId("2");
        room102.setName("Consultation Room 102");
        room102.setSpecialization(specialization);

        // Mock queue data
        String queueId = "queue1";
        when(dailyQueueService.getActiveQueueIdForToday()).thenReturn(queueId);

        // Mock department repository to return available rooms
        when(departmentRepository.findByTypeAndSpecializationId("CONSULTATION", "1"))
                .thenReturn(Optional.of(room101));  // First department (room 101)
        when(departmentRepository.countAvailableRoomsBySpecialization("1")).thenReturn(2); // Mock 2 available rooms
        when(departmentRepository.findByTypeAndRoomNumberAndSpecializationId("CONSULTATION", "101", "1"))
                .thenReturn(Optional.of(room101)); // Mock the valid room

        // Mock patient repository
        when(patientRepository.findByIdAndDeletedAtIsNull("1"))
                .thenReturn(Optional.of(patient));

        // Mock specialization repository to return a valid specialization
        when(specializationRepository.findById("1"))
                .thenReturn(Optional.of(specialization));

        // Mock the callback registry
        doNothing().when(callbackRegistry).register(anyString());

        // Mock the repository save call
        QueuePatients savedQueuePatient = new QueuePatients();
        savedQueuePatient.setQueueId(queueId);
        savedQueuePatient.setPatientId(patient.getId());
        savedQueuePatient.setRoomNumber("101");
        savedQueuePatient.setSpecialization(specialization);

        when(queuePatientsRepository.save(any(QueuePatients.class))).thenReturn(savedQueuePatient);

        // Mock queuePatientsMapper
        QueuePatientsResponse response = new QueuePatientsResponse();
        response.setQueueId(queueId);
        when(queuePatientsMapper.toResponse(savedQueuePatient)).thenReturn(response);

        // Mock the dailyQueueRepository for active queueId (this is the fix for NPE)
        DailyQueue dailyQueue = new DailyQueue();
        dailyQueue.setId(queueId);
        when(dailyQueueRepository.findByQueueDateAndDeletedAtIsNull(any(LocalDateTime.class)))
                .thenReturn(Optional.of(dailyQueue));  // Mock the valid queue

        // Mock SecurityContext and Authentication
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("username");  // Mock valid username
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Mock accountRepository to return a valid account
        Account account = new Account();
        account.setUsername("username");
        when(accountRepository.findByUsernameAndDeletedAtIsNull("username")).thenReturn(Optional.of(account));

        // Mock staffRepository to return a valid staff
        Staff staff = new Staff();
        staff.setId("staff1");
        staff.setDepartment(room101);  // Assign staff to the room
        when(staffRepository.findByAccountIdAndDeletedAtIsNull(account.getId())).thenReturn(Optional.of(staff));

        // Mock workScheduleService to simulate the staff is on shift
        when(workScheduleService.isStaffOnShiftNow(staff.getId())).thenReturn(true);  // Simulate staff is on shift

        // Call the service method
        QueuePatientsResponse result = queuePatientsService.createQueuePatients(request);

        // Assert the results
        assertNotNull(result);
        assertEquals("queue1", result.getQueueId());
        assertEquals(true, result.getIsPriority());
    }

    @Test(expected = AppException.class)
    public void testCreateQueuePatients_InvalidRoomAssignment() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("999");  // Invalid room number
        request.setRegisteredTime(LocalDateTime.now());

        // Mock repository calls for invalid room
        when(departmentRepository.findByTypeAndRoomNumberAndSpecializationId("CONSULTATION", "999", "1"))
                .thenReturn(Optional.empty());  // No valid room

        // Call the service method (expecting an exception)
        queuePatientsService.createQueuePatients(request);
    }

    @Test(expected = AppException.class)
    public void testCreateQueuePatients_FutureDateWithNoAvailableRooms() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("101");
        request.setRegisteredTime(LocalDateTime.now().plusDays(1));  // Future date

        // Mock repository calls for no available rooms
        when(departmentRepository.countAvailableRoomsBySpecialization("1")).thenReturn(0);  // No available rooms

        // Call the service method (expecting an exception)
        queuePatientsService.createQueuePatients(request);
    }

    @Test(expected = AppException.class)
    public void testCreateQueuePatients_InvalidPatientId() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("999");  // Invalid patient ID
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("101");
        request.setRegisteredTime(LocalDateTime.now());

        // Mock repository call for invalid patient
        when(patientRepository.findByIdAndDeletedAtIsNull("999")).thenReturn(Optional.empty());

        // Call the service method (expecting an exception)
        queuePatientsService.createQueuePatients(request);
    }

    @Test(expected = AppException.class)
    public void testCreateQueuePatients_SpecializationMismatch() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("2");  // Mismatched specialization ID
        request.setRoomNumber("101");
        request.setRegisteredTime(LocalDateTime.now());

        // Mock repository call for specialization mismatch
        Specialization wrongSpecialization = new Specialization();
        wrongSpecialization.setId("2");
        when(specializationRepository.findById("2")).thenReturn(Optional.of(wrongSpecialization));

        // Mock department data with a different specialization
        Department department = new Department();
        department.setId("1");
        department.setName("Consultation Room");
        department.setSpecialization(new Specialization());  // Different specialization
        when(departmentRepository.findByTypeAndSpecializationId("CONSULTATION", "2")).thenReturn(Optional.of(department));

        // Call the service method (expecting an exception)
        queuePatientsService.createQueuePatients(request);
    }

    @Test(expected = AppException.class)
    public void testCreateQueuePatients_FutureDateRoomUnavailable() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("101");
        request.setRegisteredTime(LocalDateTime.now().plusDays(1));  // Future date

        // Mock repository calls for room unavailability
        when(departmentRepository.findByTypeAndRoomNumberAndSpecializationId("CONSULTATION", "101", "1"))
                .thenReturn(Optional.empty());  // Room is unavailable

        // Call the service method (expecting an exception)
        queuePatientsService.createQueuePatients(request);
    }

    @Test(expected = AppException.class)
    public void testCreateQueuePatients_StaffNotOnShift() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("101");
        request.setRegisteredTime(LocalDateTime.now());

        Specialization specialization = new Specialization();
        specialization.setId("1");

        Patient patient = new Patient();
        patient.setId("1");

        Department department = new Department();
        department.setId("1");
        department.setName("Consultation Room");
        department.setSpecialization(specialization);

        Staff staff = new Staff();
        staff.setId("staff1");
        staff.setDepartment(department);

        // Mock SecurityContext and Authentication
        try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class)) {
            mocked.when(SecurityContextHolder::getContext).thenReturn(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("username");

            // Mock repository calls
            when(specializationRepository.findById("1")).thenReturn(Optional.of(specialization));
            when(patientRepository.findByIdAndDeletedAtIsNull("1")).thenReturn(Optional.of(patient));
            when(departmentRepository.findByTypeAndSpecializationId("CONSULTATION", "1")).thenReturn(Optional.of(department));

            // Mock work schedule service to indicate staff is not on shift
            when(workScheduleService.isStaffOnShiftNow(staff.getId())).thenReturn(false);  // Staff is not on shift

            // Call the service method (expecting an exception)
            queuePatientsService.createQueuePatients(request);
        }
    }

    @Test(expected = AppException.class)
    public void testCreateQueuePatients_SpecializationNotAssignedToDepartment() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("101");
        request.setRegisteredTime(LocalDateTime.now());

        // Mock repository call for department without the required specialization
        Department department = new Department();
        department.setId("1");
        department.setName("Consultation Room");
        department.setSpecialization(new Specialization());  // No matching specialization
        when(departmentRepository.findByTypeAndSpecializationId("CONSULTATION", "1"))
                .thenReturn(Optional.of(department));

        // Call the service method (expecting an exception)
        queuePatientsService.createQueuePatients(request);
    }

    @Test(expected = AppException.class)
    public void testCreateQueuePatients_InvalidRoomNumberFormat() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("INVALID_ROOM");  // Invalid room number
        request.setRegisteredTime(LocalDateTime.now());

        // Mock repository calls for invalid room number
        when(departmentRepository.findByTypeAndRoomNumberAndSpecializationId("CONSULTATION", "INVALID_ROOM", "1"))
                .thenReturn(Optional.empty());  // Invalid room number format

        // Call the service method (expecting an exception)
        queuePatientsService.createQueuePatients(request);
    }

    @Test(expected = AppException.class)
    public void testCreateQueuePatients_SecurityContextFailure() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("101");
        request.setRegisteredTime(LocalDateTime.now());

        // Simulate security context failure
        try (MockedStatic<SecurityContextHolder> mocked = mockStatic(SecurityContextHolder.class)) {
            mocked.when(SecurityContextHolder::getContext).thenReturn(null);  // Null security context

            // Call the service method (expecting an exception)
            queuePatientsService.createQueuePatients(request);
        }
    }

    @Test(expected = AppException.class)
    public void testCreateQueuePatients_SpecializationIdMismatchInDepartmentAndRequest() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");  // Mismatched specialization ID
        request.setRoomNumber("101");
        request.setRegisteredTime(LocalDateTime.now());

        // Mock department with a different specialization
        Specialization wrongSpecialization = new Specialization();
        wrongSpecialization.setId("2");
        Department department = new Department();
        department.setId("1");
        department.setName("Consultation Room");
        department.setSpecialization(wrongSpecialization);  // Different specialization
        when(departmentRepository.findByTypeAndSpecializationId("CONSULTATION", "1"))
                .thenReturn(Optional.of(department));

        // Call the service method (expecting an exception)
        queuePatientsService.createQueuePatients(request);
    }

    @Test(expected = AppException.class)
    public void testCreateQueuePatients_ValidRoomButDepartmentInactive() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("101");
        request.setRegisteredTime(LocalDateTime.now());

        // Mock repository calls for inactive (overloaded) department
        Department department = new Department();
        department.setId("1");
        department.setName("Consultation Room");
        department.setSpecialization(new Specialization());
        department.setOverloaded(true);  // Mark the department as overloaded/inactive
        when(departmentRepository.findByTypeAndRoomNumberAndSpecializationId("CONSULTATION", "101", "1"))
                .thenReturn(Optional.of(department));

        // Call the service method (expecting an exception)
        queuePatientsService.createQueuePatients(request);
    }

    @Test(expected = AppException.class)
    public void testCreateQueuePatients_MissingSpecializationId() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId(null);  // Missing specialization ID
        request.setRoomNumber("101");
        request.setRegisteredTime(LocalDateTime.now());

        // Call the service method (expecting an exception)
        queuePatientsService.createQueuePatients(request);
    }

    @Test(expected = AppException.class)
    public void testCreateQueuePatients_FutureDate_WithNoAvailableQueue() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("101");
        request.setRegisteredTime(LocalDateTime.now().plusDays(1));  // Future date

        // Mock repository calls for no available queue
        when(dailyQueueService.getActiveQueueIdForToday()).thenReturn(null);  // No active queue for today

        // Call the service method (expecting an exception)
        queuePatientsService.createQueuePatients(request);
    }

    @Test
    public void testCreateQueuePatients_QueueAssignmentForSpecialization() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("101");
        request.setRegisteredTime(LocalDateTime.now());

        Specialization specialization = new Specialization();
        specialization.setId("1");

        Patient patient = new Patient();
        patient.setId("1");

        Department department = new Department();
        department.setId("1");
        department.setName("Consultation Room");
        department.setSpecialization(specialization);

        // Mock repository calls
        when(specializationRepository.findById("1")).thenReturn(Optional.of(specialization));
        when(patientRepository.findByIdAndDeletedAtIsNull("1")).thenReturn(Optional.of(patient));
        when(departmentRepository.findByTypeAndSpecializationId("CONSULTATION", "1")).thenReturn(Optional.of(department));

        // Mock save call
        QueuePatients savedQueuePatient = new QueuePatients();
        savedQueuePatient.setQueueId("queue1");
        savedQueuePatient.setPatientId("1");
        savedQueuePatient.setRoomNumber("101");
        savedQueuePatient.setSpecialization(specialization);

        when(queuePatientsRepository.save(any(QueuePatients.class))).thenReturn(savedQueuePatient);

        // Call the service method
        QueuePatientsResponse result = queuePatientsService.createQueuePatients(request);

        // Assert the results
        assertNotNull(result);
        assertEquals("101", result.getRoomNumber());  // Ensure the room matches
    }

    @Test(expected = AppException.class)
    public void testCreateQueuePatients_OverloadedRoomCheck() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("101");
        request.setRegisteredTime(LocalDateTime.now());

        // Mock repository calls for overloaded room
        when(departmentRepository.countAvailableRoomsBySpecialization("1")).thenReturn(0);  // No available rooms

        // Call the service method (expecting an exception)
        queuePatientsService.createQueuePatients(request);
    }

    @Test
    public void testCreateQueuePatients_ValidPatientAndRoom() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("101");
        request.setRegisteredTime(LocalDateTime.now());

        Specialization specialization = new Specialization();
        specialization.setId("1");

        Patient patient = new Patient();
        patient.setId("1");

        Department department = new Department();
        department.setId("1");
        department.setName("Consultation Room");
        department.setSpecialization(specialization);

        // Mock repository calls
        when(specializationRepository.findById("1")).thenReturn(Optional.of(specialization));
        when(patientRepository.findByIdAndDeletedAtIsNull("1")).thenReturn(Optional.of(patient));
        when(departmentRepository.findByTypeAndRoomNumberAndSpecializationId("CONSULTATION", "101", "1"))
                .thenReturn(Optional.of(department));

        // Mock save call
        QueuePatients savedQueuePatient = new QueuePatients();
        savedQueuePatient.setQueueId("queue1");
        savedQueuePatient.setPatientId("1");
        savedQueuePatient.setRoomNumber("101");
        savedQueuePatient.setSpecialization(specialization);

        when(queuePatientsRepository.save(any(QueuePatients.class))).thenReturn(savedQueuePatient);

        // Call the service method
        QueuePatientsResponse result = queuePatientsService.createQueuePatients(request);

        // Assert the results
        assertNotNull(result);
        assertEquals("queue1", result.getQueueId());  // Ensure the queueId matches
        assertEquals("101", result.getRoomNumber());  // Ensure the room number matches
    }

    @Test(expected = AppException.class)
    public void testCreateQueuePatients_EmptyPatientId() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("");  // Empty patient ID
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("101");
        request.setRegisteredTime(LocalDateTime.now());

        // Call the service method (expecting an exception)
        queuePatientsService.createQueuePatients(request);
    }

    @Test(expected = AppException.class)
    public void testCreateQueuePatients_RoomNumberNotAllowed() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("999");  // Invalid room number
        request.setRegisteredTime(LocalDateTime.now());

        // Mock repository call for invalid room
        when(departmentRepository.findByTypeAndRoomNumberAndSpecializationId("CONSULTATION", "999", "1"))
                .thenReturn(Optional.empty());  // Invalid room number

        // Call the service method (expecting an exception)
        queuePatientsService.createQueuePatients(request);
    }

    @Test
    public void testCreateQueuePatients_ValidRoomWithSpecializationMatch() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("101");
        request.setRegisteredTime(LocalDateTime.now());

        Specialization specialization = new Specialization();
        specialization.setId("1");

        Patient patient = new Patient();
        patient.setId("1");

        Department department = new Department();
        department.setId("1");
        department.setName("Consultation Room");
        department.setSpecialization(specialization);

        // Mock repository calls
        when(specializationRepository.findById("1")).thenReturn(Optional.of(specialization));
        when(patientRepository.findByIdAndDeletedAtIsNull("1")).thenReturn(Optional.of(patient));
        when(departmentRepository.findByTypeAndRoomNumberAndSpecializationId("CONSULTATION", "101", "1"))
                .thenReturn(Optional.of(department));

        // Mock save call
        QueuePatients savedQueuePatient = new QueuePatients();
        savedQueuePatient.setQueueId("queue1");
        savedQueuePatient.setPatientId("1");
        savedQueuePatient.setRoomNumber("101");
        savedQueuePatient.setSpecialization(specialization);

        when(queuePatientsRepository.save(any(QueuePatients.class))).thenReturn(savedQueuePatient);

        // Call the service method
        QueuePatientsResponse result = queuePatientsService.createQueuePatients(request);

        // Assert the results
        assertNotNull(result);
        assertEquals("queue1", result.getQueueId());
        assertEquals("101", result.getRoomNumber());  // Room should match
        assertEquals("1", result.getSpecialization().getId());  // Specialization should match
    }

    @Test
    public void testCreateQueuePatients_SpecializationWithMultipleRooms() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("101");  // Requesting room 101
        request.setRegisteredTime(LocalDateTime.now());

        Specialization specialization = new Specialization();
        specialization.setId("1");

        Patient patient = new Patient();
        patient.setId("1");

        Department room101 = new Department();
        room101.setId("1");
        room101.setName("Consultation Room 101");
        room101.setSpecialization(specialization);
        room101.setRoomNumber("101");

        Department room102 = new Department();
        room102.setId("2");
        room102.setName("Consultation Room 102");
        room102.setSpecialization(specialization);
        room102.setRoomNumber("102");

        // Mock repository calls
        when(specializationRepository.findById("1")).thenReturn(Optional.of(specialization));
        when(patientRepository.findByIdAndDeletedAtIsNull("1")).thenReturn(Optional.of(patient));
        when(departmentRepository.findByTypeAndSpecializationId("CONSULTATION", "1"))
                .thenReturn(Optional.of(room101));  // Return a valid room (101)
        when(departmentRepository.countAvailableRoomsBySpecialization("1")).thenReturn(2);  // Two rooms available
        when(departmentRepository.findByTypeAndRoomNumberAndSpecializationId("CONSULTATION", "101", "1"))
                .thenReturn(Optional.of(room101));  // Room 101 should be valid
        when(departmentRepository.findByTypeAndRoomNumberAndSpecializationId("CONSULTATION", "102", "1"))
                .thenReturn(Optional.of(room102));  // Room 102 should be valid

        // Mock the dailyQueueService to return a valid queue ID
        String queueId = "queue1";
        when(dailyQueueService.getActiveQueueIdForToday()).thenReturn(queueId);  // Mock a valid queue ID

        // Mock save call
        QueuePatients savedQueuePatient = new QueuePatients();
        savedQueuePatient.setQueueId(queueId);
        savedQueuePatient.setPatientId("1");
        savedQueuePatient.setRoomNumber("101");
        savedQueuePatient.setSpecialization(specialization);

        when(queuePatientsRepository.save(any(QueuePatients.class))).thenReturn(savedQueuePatient);

        // Mock the SecurityContext and Authentication
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("username");  // Mocked username
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);  // Set the mocked context

        // Mock the accountRepository to return a valid account
        Account account = new Account();
        account.setUsername("username");  // Matching username
        when(accountRepository.findByUsernameAndDeletedAtIsNull("username")).thenReturn(Optional.of(account));  // Mock valid account

        // Mock the staffRepository to return valid staff information linked to the account
        Staff staff = new Staff();
        staff.setId("staff1");
        staff.setDepartment(room101);  // Staff assigned to room101 (department)
        when(staffRepository.findByAccountIdAndDeletedAtIsNull(account.getId())).thenReturn(Optional.of(staff));  // Mock valid staff for account

        // Mock the workScheduleService to ensure the staff is on shift
        when(workScheduleService.isStaffOnShiftNow(staff.getId())).thenReturn(true);  // Staff is on shift

        // Mock the queuePatientsMapper to return a valid response
        QueuePatientsResponse queuePatientsResponse = new QueuePatientsResponse();
        queuePatientsResponse.setQueueId(queueId);
        queuePatientsResponse.setRoomNumber("101");
        when(queuePatientsMapper.toResponse(savedQueuePatient)).thenReturn(queuePatientsResponse);  // Mocked response

        // Call the service method
        QueuePatientsResponse result = queuePatientsService.createQueuePatients(request);

        // Assert the results
        assertNotNull(result);
        assertEquals(queueId, result.getQueueId());
        assertTrue(result.getRoomNumber().equals("101") || result.getRoomNumber().equals("102"));  // Room should be either 101 or 102
    }

    @Test(expected = AppException.class)
    public void testCreateQueuePatients_RoomNumberConflict() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("101");  // Room is already occupied
        request.setRegisteredTime(LocalDateTime.now());

        Specialization specialization = new Specialization();
        specialization.setId("1");

        Patient patient = new Patient();
        patient.setId("1");

        // Mock department with room already assigned
        Department department = new Department();
        department.setId("1");
        department.setName("Consultation Room");
        department.setSpecialization(specialization);

        // Mock repository calls for room conflict
        when(departmentRepository.findByTypeAndRoomNumberAndSpecializationId("CONSULTATION", "101", "1"))
                .thenReturn(Optional.of(department));  // Room is already occupied

        // Call the service method (expecting an exception)
        queuePatientsService.createQueuePatients(request);
    }

    @Test(expected = AppException.class)
    public void testCreateQueuePatients_PatientNotAssignedToAnyQueue() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("101");
        request.setRegisteredTime(LocalDateTime.now());

        // Mock repository calls for no valid department
        when(patientRepository.findByIdAndDeletedAtIsNull("1")).thenReturn(Optional.of(new Patient()));

        // Mock no matching department
        when(departmentRepository.findByTypeAndSpecializationId("CONSULTATION", "1"))
                .thenReturn(Optional.empty());  // No valid department available

        // Call the service method (expecting an exception)
        queuePatientsService.createQueuePatients(request);
    }

    @Test(expected = AppException.class)
    public void testCreateQueuePatients_StaffNotFoundForQueueAssignment() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("101");
        request.setRegisteredTime(LocalDateTime.now());

        Specialization specialization = new Specialization();
        specialization.setId("1");

        Patient patient = new Patient();
        patient.setId("1");

        Department department = new Department();
        department.setId("1");
        department.setName("Consultation Room");
        department.setSpecialization(specialization);

        // Mock repository calls for no staff available
        when(specializationRepository.findById("1")).thenReturn(Optional.of(specialization));
        when(patientRepository.findByIdAndDeletedAtIsNull("1")).thenReturn(Optional.of(patient));
        when(departmentRepository.findByTypeAndRoomNumberAndSpecializationId("CONSULTATION", "101", "1"))
                .thenReturn(Optional.of(department));

        // Mock no staff available for the department
        when(staffRepository.findByAccountIdAndDeletedAtIsNull(anyString())).thenReturn(Optional.empty());

        // Call the service method (expecting an exception)
        queuePatientsService.createQueuePatients(request);
    }

    @Test(expected = AppException.class)
    public void testCreateQueuePatients_QueueServiceException() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("101");
        request.setRegisteredTime(LocalDateTime.now());

        // Mock queue service to throw an exception
        when(dailyQueueService.getActiveQueueIdForToday()).thenThrow(new RuntimeException("Queue service error"));

        // Call the service method (expecting an exception)
        queuePatientsService.createQueuePatients(request);
    }

    @Test(expected = AppException.class)
    public void testCreateQueuePatients_ValidRequestWithSpecializationNotAssignedToRoom() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("101");
        request.setRegisteredTime(LocalDateTime.now());

        Specialization specialization = new Specialization();
        specialization.setId("1");

        Patient patient = new Patient();
        patient.setId("1");

        // Mock repository calls for a department with a different specialization
        Department department = new Department();
        department.setId("1");
        department.setName("Consultation Room");
        department.setSpecialization(new Specialization());  // Different specialization
        when(specializationRepository.findById("1")).thenReturn(Optional.of(specialization));
        when(patientRepository.findByIdAndDeletedAtIsNull("1")).thenReturn(Optional.of(patient));
        when(departmentRepository.findByTypeAndRoomNumberAndSpecializationId("CONSULTATION", "101", "1"))
                .thenReturn(Optional.of(department));

        // Call the service method (expecting an exception)
        queuePatientsService.createQueuePatients(request);
    }

    @Test(expected = AppException.class)
    public void testCreateQueuePatients_PatientAlreadyInOtherRoom() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("101");
        request.setRegisteredTime(LocalDateTime.now());

        Specialization specialization = new Specialization();
        specialization.setId("1");

        Patient patient = new Patient();
        patient.setId("1");

        Department department = new Department();
        department.setId("1");
        department.setName("Consultation Room");
        department.setSpecialization(specialization);

        // Mock repository calls for patient already in another room
        when(specializationRepository.findById("1")).thenReturn(Optional.of(specialization));
        when(patientRepository.findByIdAndDeletedAtIsNull("1")).thenReturn(Optional.of(patient));
        when(departmentRepository.findByTypeAndRoomNumberAndSpecializationId("CONSULTATION", "101", "1"))
                .thenReturn(Optional.of(department));
        when(queuePatientsRepository.countActiveVisits("queue1", "1")).thenReturn(1);  // Patient already in queue

        // Call the service method (expecting an exception)
        queuePatientsService.createQueuePatients(request);
    }

    @Test(expected = AppException.class)
    public void testCreateQueuePatients_RoomNotAvailableForSpecialization() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("101");
        request.setRegisteredTime(LocalDateTime.now());

        Specialization specialization = new Specialization();
        specialization.setId("1");

        Patient patient = new Patient();
        patient.setId("1");

        Department department = new Department();
        department.setId("1");
        department.setName("Consultation Room");
        department.setSpecialization(specialization);

        // Mock repository calls for no room available for specialization
        when(specializationRepository.findById("1")).thenReturn(Optional.of(specialization));
        when(patientRepository.findByIdAndDeletedAtIsNull("1")).thenReturn(Optional.of(patient));
        when(departmentRepository.findByTypeAndSpecializationId("CONSULTATION", "1"))
                .thenReturn(Optional.empty());  // No room available for the specialization

        // Call the service method (expecting an exception)
        queuePatientsService.createQueuePatients(request);
    }

    @Test(expected = AppException.class)
    public void testCreateQueuePatients_InvalidDepartmentType() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(null);  // Invalid department type
        request.setSpecializationId("1");
        request.setRoomNumber("101");
        request.setRegisteredTime(LocalDateTime.now());

        // Mock repository call
        when(departmentRepository.findByTypeAndSpecializationId(null, "1")).thenReturn(Optional.empty());

        // Call the service method (expecting an exception)
        queuePatientsService.createQueuePatients(request);
    }

    @Test(expected = AppException.class)
    public void testCreateQueuePatients_PatientNotFound() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("999");  // Invalid patient ID
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("101");
        request.setRegisteredTime(LocalDateTime.now());

        // Mock repository call for patient not found
        when(patientRepository.findByIdAndDeletedAtIsNull("999")).thenReturn(Optional.empty());  // Patient not found

        // Call the service method (expecting an exception)
        queuePatientsService.createQueuePatients(request);
    }

    @Test(expected = AppException.class)
    public void testCreateQueuePatients_ActiveQueueIdNotFound() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("101");
        request.setRegisteredTime(LocalDateTime.now());

        // Mock repository call for no active queue for today
        when(dailyQueueService.getActiveQueueIdForToday()).thenReturn(null);  // No active queue

        // Call the service method (expecting an exception)
        queuePatientsService.createQueuePatients(request);
    }

    @Test(expected = AppException.class)
    public void testCreateQueuePatients_QueueAlreadyFull() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("101");
        request.setRegisteredTime(LocalDateTime.now());

        Specialization specialization = new Specialization();
        specialization.setId("1");

        Patient patient = new Patient();
        patient.setId("1");

        // Mock department data
        Department department = new Department();
        department.setId("1");
        department.setName("Consultation Room");
        department.setSpecialization(specialization);

        // Simulate that the room is full (maximum number of patients reached)
        when(departmentRepository.countAvailableRoomsBySpecialization("1")).thenReturn(0);  // No room available

        // Call the service method (expecting an exception)
        queuePatientsService.createQueuePatients(request);
    }

    @Test(expected = AppException.class)
    public void testCreateQueuePatients_PatientHasNoDepartment() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("101");
        request.setRegisteredTime(LocalDateTime.now());

        // Mock repository call for patient without a department
        Patient patient = new Patient();
        patient.setId("1");

        when(patientRepository.findByIdAndDeletedAtIsNull("1")).thenReturn(Optional.of(patient));

        // Call the service method (expecting an exception)
        queuePatientsService.createQueuePatients(request);
    }

    @Test(expected = AppException.class)
    public void testCreateQueuePatients_StaffShiftMismatch() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("101");
        request.setRegisteredTime(LocalDateTime.now());

        Specialization specialization = new Specialization();
        specialization.setId("1");

        Patient patient = new Patient();
        patient.setId("1");

        Department department = new Department();
        department.setId("1");
        department.setName("Consultation Room");
        department.setSpecialization(specialization);

        Staff staff = new Staff();
        staff.setId("staff1");
        staff.setDepartment(department);

        // Mock repository and service calls
        when(specializationRepository.findById("1")).thenReturn(Optional.of(specialization));
        when(patientRepository.findByIdAndDeletedAtIsNull("1")).thenReturn(Optional.of(patient));
        when(departmentRepository.findByTypeAndRoomNumberAndSpecializationId("CONSULTATION", "101", "1"))
                .thenReturn(Optional.of(department));
        when(staffRepository.findByAccountIdAndDeletedAtIsNull(anyString())).thenReturn(Optional.of(staff));
        when(workScheduleService.isStaffOnShiftNow("staff1")).thenReturn(false);  // Staff is not on shift

        // Call the service method (expecting an exception)
        queuePatientsService.createQueuePatients(request);
    }

    @Test(expected = AppException.class)
    public void testCreateQueuePatients_RoomNumberAlreadyAssignedForSpecialization() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("101");
        request.setRegisteredTime(LocalDateTime.now());

        Specialization specialization = new Specialization();
        specialization.setId("1");

        Patient patient = new Patient();
        patient.setId("1");

        Department department = new Department();
        department.setId("1");
        department.setName("Consultation Room");
        department.setSpecialization(specialization);

        // Mock repository calls for room already assigned to a different specialization
        Department departmentWithDifferentSpecialization = new Department();
        departmentWithDifferentSpecialization.setId("1");
        departmentWithDifferentSpecialization.setSpecialization(new Specialization());  // Different specialization

        when(specializationRepository.findById("1")).thenReturn(Optional.of(specialization));
        when(patientRepository.findByIdAndDeletedAtIsNull("1")).thenReturn(Optional.of(patient));
        when(departmentRepository.findByTypeAndRoomNumberAndSpecializationId("CONSULTATION", "101", "1"))
                .thenReturn(Optional.of(departmentWithDifferentSpecialization));  // Room already assigned

        // Call the service method (expecting an exception)
        queuePatientsService.createQueuePatients(request);
    }

    @Test(expected = AppException.class)
    public void testCreateQueuePatients_MissingRoomNumberInRequest() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber(null);  // Missing room number
        request.setRegisteredTime(LocalDateTime.now());

        // Call the service method (expecting an exception)
        queuePatientsService.createQueuePatients(request);
    }

    @Test(expected = AppException.class)
    public void testCreateQueuePatients_PatientAlreadyAssignedToRoomForTheDay() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("101");
        request.setRegisteredTime(LocalDateTime.now());

        Specialization specialization = new Specialization();
        specialization.setId("1");

        Patient patient = new Patient();
        patient.setId("1");

        Department department = new Department();
        department.setId("1");
        department.setName("Consultation Room");
        department.setSpecialization(specialization);

        // Mock repository calls to indicate the patient is already assigned to the room
        when(specializationRepository.findById("1")).thenReturn(Optional.of(specialization));
        when(patientRepository.findByIdAndDeletedAtIsNull("1")).thenReturn(Optional.of(patient));
        when(departmentRepository.findByTypeAndRoomNumberAndSpecializationId("CONSULTATION", "101", "1"))
                .thenReturn(Optional.of(department));
        when(queuePatientsRepository.countActiveVisits("queue1", "1")).thenReturn(1);  // Patient already assigned

        // Call the service method (expecting an exception)
        queuePatientsService.createQueuePatients(request);
    }

    @Test
    public void testCreateQueuePatients_ValidRoomAssignmentForExistingPatient() {
        // Setup mock data
        QueuePatientsRequest request = new QueuePatientsRequest();
        request.setPatientId("1");
        request.setType(DepartmentType.CONSULTATION);
        request.setSpecializationId("1");
        request.setRoomNumber("101");
        request.setRegisteredTime(LocalDateTime.now());

        Specialization specialization = new Specialization();
        specialization.setId("1");

        Patient patient = new Patient();
        patient.setId("1");

        Department department = new Department();
        department.setId("1");
        department.setName("Consultation Room");
        department.setSpecialization(specialization);

        // Mock repository calls
        when(specializationRepository.findById("1")).thenReturn(Optional.of(specialization));
        when(patientRepository.findByIdAndDeletedAtIsNull("1")).thenReturn(Optional.of(patient));
        when(departmentRepository.findByTypeAndRoomNumberAndSpecializationId("CONSULTATION", "101", "1"))
                .thenReturn(Optional.of(department));

        // Mock save call
        QueuePatients savedQueuePatient = new QueuePatients();
        savedQueuePatient.setQueueId("queue1");
        savedQueuePatient.setPatientId("1");
        savedQueuePatient.setRoomNumber("101");
        savedQueuePatient.setSpecialization(specialization);

        when(queuePatientsRepository.save(any(QueuePatients.class))).thenReturn(savedQueuePatient);

        // Call the service method
        QueuePatientsResponse result = queuePatientsService.createQueuePatients(request);

        // Assert the results
        assertNotNull(result);
        assertEquals("queue1", result.getQueueId());
        assertEquals("101", result.getRoomNumber());
    }

}
