package vn.edu.fpt.medicaldiagnosis.service;

import vn.edu.fpt.medicaldiagnosis.entity.DbTask;
import vn.edu.fpt.medicaldiagnosis.enums.Status;

import java.util.List;

public interface DbTaskService {
    void createDatabase(String tenantCode) throws Exception;
    void dropDatabase(String tenantCode) throws Exception;
    List<DbTask> findByStatus(Status status);
}

