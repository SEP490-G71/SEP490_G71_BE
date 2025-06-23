package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.medicaldiagnosis.entity.Invoice;
import vn.edu.fpt.medicaldiagnosis.entity.MedicalOrder;

import java.util.List;
import java.util.Optional;

public interface MedicalOrderRepository extends JpaRepository<MedicalOrder, String> {

    Optional<MedicalOrder> findByIdAndDeletedAtIsNull(String id);

    List<MedicalOrder> findAllByInvoiceItemInvoiceId(String invoiceId);

    List<MedicalOrder> findAllByMedicalRecordIdAndDeletedAtIsNull(String invoiceId);
}
