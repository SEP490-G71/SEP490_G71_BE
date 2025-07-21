package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.medicaldiagnosis.entity.Invoice;
import vn.edu.fpt.medicaldiagnosis.entity.MedicalOrder;
import vn.edu.fpt.medicaldiagnosis.enums.MedicalOrderStatus;

import java.util.List;
import java.util.Optional;

public interface MedicalOrderRepository extends JpaRepository<MedicalOrder, String> {

    Optional<MedicalOrder> findByIdAndDeletedAtIsNull(String id);

    List<MedicalOrder> findAllByInvoiceItemInvoiceId(String invoiceId);
    List<MedicalOrder> findAllByInvoiceItemIdIn(List<String> invoiceItemIds);

    List<MedicalOrder> findAllByMedicalRecordIdAndDeletedAtIsNull(String invoiceId);

    List<MedicalOrder> findAllByService_Department_IdAndStatusNotAndDeletedAtIsNull(String departmentId, MedicalOrderStatus medicalOrderStatus);
}
