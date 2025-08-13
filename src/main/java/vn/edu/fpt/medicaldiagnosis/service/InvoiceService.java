package vn.edu.fpt.medicaldiagnosis.service;

import org.springframework.data.domain.Page;
import vn.edu.fpt.medicaldiagnosis.dto.request.PayInvoiceRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.UpdateInvoiceRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.DailyRevenueSeriesResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.InvoiceDetailResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.InvoiceResponse;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

public interface InvoiceService {
    InvoiceResponse payInvoice(PayInvoiceRequest request);

    Page<InvoiceResponse> getInvoicesPaged(Map<String, String> filters, int page, int size, String sortBy, String sortDir);

    InvoiceResponse updateInvoiceItems(UpdateInvoiceRequest request);

    ByteArrayInputStream generateInvoicePdf(String invoiceId);

    InvoiceDetailResponse getInvoiceDetail(String id);

    BigDecimal sumTotalAmount(Map<String, String> filters);

    BigDecimal sumMonthlyRevenue();

    long countValidInvoices(Map<String, String> filters);

    List<InvoiceResponse> getAllInvoices(Map<String, String> filters, String sortBy, String sortDir);

    DailyRevenueSeriesResponse getDailySeries(YearMonth ym);
}
