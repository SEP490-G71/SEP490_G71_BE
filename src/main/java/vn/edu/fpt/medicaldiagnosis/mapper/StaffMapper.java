package vn.edu.fpt.medicaldiagnosis.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import vn.edu.fpt.medicaldiagnosis.dto.request.StaffCreateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.StaffUpdateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.DepartmentBasicInfo;
import vn.edu.fpt.medicaldiagnosis.dto.response.StaffBasicResponse;
import vn.edu.fpt.medicaldiagnosis.dto.response.StaffResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Department;
import vn.edu.fpt.medicaldiagnosis.entity.Staff;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface StaffMapper {

    StaffBasicResponse toBasicResponse(Staff staff);

    Staff toStaff(StaffCreateRequest request);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "department", expression = "java(toDepartmentBasicInfo(staff.getDepartment()))")
    StaffResponse toStaffResponse(Staff staff);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateStaff(@MappingTarget Staff staff, StaffUpdateRequest request);

    // ✅ Thêm hàm default để map từ Department -> DepartmentBasicInfo
    default DepartmentBasicInfo toDepartmentBasicInfo(Department department) {
        if (department == null) return null;
        return DepartmentBasicInfo.builder()
                .id(department.getId())
                .name(department.getName())
                .roomNumber(department.getRoomNumber())
                .build();
    }
}

