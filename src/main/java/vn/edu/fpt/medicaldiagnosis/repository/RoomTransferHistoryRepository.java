package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.medicaldiagnosis.entity.RoomTransferHistory;

import java.util.List;
import java.util.Optional;

public interface RoomTransferHistoryRepository extends JpaRepository<RoomTransferHistory, String> {
    List<RoomTransferHistory> findByMedicalRecord_IdAndToDepartment_IdOrderByTransferTimeDesc(String id, String id1);

    Optional<RoomTransferHistory> findFinalByMedicalRecordId(String id);

    Optional<RoomTransferHistory> findTopByMedicalRecordIdOrderByTransferTimeDesc(String medicalRecordId);

    Optional<RoomTransferHistory> findTopByMedicalRecordIdAndToDepartmentIdOrderByTransferTimeDesc(String id, String departmentId);

    List<RoomTransferHistory> findAllByMedicalRecordIdAndDeletedAtIsNullOrderByTransferTimeAsc(String recordId);

    Page<RoomTransferHistory> findAll(Specification<RoomTransferHistory> spec, Pageable pageable);
}
