package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.medicaldiagnosis.entity.Setting;

import java.util.Optional;

public interface SettingRepository extends JpaRepository<Setting, String> {
    Optional<Object> findFirstByOrderByCreatedAtAsc();
}
