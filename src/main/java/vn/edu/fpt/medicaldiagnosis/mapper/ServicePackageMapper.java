package vn.edu.fpt.medicaldiagnosis.mapper;

import org.mapstruct.*;
import vn.edu.fpt.medicaldiagnosis.dto.request.ServicePackageRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.ServicePackageResponse;
import vn.edu.fpt.medicaldiagnosis.entity.ServicePackage;

@Mapper(componentModel = "spring")
public interface ServicePackageMapper {
    ServicePackage toEntity(ServicePackageRequest request);
    ServicePackageResponse toResponse(ServicePackage entity);
    void updateEntityFromRequest(ServicePackageRequest request, @MappingTarget ServicePackage entity);
}

