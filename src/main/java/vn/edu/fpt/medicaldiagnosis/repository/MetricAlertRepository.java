package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.medicaldiagnosis.entity.MetricAlert;

public interface MetricAlertRepository extends JpaRepository<MetricAlert, String> {
    Page<MetricAlert> findAll(Specification<MetricAlert> spec, Pageable pageable);
}
