package vn.edu.fpt.medicaldiagnosis.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import vn.edu.fpt.medicaldiagnosis.dto.request.StaffCreateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.request.StaffUpdateRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.StaffResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Staff;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface StaffMapper {


    Staff toStaff(StaffCreateRequest request);

    @Mapping(target = "id", source = "id")
    StaffResponse toStaffResponse(Staff staff);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateStaff(@MappingTarget Staff staff, StaffUpdateRequest request);
}
