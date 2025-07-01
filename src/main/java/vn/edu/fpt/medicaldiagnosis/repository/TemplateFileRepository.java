package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.medicaldiagnosis.entity.TemplateFile;

public interface TemplateFileRepository extends JpaRepository<TemplateFile, String> {
}
