package vn.edu.fpt.medicaldiagnosis.service;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import vn.edu.fpt.medicaldiagnosis.config.DataSourceProvider;
import vn.edu.fpt.medicaldiagnosis.config.TenantSchemaInitializer;
import vn.edu.fpt.medicaldiagnosis.dto.request.TenantRequest;
import vn.edu.fpt.medicaldiagnosis.entity.DbTask;
import vn.edu.fpt.medicaldiagnosis.entity.ServicePackage;
import vn.edu.fpt.medicaldiagnosis.entity.Tenant;
import vn.edu.fpt.medicaldiagnosis.enums.Status;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.repository.*;
import vn.edu.fpt.medicaldiagnosis.service.impl.TenantServiceImpl;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class TenantServiceTest {

    @Mock private DataSource controlDataSource;
    @Mock private TenantSchemaInitializer schemaInitializer;
    @Mock private DataSourceProvider dataSourceProvider;
    @Mock private DbTaskRepository dbTaskRepository;
    @Mock private EmailTaskRepository emailTaskRepository;
    @Mock private CloudflareTaskRepository cloudflareTaskRepository;
    @Mock private ServicePackageRepository servicePackageRepository;
    @Mock private TransactionHistoryRepository transactionHistoryRepository;
    @Mock private TransactionHistoryService transactionHistoryService;
    private TenantServiceImpl tenantServiceImpl; // Spy

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        TenantServiceImpl realService = new TenantServiceImpl(
                controlDataSource,
                schemaInitializer,
                dataSourceProvider,
                dbTaskRepository,
                emailTaskRepository,
                cloudflareTaskRepository,
                servicePackageRepository,
                transactionHistoryRepository,
                transactionHistoryService
        );

        tenantServiceImpl = spy(realService);
    }

    // ---------------- CREATE TENANT ----------------

    @Test
    public void testCreateTenant_Success() {
        doReturn(null).when(tenantServiceImpl).getTenantByCode(anyString());

        ServicePackage servicePackage = new ServicePackage();
        servicePackage.setId("1");
        servicePackage.setPackageName("Basic Package");

        TenantRequest request = new TenantRequest();
        request.setCode("TENANT002");
        request.setName("Test Tenant");
        request.setServicePackageId("1");
        request.setEmail("test@tenant.com");
        request.setPhone("123456789");

        when(servicePackageRepository.findByIdAndDeletedAtIsNull("1"))
                .thenReturn(Optional.of(servicePackage));

        doNothing().when(tenantServiceImpl).insertTenantToControlDb(any());
        doNothing().when(tenantServiceImpl).queueCloudflareSubdomain(any());
        doNothing().when(tenantServiceImpl).processPackagePurchase(any(), any());

        DbTask task = new DbTask();
        task.setTenantCode("TENANT002");
        task.setStatus(Status.PENDING);
        when(dbTaskRepository.save(any(DbTask.class))).thenReturn(task);

        Tenant createdTenant = tenantServiceImpl.createTenant(request);

        assertNotNull(createdTenant);
        assertEquals("TENANT002", createdTenant.getCode());
    }

    @Test(expected = AppException.class)
    public void testCreateTenant_ExistingActiveTenant_ThrowsAppException() {
        Tenant existingTenant = new Tenant();
        existingTenant.setStatus(Status.ACTIVE.name());

        doReturn(existingTenant).when(tenantServiceImpl).getTenantByCode(anyString());

        TenantRequest request = new TenantRequest();
        request.setCode("TENANT001");

        tenantServiceImpl.createTenant(request);
    }

    @Test(expected = AppException.class)
    public void testCreateTenant_ServicePackageNotFound() {
        doReturn(null).when(tenantServiceImpl).getTenantByCode(anyString());

        TenantRequest request = new TenantRequest();
        request.setCode("TENANT003");
        request.setServicePackageId("1");

        when(servicePackageRepository.findByIdAndDeletedAtIsNull("1"))
                .thenReturn(Optional.empty());

        tenantServiceImpl.createTenant(request);
    }

    // ---------------- DELETE TENANT ----------------

    @Test
    public void testDeleteTenant_Success() throws Exception {
        Tenant tenant = Tenant.builder()
                .id(UUID.randomUUID().toString())
                .code("TENANT004")
                .status(Status.PENDING.name())
                .build();

        doReturn(tenant).when(tenantServiceImpl).getTenantByCode("TENANT004");

        Connection mockConn = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);
        when(controlDataSource.getConnection()).thenReturn(mockConn);
        when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);

        tenantServiceImpl.deleteTenant("TENANT004");

        verify(mockStmt, times(1)).executeUpdate();
    }

    @Test(expected = AppException.class)
    public void testDeleteTenant_NotFound() {
        doReturn(null).when(tenantServiceImpl).getTenantByCode("TENANT_NOT_EXIST");

        tenantServiceImpl.deleteTenant("TENANT_NOT_EXIST");
    }

    @Test(expected = RuntimeException.class)
    public void testDeleteTenant_SQLException() throws Exception {
        Tenant tenant = Tenant.builder()
                .id(UUID.randomUUID().toString())
                .code("TENANT005")
                .status(Status.PENDING.name())
                .build();

        doReturn(tenant).when(tenantServiceImpl).getTenantByCode("TENANT005");

        when(controlDataSource.getConnection()).thenThrow(new SQLException("DB error"));

        tenantServiceImpl.deleteTenant("TENANT005");
    }
}
