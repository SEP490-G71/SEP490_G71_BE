package vn.edu.fpt.medicaldiagnosis.service;

import vn.edu.fpt.medicaldiagnosis.dto.request.TransactionHistoryRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.TransactionHistoryResponse;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface TransactionHistoryService {

    TransactionHistoryResponse create(TransactionHistoryRequest request);

    TransactionHistoryResponse update(String id, TransactionHistoryRequest request);

    void delete(String id);

    TransactionHistoryResponse getById(String id);

    Page<TransactionHistoryResponse> getPagedTransactions(Map<String, String> filters, int page, int size, String sortBy, String sortDir);

    TransactionHistoryResponse findLatestActivePackage(String tenantId);
}
