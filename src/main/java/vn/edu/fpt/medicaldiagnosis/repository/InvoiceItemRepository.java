package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import vn.edu.fpt.medicaldiagnosis.dto.response.TopServiceResponse;
import vn.edu.fpt.medicaldiagnosis.entity.InvoiceItem;

import java.util.List;

public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, String> {
    List<InvoiceItem> findAllByInvoiceId(String invoiceId);

    List<InvoiceItem> findAll(Specification<InvoiceItem> spec);

    @Query("""
    SELECT new vn.edu.fpt.medicaldiagnosis.dto.response.TopServiceResponse(
        i.service.serviceCode,
        i.service.name,
        COUNT(i),
        SUM(i.total)
    )
    FROM InvoiceItem i
    WHERE i.invoice.status = 'PAID' AND i.invoice.deletedAt IS NULL
    GROUP BY i.service.serviceCode, i.service.name
    ORDER BY SUM(i.total) DESC
""")
    List<TopServiceResponse> findTopServicesByRevenue(Pageable pageable);


}
