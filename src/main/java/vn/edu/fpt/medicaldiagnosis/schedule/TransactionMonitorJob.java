package vn.edu.fpt.medicaldiagnosis.schedule;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.fpt.medicaldiagnosis.entity.TransactionHistory;
import vn.edu.fpt.medicaldiagnosis.repository.TransactionHistoryRepository;
import vn.edu.fpt.medicaldiagnosis.service.TenantService;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionMonitorJob {

    private final TransactionHistoryRepository transactionHistoryRepository;
    private final TenantService tenantService;

    @Scheduled(fixedDelay = 3000)
    @Transactional
    public void checkExpiredTransactions() {
        LocalDateTime targetTime = LocalDateTime.now();
        List<TransactionHistory> expired = transactionHistoryRepository.findExpiredTransactions(targetTime);
        if (expired.isEmpty()) return;

        for (TransactionHistory transactionHistory : expired) {
            if (transactionHistory.getDeletedAt() != null) continue;

            // Đánh dấu đã hết hạn
            LocalDateTime now = LocalDateTime.now();
            transactionHistory.setDeletedAt(now);
            transactionHistoryRepository.save(transactionHistory);
            log.info("Transaction {} expired. Marked deleted_at = {}", transactionHistory.getId(), now);

            // Tìm gói kế tiếp (start_date > end_date của gói vừa hết hạn)
            transactionHistoryRepository
                    .findNextPackageAfter(transactionHistory.getTenantId(), transactionHistory.getEndDate())
                    .ifPresent(next -> {
                        tenantService.updateTenantServicePackage(transactionHistory.getTenantId(), next.getServicePackageId());
                        log.info("Tenant {} switched to next package {}", transactionHistory.getTenantId(), next.getServicePackageId());
                    });
        }
    }
}
