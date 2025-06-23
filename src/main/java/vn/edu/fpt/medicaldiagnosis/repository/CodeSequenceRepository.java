package vn.edu.fpt.medicaldiagnosis.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import vn.edu.fpt.medicaldiagnosis.entity.CodeSequence;

import java.util.Optional;

@Repository
public interface CodeSequenceRepository extends JpaRepository<CodeSequence, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CodeSequence c WHERE c.codeType = :codeType")
    Optional<CodeSequence> findByCodeTypeForUpdate(@Param("codeType") String codeType);
}
