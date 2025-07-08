package vn.edu.fpt.medicaldiagnosis.service;

import vn.edu.fpt.medicaldiagnosis.dto.response.InvoiceItemStatisticResponse;

import java.util.Map;

public interface InvoiceItemService {
    InvoiceItemStatisticResponse getInvoiceItemStatistics(Map<String, String> filters, int page, int size, String sortBy, String sortDir);
}
