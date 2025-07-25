package vn.edu.fpt.medicaldiagnosis.controller;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.PagedResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.TransactionHistoryResponse;
import vn.edu.fpt.medicaldiagnosis.service.TransactionHistoryService;

import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/transaction-history")
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class TransactionHistoryController {
    TransactionHistoryService service;

    @GetMapping("")
    public ApiResponse<PagedResponse<TransactionHistoryResponse>> getTransactionHistoryPaged(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) {
        log.info("Get paged transaction history: filters={}, page={}, size={}", filters, page, size);
        Page<TransactionHistoryResponse> result = service.getPagedTransactions(filters, page, size, sortBy, sortDir);

        return ApiResponse.<PagedResponse<TransactionHistoryResponse>>builder()
                .result(new PagedResponse<>(
                        result.getContent(),
                        result.getNumber(),
                        result.getSize(),
                        result.getTotalElements(),
                        result.getTotalPages(),
                        result.isLast()
                ))
                .build();
    }

}
