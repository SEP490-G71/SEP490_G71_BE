package vn.edu.fpt.medicaldiagnosis.service;

import org.springframework.jdbc.core.JdbcTemplate;

public interface JdbcTemplateFactory {
    JdbcTemplate create(String tenantCode);
}
