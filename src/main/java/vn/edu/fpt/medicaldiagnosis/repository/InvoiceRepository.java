package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import vn.edu.fpt.medicaldiagnosis.entity.Invoice;
import vn.edu.fpt.medicaldiagnosis.entity.Staff;
import vn.edu.fpt.medicaldiagnosis.enums.InvoiceStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;


public interface InvoiceRepository extends JpaRepository<Invoice, String>, JpaSpecificationExecutor<Invoice> {
    Optional<Invoice> findByIdAndDeletedAtIsNull(String id);

    Page<Invoice> findAll(Specification<Invoice> spec, Pageable pageable);

    List<Invoice> findAllByCreatedAtBetweenAndStatusAndDeletedAtIsNull(LocalDateTime start, LocalDateTime end, InvoiceStatus invoiceStatus);
}
