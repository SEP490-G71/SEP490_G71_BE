package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.medicaldiagnosis.entity.InvoiceItem;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, String> {
}
