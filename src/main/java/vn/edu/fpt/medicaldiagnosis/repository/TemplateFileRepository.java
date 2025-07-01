package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.medicaldiagnosis.entity.TemplateFile;

public interface TemplateFileRepository extends JpaRepository<TemplateFile, String> {
    Page<TemplateFile> findAll(Specification<TemplateFile> spec, Pageable pageable);
}
