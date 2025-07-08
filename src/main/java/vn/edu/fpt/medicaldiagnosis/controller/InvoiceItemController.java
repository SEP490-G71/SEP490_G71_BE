package vn.edu.fpt.medicaldiagnosis.controller;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.InvoiceItemStatisticResponse;
import vn.edu.fpt.medicaldiagnosis.service.InvoiceItemService;

import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/invoice-item")
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class InvoiceItemController {
    InvoiceItemService invoiceItemService;

    @GetMapping("/invoice-item-statistics")
    public ApiResponse<InvoiceItemStatisticResponse> getInvoiceItemStatistics(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "totalUsage") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        log.info("Controller: get invoice item statistics with filters={}, page={}, size={}, sortBy={}, sortDir={}", filters, page, size, sortBy, sortDir);
        return ApiResponse.<InvoiceItemStatisticResponse>builder()
                .result(invoiceItemService.getInvoiceItemStatistics(filters, page, size, sortBy, sortDir))
                .build();
    }

}
