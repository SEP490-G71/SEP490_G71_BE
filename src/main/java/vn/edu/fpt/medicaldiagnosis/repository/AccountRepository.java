package vn.edu.fpt.medicaldiagnosis.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.edu.fpt.medicaldiagnosis.entity.Account;

@Repository
public interface AccountRepository extends JpaRepository<Account, String> {
    boolean existsByUsername(String username);

    Optional<Account> findByUsername(String username);

    @Query("SELECT a.username FROM Account a WHERE a.username LIKE :prefix%")
    List<String> findUsernamesByPrefix(@Param("prefix") String prefix);

    Page<Account> findAll(Specification<Account> spec, Pageable pageable);
}
