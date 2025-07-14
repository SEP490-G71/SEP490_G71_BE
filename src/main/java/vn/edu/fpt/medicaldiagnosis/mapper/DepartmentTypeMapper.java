package vn.edu.fpt.medicaldiagnosis.mapper;

import org.mapstruct.Mapper;
import vn.edu.fpt.medicaldiagnosis.dto.response.DepartmentTypeResponse;
import vn.edu.fpt.medicaldiagnosis.entity.DepartmentType;

@Mapper(componentModel = "spring")
public interface DepartmentTypeMapper {
    DepartmentTypeResponse toResponse(DepartmentType entity);
}
