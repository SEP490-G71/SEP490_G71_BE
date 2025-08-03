package vn.edu.fpt.medicaldiagnosis.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.edu.fpt.medicaldiagnosis.entity.Account;
import vn.edu.fpt.medicaldiagnosis.entity.Tenant;
import vn.edu.fpt.medicaldiagnosis.enums.Status;
import vn.edu.fpt.medicaldiagnosis.service.TenantService;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class AccountSyncJob {

    private final TenantService tenantService;

    private final DataSource controlDataSource;

    @Scheduled(fixedDelay = 3000)
    public void syncTenantAccountsToControl() {
        List<Tenant> tenants = tenantService.getAllTenantsActive();

        for (Tenant tenant : tenants) {
            try (Connection tenantConn = getTenantConnection(tenant);
                 PreparedStatement stmt = tenantConn.prepareStatement(
                         "SELECT * FROM accounts WHERE is_tenant = TRUE AND deleted_at IS NULL");
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    String id = rs.getString("id");
                    String username = rs.getString("username");
                    String password = rs.getString("password");

                    if (!accountExistsInControl(id)) {
                        insertToControlDb(id, username, password);
                    }
                }

            } catch (Exception e) {
                log.error("Error syncing accounts for tenant {}: {}", tenant.getCode(), e.getMessage());
            }
        }
    }

    private boolean accountExistsInControl(String accountId) {
        String sql = "SELECT COUNT(*) FROM accounts WHERE id = ?";
        try (Connection conn = controlDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, accountId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (Exception e) {
            log.warn("Error checking account existence in control DB: {}", e.getMessage());
        }
        return false;
    }

    private void insertToControlDb(String id, String username, String password) {
        String insertAccountSql = "INSERT IGNORE INTO accounts (id, username, password, is_tenant) VALUES (?, ?, ?, TRUE)";
        String insertRoleSql = "INSERT IGNORE INTO account_roles (account_id, role_name) VALUES (?, ?)";

            try (Connection conn = controlDataSource.getConnection()) {
                conn.setAutoCommit(false); // đảm bảo atomic insert

                try (
                        PreparedStatement insertAccountStmt = conn.prepareStatement(insertAccountSql);
                        PreparedStatement insertRoleStmt = conn.prepareStatement(insertRoleSql)
                ) {
                    // Insert account
                    insertAccountStmt.setString(1, id);
                    insertAccountStmt.setString(2, username);
                    insertAccountStmt.setString(3, password);
                    insertAccountStmt.executeUpdate();

                    // Insert role
                    insertRoleStmt.setString(1, id);
                    insertRoleStmt.setString(2, "TENANT");
                insertRoleStmt.executeUpdate();

                conn.commit();
                } catch (SQLException e) {
                conn.rollback();
                log.error("Failed to insert account {} or role to control DB: {}", username, e.getMessage());
            }

        } catch (Exception e) {
            log.error("Error opening connection to control DB: {}", e.getMessage());
        }
    }

    private Connection getTenantConnection(Tenant tenant) throws SQLException {
        String url = "jdbc:mysql://" + tenant.getDbHost() + ":" + tenant.getDbPort() + "/" + tenant.getDbName();
        return DriverManager.getConnection(url, tenant.getDbUsername(), tenant.getDbPassword());
    }
}
