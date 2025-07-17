package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.medicaldiagnosis.entity.TemplateFile;
import vn.edu.fpt.medicaldiagnosis.enums.TemplateFileType;

import java.util.Optional;

public interface TemplateFileRepository extends JpaRepository<TemplateFile, String> {
    Page<TemplateFile> findAll(Specification<TemplateFile> spec, Pageable pageable);

    Optional<TemplateFile> findByIdAndDeletedAtIsNull(String id);

    long countByTypeAndIsDefaultTrueAndDeletedAtIsNull(TemplateFileType type);

    boolean existsByTypeAndIsDefaultTrueAndIdNotAndDeletedAtIsNull(TemplateFileType type, String id);

    long countByTypeAndDeletedAtIsNull(TemplateFileType type);

    boolean existsByTypeAndIsDefaultTrueAndDeletedAtIsNull(TemplateFileType type);

    Optional<TemplateFile> findByTypeAndIsDefaultTrueAndDeletedAtIsNull(TemplateFileType type);
}
