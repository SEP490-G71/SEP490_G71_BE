package vn.edu.fpt.medicaldiagnosis.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import vn.edu.fpt.medicaldiagnosis.dto.request.TransactionHistoryRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.StatisticResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.TransactionHistoryResponse;
import vn.edu.fpt.medicaldiagnosis.entity.TransactionHistory;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.mapper.TransactionHistoryMapper;
import vn.edu.fpt.medicaldiagnosis.repository.TransactionHistoryRepository;
import vn.edu.fpt.medicaldiagnosis.service.TransactionHistoryService;
import vn.edu.fpt.medicaldiagnosis.specification.TransactionHistorySpecification;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionHistoryServiceImpl implements TransactionHistoryService {

    private final TransactionHistoryRepository repository;
    private final TransactionHistoryMapper mapper;

    @Override
    public TransactionHistoryResponse create(TransactionHistoryRequest request) {
        log.info("Creating transaction: {}", request);
        TransactionHistory entity = mapper.toEntity(request);
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    public TransactionHistoryResponse update(String id, TransactionHistoryRequest request) {
        TransactionHistory entity = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));
        mapper.updateEntity(entity, request);
        return mapper.toResponse(repository.save(entity));
    }

    @Override
    public void delete(String id) {
        TransactionHistory entity = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));
        entity.setDeletedAt(LocalDateTime.now());
        repository.save(entity);
    }

    @Override
    public TransactionHistoryResponse getById(String id) {
        TransactionHistory entity = repository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));
        return mapper.toResponse(entity);
    }

    public Page<TransactionHistoryResponse> getPagedTransactions(Map<String, String> filters, int page, int size, String sortBy, String sortDir) {
        int offset = page * size;

        String packageName = normalize(filters.getOrDefault("packageName", null));
        String tenantCode = normalize(filters.getOrDefault("tenantCode", null));
        String billingType = normalize(filters.getOrDefault("billingType", null));
        Timestamp startDate = parseDate(filters.get("startDate"));
        Timestamp endDate = parseDate(filters.get("endDate"));

        List<Object[]> rows = repository.findTransactionHistoryWithPackage(
                packageName, tenantCode, billingType, startDate, endDate, size, offset
        );
        long total = repository.countTransactionHistoryWithPackage(
                packageName, tenantCode, billingType, startDate, endDate
        );

        List<TransactionHistoryResponse> content = rows.stream().map(this::mapRow).toList();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.fromString(sortDir), sortBy));
        return new PageImpl<>(content, pageable, total);
    }

    private TransactionHistoryResponse mapRow(Object[] row) {
        return TransactionHistoryResponse.builder()
                .id((String) row[0])
                .tenantId((String) row[1])
                .tenantCode((String) row[2])
                .servicePackageId((String) row[3])
                .packageName((String) row[4])
                .billingType((String) row[5])
                .quantity(row[6] != null ? ((Number) row[6]).intValue() : null)
                .price(row[7] != null ? ((Number) row[7]).intValue() : null)
                .startDate(row[8] != null ? ((Timestamp) row[8]).toLocalDateTime() : null)
                .endDate(row[9] != null ? ((Timestamp) row[9]).toLocalDateTime() : null)
                .createdAt(row[10] != null ? ((Timestamp) row[10]).toLocalDateTime() : null)
                .build();
    }

    private String normalize(String input) {
        return (input == null || input.isBlank()) ? null : input.toLowerCase();
    }

    private Timestamp parseDate(String s) {
        try {
            return (s == null || s.isBlank()) ? null : Timestamp.valueOf(s + " 00:00:00");
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public TransactionHistoryResponse findLatestActivePackage(String tenantId) {
        return repository.findLatestActivePackage(tenantId)
                .map(mapper::toResponse)
                .orElseThrow(() -> new AppException(ErrorCode.TRANSACTION_NOT_FOUND));
    }

    @Override
    public StatisticResponse getBusinessStatistics() {
        List<TransactionHistory> transactions = repository.findAll();

        long paidPackageCount = transactions.stream()
                .filter(tx -> tx.getPrice() != null && tx.getPrice() > 0)
                .count();

        double totalRevenue = transactions.stream()
                .filter(tx -> tx.getPrice() != null)
                .mapToDouble(TransactionHistory::getPrice)
                .sum();

        long tenantCount = transactions.stream()
                .map(TransactionHistory::getTenantId)
                .distinct()
                .count();

        return StatisticResponse.builder()
                .paidPackageCount(paidPackageCount)
                .totalRevenue(totalRevenue)
                .tenantCount(tenantCount)
                .build();
    }

}
