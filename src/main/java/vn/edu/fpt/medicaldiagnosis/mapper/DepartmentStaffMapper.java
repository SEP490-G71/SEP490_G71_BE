package vn.edu.fpt.medicaldiagnosis.mapper;


import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import vn.edu.fpt.medicaldiagnosis.dto.request.DepartmentStaffCreateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.StaffUpdateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.DepartmentStaffResponse;
import vn.edu.fpt.medicaldiagnosis.entity.DepartmentStaff;
import vn.edu.fpt.medicaldiagnosis.entity.Staff;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)

public interface DepartmentStaffMapper {


    DepartmentStaff toDepartmentStaff(DepartmentStaffCreateRequest request);

    @Mapping(target = "id", source = "id")
    @Mapping(source = "department.id", target = "departmentId")
    @Mapping(source = "staff.id", target = "staffId")
    @Mapping(source = "department.name", target = "departmentName")
    @Mapping(source = "staff.firstName", target = "firstName")
    @Mapping(source = "staff.middleName", target = "middleName")
    @Mapping(source = "staff.lastName", target = "lastName")
    DepartmentStaffResponse toDepartmentStaffResponse(DepartmentStaff departmentStaff);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateStaff(@MappingTarget Staff staff, StaffUpdateRequest request);
}
