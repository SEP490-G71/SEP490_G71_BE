package vn.edu.fpt.medicaldiagnosis.dto.response;

import java.time.LocalDate;

public interface BirthdayProjection {
    String getFullName();
    String getPatientCode();
    String getEmail();
    String getPhone();
    LocalDate getDob();
}