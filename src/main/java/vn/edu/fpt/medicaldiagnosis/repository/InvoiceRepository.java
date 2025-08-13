package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.medicaldiagnosis.dto.response.ChartDataResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.MonthlyCountResponse;
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

    long countByDeletedAtIsNull();

    int countByStatus(InvoiceStatus paid);

    @Query("""
    SELECT COALESCE(SUM(i.total), 0)
    FROM Invoice i
    WHERE i.status = :status AND i.deletedAt IS NULL
""")
    Long sumTotalAmountByStatus(@Param("status") InvoiceStatus status);

    @Query(value = """
    SELECT DATE_FORMAT(created_at, '%b') AS month,
           SUM(total) AS total
    FROM invoices
    WHERE YEAR(created_at) = YEAR(CURDATE())
      AND deleted_at IS NULL
      AND status = 'PAID'
    GROUP BY DATE_FORMAT(created_at, '%b'), MONTH(created_at)
    ORDER BY MONTH(created_at)
""", nativeQuery = true)
    List<MonthlyCountResponse> getMonthlyRevenueStats();

    @Query(value = """
    SELECT DATE_FORMAT(created_at, '%b') AS month,
           COUNT(*) AS total
    FROM invoices
    WHERE YEAR(created_at) = YEAR(CURDATE())
      AND deleted_at IS NULL
    GROUP BY DATE_FORMAT(created_at, '%b'), MONTH(created_at)
    ORDER BY MONTH(created_at)
""", nativeQuery = true)
    List<MonthlyCountResponse> getMonthlyInvoiceStats();

    @Query(value = """
      SELECT DATE(i.confirmed_at) AS d, COALESCE(SUM(i.total), 0) AS revenue
      FROM invoices i
      WHERE i.status = 'PAID'
        AND i.confirmed_at >= :start
        AND i.confirmed_at <  :end
      GROUP BY DATE(i.confirmed_at)
      ORDER BY d
    """, nativeQuery = true)
    List<Object[]> sumDailyBetween(@Param("start") LocalDateTime start,
                                   @Param("end") LocalDateTime end);
}
