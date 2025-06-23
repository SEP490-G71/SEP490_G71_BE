package vn.edu.fpt.medicaldiagnosis.service;

import vn.edu.fpt.medicaldiagnosis.dto.request.PayInvoiceRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.InvoiceResponse;

public interface InvoiceService {
    InvoiceResponse payInvoice(PayInvoiceRequest request);
}
