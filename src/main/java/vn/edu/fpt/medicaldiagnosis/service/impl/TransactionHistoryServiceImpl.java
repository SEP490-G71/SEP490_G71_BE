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

    @Override
    public Page<TransactionHistoryResponse> getPagedTransactions(Map<String, String> filters, int page, int size, String sortBy, String sortDir) {
        String sortColumn = (sortBy == null || sortBy.isBlank()) ? "createdAt" : sortBy;
        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortColumn).ascending() : Sort.by(sortColumn).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<TransactionHistory> spec = TransactionHistorySpecification.buildSpecification(filters);
        Page<TransactionHistory> pageResult = repository.findAll(spec, pageable);

        return pageResult.map(mapper::toResponse);
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
