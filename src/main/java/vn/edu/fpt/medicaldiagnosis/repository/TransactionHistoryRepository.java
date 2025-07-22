package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.medicaldiagnosis.entity.TransactionHistory;

import java.time.LocalDateTime;
import java.util.List;
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

    @Query(value = """
        SELECT * FROM transaction_history
        WHERE deleted_at IS NULL
          AND end_date < :targetTime
    """, nativeQuery = true)
    List<TransactionHistory> findExpiredTransactions(@Param("targetTime") LocalDateTime targetTime);

    @Query(value = """
        SELECT * FROM transaction_history
        WHERE tenant_id = :tenantId
          AND deleted_at IS NULL
          AND start_date > :lastEndDate
        ORDER BY start_date ASC
        LIMIT 1
    """, nativeQuery = true)
    Optional<TransactionHistory> findNextPackageAfter(
            @Param("tenantId") String tenantId,
            @Param("lastEndDate") LocalDateTime lastEndDate
    );

    @Query(value = """
        SELECT * FROM transaction_history
    """, nativeQuery = true)
    List<TransactionHistory> findAll();
}
