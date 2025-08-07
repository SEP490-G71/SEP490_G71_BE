package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.medicaldiagnosis.entity.RoomTransferHistory;

public interface RoomTransferHistoryRepository extends JpaRepository<RoomTransferHistory, String> {
}
