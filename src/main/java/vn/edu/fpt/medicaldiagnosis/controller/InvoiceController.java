package vn.edu.fpt.medicaldiagnosis.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.PayInvoiceRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.UpdateInvoiceRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ApiResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.InvoiceResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.MedicalServiceResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.PagedResponse;
import vn.edu.fpt.medicaldiagnosis.enums.InvoiceStatus;
import vn.edu.fpt.medicaldiagnosis.exception.AppException;
import vn.edu.fpt.medicaldiagnosis.exception.ErrorCode;
import vn.edu.fpt.medicaldiagnosis.service.InvoiceService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static lombok.AccessLevel.PRIVATE;

@RestController
@RequestMapping("/invoice")
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = PRIVATE, makeFinal = true)
public class InvoiceController {
    InvoiceService invoiceService;
    @PostMapping("/pay")
    public ApiResponse<InvoiceResponse> payInvoice(@RequestBody @Valid PayInvoiceRequest request) {
        log.info("Controller: {}", request);
        return ApiResponse.<InvoiceResponse>builder()
                .result(invoiceService.payInvoice(request))
                .build();
    }

    @GetMapping
    public ApiResponse<PagedResponse<InvoiceResponse>> getInvoices(
            @RequestParam Map<String, String> filters,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        log.info("Controller: get invoices with filters={}, page={}, size={}, sortBy={}, sortDir={}",
                filters, page, size, sortBy, sortDir);

        Page<InvoiceResponse> result = invoiceService.getInvoicesPaged(filters, page, size, sortBy, sortDir);

        PagedResponse<InvoiceResponse> response = new PagedResponse<>(
                result.getContent(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages(),
                result.isLast()
        );
        return ApiResponse.<PagedResponse<InvoiceResponse>>builder().result(response).build();
    }

    @PutMapping("/update-items")
    public ApiResponse<InvoiceResponse> updateInvoiceItems(@RequestBody @Valid UpdateInvoiceRequest request) {
        log.info("Controller: update invoice items for invoiceId={}, staffId={}", request.getInvoiceId(), request.getStaffId());
        return ApiResponse.<InvoiceResponse>builder()
                .result(invoiceService.updateInvoiceItems(request))
                .build();
    }


    // ✅ Xem hóa đơn (hiển thị trực tiếp trên trình duyệt)
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

    // ✅ Tải hóa đơn (bắt tải xuống file PDF)
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
}
