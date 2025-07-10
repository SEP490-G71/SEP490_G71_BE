package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.medicaldiagnosis.entity.ServicePackage;

import java.util.List;
import java.util.Optional;

public interface ServicePackageRepository extends JpaRepository<ServicePackage, String>, JpaSpecificationExecutor<ServicePackage> {

    @Query(value = "SELECT * FROM service_packages WHERE tenant_id = :tenantId AND deleted_at IS NULL", nativeQuery = true)
    List<ServicePackage> findByTenantId(@Param("tenantId") String tenantId);

    @Query(value = "SELECT * FROM service_packages WHERE tenant_id = :tenantId AND status = :status AND deleted_at IS NULL", nativeQuery = true)
    List<ServicePackage> findByTenantIdAndStatus(@Param("tenantId") String tenantId,
                                                 @Param("status") String status);

    Optional<ServicePackage> findByIdAndDeletedAtIsNull(String id);

    boolean existsByIdAndDeletedAtIsNull(String id);

    @Query(value = """
    SELECT EXISTS (
        SELECT 1 FROM service_package
        WHERE tenant_id = :tenantId
          AND LOWER(package_name) = LOWER(:name)
          AND deleted_at IS NULL
    )
    """, nativeQuery = true)
    boolean existsByTenantAndName(@Param("tenantId") String tenantId,
                                  @Param("name") String name);

    @Query(value = """
    SELECT EXISTS (
        SELECT 1 FROM service_package
        WHERE tenant_id = :tenantId
          AND LOWER(package_name) = LOWER(:name)
          AND id <> :id
          AND deleted_at IS NULL
    )
    """, nativeQuery = true)
    boolean existsByTenantAndNameNotId(@Param("tenantId") String tenantId,
                                       @Param("name") String name,
                                       @Param("id") String id);

}
