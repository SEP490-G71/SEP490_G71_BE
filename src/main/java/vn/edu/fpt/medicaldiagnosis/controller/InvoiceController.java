package vn.edu.fpt.medicaldiagnosis.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.PayInvoiceRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.UpdateInvoiceRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.*;
import vn.edu.fpt.medicaldiagnosis.service.InvoiceService;
import vn.edu.fpt.medicaldiagnosis.service.impl.ExportServiceImpl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/invoices")
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class InvoiceController {
    InvoiceService invoiceService;
    ExportServiceImpl invoiceExportService;
    @PostMapping("/pay")
    public ApiResponse<InvoiceResponse> payInvoice(@RequestBody @Valid PayInvoiceRequest request) {
        log.info("Controller: {}", request);
        return ApiResponse.<InvoiceResponse>builder()
                .result(invoiceService.payInvoice(request))
                .build();
    }

    @PutMapping("/update-items")
    public ApiResponse<InvoiceResponse> updateInvoiceItems(@RequestBody @Valid UpdateInvoiceRequest request) {
        log.info("Controller: update invoice items for invoiceId={}, staffId={}", request.getInvoiceId(), request.getStaffId());
        return ApiResponse.<InvoiceResponse>builder()
                .result(invoiceService.updateInvoiceItems(request))
                .build();
    }


    // Xem hóa đơn (hiển thị trực tiếp trên trình duyệt)
    @GetMapping("/{id}/preview")
    public ResponseEntity<byte[]> previewInvoice(@PathVariable String id) {
        log.info("Controller - Preview Invoice: {}", id);
        ByteArrayInputStream pdfStream = invoiceService.generateInvoicePdf(id);
        byte[] pdfBytes;

        pdfBytes = pdfStream.readAllBytes();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.inline()
                .filename("invoice-" + id + ".pdf").build());

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    // Tải hóa đơn (bắt tải xuống file PDF)
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadInvoice(@PathVariable String id) {
        log.info("Controller - Download Invoice: {}", id);
        ByteArrayInputStream pdfStream = invoiceService.generateInvoicePdf(id);
        byte[] pdfBytes;

        pdfBytes = pdfStream.readAllBytes();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("invoice-" + id + ".pdf").build());

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ApiResponse<InvoiceDetailResponse> getInvoiceDetail(@PathVariable String id) {
        log.info("Controller - Get Invoice Detail: {}", id);
        InvoiceDetailResponse response = invoiceService.getInvoiceDetail(id);
        return ApiResponse.<InvoiceDetailResponse>builder()
                .result(response)
                .build();
    }

    @GetMapping
    public ApiResponse<InvoiceStatisticResponse> getInvoices(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        log.info("Controller: get invoices with filters={}, page={}, size={}, sortBy={}, sortDir={}",
                filters, page, size, sortBy, sortDir);

        Page<InvoiceResponse> pagedResult = invoiceService.getInvoicesPaged(filters, page, size, sortBy, sortDir);

        long totalInvoices = pagedResult.getTotalElements();
        BigDecimal totalAmount = invoiceService.sumTotalAmount(filters);
        BigDecimal monthlyRevenue = invoiceService.sumMonthlyRevenue();
        long validInvoices = invoiceService.countValidInvoices(filters);

        InvoiceStatisticResponse result = InvoiceStatisticResponse.builder()
                .data(new PagedResponse<>(pagedResult.getContent(), pagedResult.getNumber(),
                        pagedResult.getSize(), pagedResult.getTotalElements(),
                        pagedResult.getTotalPages(), pagedResult.isLast()))
                .totalInvoices(totalInvoices)
                .totalAmount(totalAmount)
                .monthlyRevenue(monthlyRevenue)
                .validInvoices(validInvoices)
                .build();

        return ApiResponse.<InvoiceStatisticResponse>builder().result(result).build();
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportInvoicesExcel(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir
    ) throws IOException {
        List<InvoiceResponse> invoices = invoiceService.getAllInvoices(filters, sortBy, sortDir);
        ByteArrayInputStream in = invoiceExportService.exportInvoiceToExcel(invoices);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=invoices.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(in.readAllBytes());
    }

}
