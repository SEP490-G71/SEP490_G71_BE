package vn.edu.fpt.medicaldiagnosis.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateInvoiceRequest {
    @NotBlank
    private String invoiceId;

    @NotBlank
    private String staffId;

    @NotEmpty
    private List<InvoiceItemUpdateRequest> services;

    @Data
    public static class InvoiceItemUpdateRequest {
        @NotBlank
        private String serviceId;

        @Min(1)
        private int quantity;
    }
}
