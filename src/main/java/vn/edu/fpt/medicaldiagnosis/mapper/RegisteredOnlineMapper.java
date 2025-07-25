package vn.edu.fpt.medicaldiagnosis.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import vn.edu.fpt.medicaldiagnosis.dto.request.RegisteredOnlineRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.RegisteredOnlineResponse;
import vn.edu.fpt.medicaldiagnosis.entity.RegisteredOnline;

@Mapper(componentModel = "spring")
public interface RegisteredOnlineMapper {

    RegisteredOnline toEntity(RegisteredOnlineRequest request);

    RegisteredOnlineResponse toResponse(RegisteredOnline entity);

    void updateEntity(@MappingTarget RegisteredOnline entity, RegisteredOnlineRequest request);
}
