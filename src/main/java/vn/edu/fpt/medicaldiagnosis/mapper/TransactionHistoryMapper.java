package vn.edu.fpt.medicaldiagnosis.mapper;

import org.mapstruct.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.TransactionHistoryRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.TransactionHistoryResponse;
import vn.edu.fpt.medicaldiagnosis.entity.TransactionHistory;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TransactionHistoryMapper {

    TransactionHistory toEntity(TransactionHistoryRequest request);

    TransactionHistoryResponse toResponse(TransactionHistory transactionHistory);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(@MappingTarget TransactionHistory entity, TransactionHistoryRequest request);
}
