package vn.edu.fpt.medicaldiagnosis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.edu.fpt.medicaldiagnosis.entity.Account;
import vn.edu.fpt.medicaldiagnosis.entity.ChatHistory;

import java.util.List;

public interface ChatHistoryRepository extends JpaRepository<ChatHistory, String> {

    List<ChatHistory> findByAccountOrderByCreatedAtDesc(Account account);
}
