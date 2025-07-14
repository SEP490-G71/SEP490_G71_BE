package vn.edu.fpt.medicaldiagnosis.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import vn.edu.fpt.medicaldiagnosis.dto.request.DepartmentCreateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.DepartmentUpdateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.DepartmentResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Department;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface DepartmentMapper {

    Department toDepartment(DepartmentCreateRequest request);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "description", expression = "java(department.getDescription() == null ? \"\" : department.getDescription())")
    DepartmentResponse toDepartmentResponse(Department department);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateDepartment(@MappingTarget Department department, DepartmentUpdateRequest request);
}
