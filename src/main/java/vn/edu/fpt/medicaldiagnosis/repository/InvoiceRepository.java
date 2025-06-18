package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.medicaldiagnosis.entity.Invoice;

import java.util.Optional;


public interface InvoiceRepository extends JpaRepository<Invoice, String> {
    Optional<Invoice> findByIdAndDeletedAtIsNull(String id);
}
