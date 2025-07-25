package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.medicaldiagnosis.entity.TransactionHistory;

import java.sql.Timestamp;
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

    @Query(value = """
        SELECT th.id, th.tenant_id, t.code AS tenant_code, th.service_package_id,
               sp.package_name, sp.billing_type, sp.quantity, sp.price,
               th.start_date, th.end_date, th.created_at
        FROM transaction_history th
        LEFT JOIN service_packages sp ON th.service_package_id = sp.id
        LEFT JOIN tenants t ON th.tenant_id = t.id
        WHERE (:packageName IS NULL OR LOWER(sp.package_name) LIKE %:packageName%)
          AND (:tenantCode IS NULL OR LOWER(t.code) LIKE %:tenantCode%)
          AND (:billingType IS NULL OR LOWER(sp.billing_type) = LOWER(:billingType))
          AND (:startDate IS NULL OR th.start_date >= :startDate)
          AND (:endDate IS NULL OR th.end_date <= :endDate)
        ORDER BY th.created_at DESC
        LIMIT :limit OFFSET :offset
    """, nativeQuery = true)
    List<Object[]> findTransactionHistoryWithPackage(
            @Param("packageName") String packageName,
            @Param("tenantCode") String tenantCode,
            @Param("billingType") String billingType,
            @Param("startDate") Timestamp startDate,
            @Param("endDate") Timestamp endDate,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

    @Query(value = """
        SELECT COUNT(*)
        FROM transaction_history th
        LEFT JOIN service_packages sp ON th.service_package_id = sp.id
        LEFT JOIN tenants t ON th.tenant_id = t.id
        WHERE (:packageName IS NULL OR LOWER(sp.package_name) LIKE %:packageName%)
          AND (:tenantCode IS NULL OR LOWER(t.code) LIKE %:tenantCode%)
          AND (:billingType IS NULL OR LOWER(sp.billing_type) = LOWER(:billingType))
          AND (:startDate IS NULL OR th.start_date >= :startDate)
          AND (:endDate IS NULL OR th.end_date <= :endDate)
    """, nativeQuery = true)
    long countTransactionHistoryWithPackage(
            @Param("packageName") String packageName,
            @Param("tenantCode") String tenantCode,
            @Param("billingType") String billingType,
            @Param("startDate") Timestamp startDate,
            @Param("endDate") Timestamp endDate
    );

}
