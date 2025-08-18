package vn.edu.fpt.medicaldiagnosis.service;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.TenantRequest;
import vn.edu.fpt.medicaldiagnosis.entity.DbTask;
import vn.edu.fpt.medicaldiagnosis.entity.ServicePackage;
import vn.edu.fpt.medicaldiagnosis.entity.Tenant;
import vn.edu.fpt.medicaldiagnosis.enums.Status;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.repository.DbTaskRepository;
import vn.edu.fpt.medicaldiagnosis.repository.ServicePackageRepository;
import vn.edu.fpt.medicaldiagnosis.service.impl.TenantServiceImpl;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

public class TenantServiceTest {

    @Mock
    private ServicePackageRepository servicePackageRepository;

    @Mock
    private DbTaskRepository dbTaskRepository;

    @Mock
    private DataSource controlDataSource;

    @InjectMocks
    private TenantServiceImpl tenantServiceImpl;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);  // Replaces initMocks, used for newer versions of Mockito
    }

    @Test(expected = AppException.class)
    public void testCreateTenant_ExistingActiveTenant_ThrowsAppException() throws SQLException {
        // Mock DataSource connection
        Connection mockConnection = mock(Connection.class);
        when(controlDataSource.getConnection()).thenReturn(mockConnection);

        // Arrange
        Tenant existingTenant = new Tenant();
        existingTenant.setStatus(Status.ACTIVE.name());

        when(tenantServiceImpl.getTenantByCode(anyString())).thenReturn(existingTenant);

        TenantRequest request = new TenantRequest();
        request.setCode("TENANT001");

        // Act
        tenantServiceImpl.createTenant(request);

        // Assert is handled by expected exception
    }

    @Test
    public void testCreateTenant_Success() {
        // Arrange
        ServicePackage servicePackage = new ServicePackage();
        servicePackage.setId("1");  // Set ID as String
        servicePackage.setPackageName("Basic Package");

        TenantRequest request = new TenantRequest();
        request.setCode("TENANT002");
        request.setName("Test Tenant");
        request.setServicePackageId("1");  // Set ServicePackage ID as String
        request.setEmail("test@tenant.com");
        request.setPhone("123456789");

        when(servicePackageRepository.findByIdAndDeletedAtIsNull(anyString()))
                .thenReturn(Optional.of(servicePackage));

        // Mock methods to avoid actual calls
        doNothing().when(tenantServiceImpl).processPackagePurchase(any(), any());
        doNothing().when(tenantServiceImpl).insertTenantToControlDb(any());
        doNothing().when(tenantServiceImpl).queueCloudflareSubdomain(any());

        // Act
        Tenant createdTenant = tenantServiceImpl.createTenant(request);

        // Assert
        assertNotNull(createdTenant);
        assertEquals("TENANT002", createdTenant.getCode());
        verify(tenantServiceImpl, times(1)).processPackagePurchase(any(), any());
        verify(tenantServiceImpl, times(1)).insertTenantToControlDb(any());
        verify(tenantServiceImpl, times(1)).queueCloudflareSubdomain(any());

        // Verify that DbTask was saved
        DbTask savedTask = dbTaskRepository.findByTenantCode(createdTenant.getCode());
        assertNotNull(savedTask);
        assertEquals(Status.PENDING.name(), savedTask.getStatus());
    }

    @Test(expected = AppException.class)
    public void testCreateTenant_ServicePackageNotFound() {
        // Arrange
        TenantRequest request = new TenantRequest();
        request.setCode("TENANT003");
        request.setServicePackageId("1");  // Set ServicePackage ID as String

        when(servicePackageRepository.findByIdAndDeletedAtIsNull(anyString()))
                .thenReturn(Optional.empty());

        // Act
        tenantServiceImpl.createTenant(request);

        // Assert is handled by expected exception
    }

    @Test
    public void testDeleteTenant_Success() throws SQLException {
        // Arrange
        Tenant tenant = new Tenant();
        tenant.setCode("TENANT004");
        tenant.setStatus(Status.PENDING.name());

        // Mock getTenantByCode method to return the mock tenant
        when(tenantServiceImpl.getTenantByCode("TENANT004")).thenReturn(tenant);

        // Mocking the Connection and PreparedStatement
        Connection mockConnection = mock(Connection.class);  // Mock the Connection
        PreparedStatement mockPreparedStatement = mock(PreparedStatement.class);  // Mock the PreparedStatement

        // When getConnection() is called on the DataSource, return the mock connection
        when(controlDataSource.getConnection()).thenReturn(mockConnection);

        // When prepareStatement is called on the mock connection, return the mock PreparedStatement
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);

        // Mock executeUpdate() to do nothing (we don't want to hit the real DB)
        doNothing().when(mockPreparedStatement).executeUpdate();

        // Act
        tenantServiceImpl.deleteTenant("TENANT004");

        // Assert: Verify that executeUpdate() was called on the mock PreparedStatement
        verify(mockPreparedStatement, times(1)).executeUpdate();  // Ensures the delete SQL was executed
    }

    @Test(expected = AppException.class)
    public void testDeleteTenant_NotFound() {
        // Arrange
        when(tenantServiceImpl.getTenantByCode("TENANT_NOT_EXIST")).thenReturn(null);

        // Act
        tenantServiceImpl.deleteTenant("TENANT_NOT_EXIST");

        // Assert is handled by expected exception
    }
}
