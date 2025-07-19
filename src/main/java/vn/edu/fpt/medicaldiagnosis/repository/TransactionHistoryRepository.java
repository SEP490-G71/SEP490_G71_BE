package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.medicaldiagnosis.entity.TransactionHistory;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TransactionHistoryRepository extends JpaRepository<TransactionHistory, String> {
    Optional<TransactionHistory> findByIdAndDeletedAtIsNull(String id);

    Page<TransactionHistory> findAll(Specification<TransactionHistory> spec, Pageable pageable);

    @Query(value = """
        SELECT * FROM transaction_history
        WHERE tenant_id = :tenantId
          AND deleted_at IS NULL
        ORDER BY end_date DESC
        LIMIT 1
    """, nativeQuery = true)
    Optional<TransactionHistory> findLatestActivePackage(@Param("tenantId") String tenantId);

}
