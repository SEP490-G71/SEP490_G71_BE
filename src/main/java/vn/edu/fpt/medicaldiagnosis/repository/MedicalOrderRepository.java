package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.medicaldiagnosis.entity.MedicalOrder;

import java.util.List;

public interface MedicalOrderRepository extends JpaRepository<MedicalOrder, String> {
    List<MedicalOrder> findAllByInvoiceItemInvoiceId(String invoiceId);
}
