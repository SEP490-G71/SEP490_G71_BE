package vn.edu.fpt.medicaldiagnosis.controller;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.InvoiceItemStatisticResponse;
import vn.edu.fpt.medicaldiagnosis.service.InvoiceItemService;
import vn.edu.fpt.medicaldiagnosis.service.impl.ExportServiceImpl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/invoice-items")
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class InvoiceItemController {
    InvoiceItemService invoiceItemService;
    ExportServiceImpl invoiceExportService;
    @GetMapping("/statistics")
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

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportInvoiceItemStatisticsExcel(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "1000") int size, // xuất nhiều bản ghi hơn mặc định
            @RequestParam(defaultValue = "totalUsage") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) throws IOException {
        InvoiceItemStatisticResponse data = invoiceItemService.getInvoiceItemStatistics(filters, page, size, sortBy, sortDir);
        ByteArrayInputStream in = invoiceExportService.exportInvoiceItemToExcel(data.getDetails().getContent(), data);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=invoice_items.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(in.readAllBytes());
    }


}
