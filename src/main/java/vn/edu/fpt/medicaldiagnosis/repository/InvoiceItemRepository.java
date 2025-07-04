package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.medicaldiagnosis.entity.InvoiceItem;

import java.util.List;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, String> {
    List<InvoiceItem> findAllByInvoiceId(String invoiceId);

}
