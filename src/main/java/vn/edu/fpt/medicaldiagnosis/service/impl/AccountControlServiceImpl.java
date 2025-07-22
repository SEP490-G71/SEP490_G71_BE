package vn.edu.fpt.medicaldiagnosis.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.dto.response.AccountResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Account;
import vn.edu.fpt.medicaldiagnosis.mapper.AccountMapper;
import vn.edu.fpt.medicaldiagnosis.service.AccountControlService;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountControlServiceImpl implements AccountControlService {

    @Qualifier("controlDataSource")
    private final DataSource controlDataSource;

    private final AccountMapper accountMapper;

    @Override
    public List<AccountResponse> getAllAccounts() {
        List<AccountResponse> responses = new ArrayList<>();
        String sql = "SELECT * FROM accounts WHERE deleted_at IS NULL";

        try (Connection conn = controlDataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Account account = Account.builder()
                        .id(rs.getString("id"))
                        .username(rs.getString("username"))
                        .password(rs.getString("password"))
                        .isTenant(rs.getBoolean("is_tenant"))
                        .build();

                responses.add(accountMapper.toAccountResponse(account));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Error loading accounts from control_db", e);
        }

        return responses;
    }

}
