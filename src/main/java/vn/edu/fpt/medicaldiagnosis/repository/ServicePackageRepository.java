package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import vn.edu.fpt.medicaldiagnosis.entity.ServicePackage;

import java.util.List;
import java.util.Optional;

public interface ServicePackageRepository extends JpaRepository<ServicePackage, String>, JpaSpecificationExecutor<ServicePackage> {
    Optional<ServicePackage> findByIdAndDeletedAtIsNull(String id);

    @Query(value = """
    SELECT EXISTS (
        SELECT 1 FROM service_packages 
        WHERE LOWER(package_name) = LOWER(:packageName)
        AND deleted_at IS NULL
    )
    """, nativeQuery = true)
    Long packageNameExists(@Param("packageName") String packageName);

    @Query(value = """
    SELECT * FROM service_packages 
        WHERE LOWER(package_name) = LOWER(:packageName)
        AND status = 'ACTIVE'
        AND start_date <= NOW()
        AND (end_date IS NULL OR end_date >= NOW())
        AND deleted_at IS NULL
        LIMIT 1
    """, nativeQuery = true)
    Optional<ServicePackage> findPackageByName(@Param("packageName") String packageName);
}
