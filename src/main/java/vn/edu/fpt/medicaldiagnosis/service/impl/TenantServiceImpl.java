package vn.edu.fpt.medicaldiagnosis.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.entity.Tenant;
import vn.edu.fpt.medicaldiagnosis.service.TenantService;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class TenantServiceImpl implements TenantService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public TenantServiceImpl(@Qualifier("controlDataSource") DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    @Override
    public List<Tenant> getAllTenants() {
        return jdbcTemplate.query(
                "SELECT id, db_url, db_username, db_password FROM tenants",
                (rs, rowNum) -> new Tenant(
                        rs.getString("id"),
                        rs.getString("db_url"),
                        rs.getString("db_username"),
                        rs.getString("db_password")
                )
        );
    }

    @Override
    public Tenant getTenantById(String id) {
        return jdbcTemplate.queryForObject(
                "SELECT id, db_url, db_username, db_password FROM tenants WHERE id = ?",
                new Object[]{id},
                (rs, rowNum) -> new Tenant(
                        rs.getString("id"),
                        rs.getString("db_url"),
                        rs.getString("db_username"),
                        rs.getString("db_password")
                )
        );
    }
}
