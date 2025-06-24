package vn.edu.fpt.medicaldiagnosis.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.NullValuePropertyMappingStrategy;
import vn.edu.fpt.medicaldiagnosis.dto.response.InvoiceResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Invoice;
import vn.edu.fpt.medicaldiagnosis.enums.PaymentType;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)

public interface InvoiceMapper {

    @Mapping(target = "invoiceId", source = "id")
    @Mapping(target = "patientName", source = "patient.fullName")
    @Mapping(target = "paymentType", source = "paymentType", qualifiedByName = "mapPaymentTypeToString")
    InvoiceResponse toInvoiceResponse(Invoice invoice);

    @Named("mapPaymentTypeToString")
    static String mapPaymentTypeToString(PaymentType paymentType) {
        return paymentType != null ? paymentType.name() : null;
    }
}
