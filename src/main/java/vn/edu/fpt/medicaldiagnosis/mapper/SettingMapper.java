package vn.edu.fpt.medicaldiagnosis.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import vn.edu.fpt.medicaldiagnosis.dto.request.SettingRequest;
import vn.edu.fpt.medicaldiagnosis.dto.response.SettingResponse;
import vn.edu.fpt.medicaldiagnosis.entity.Setting;

@Mapper(componentModel = "spring")
public interface SettingMapper {

    Setting toEntity(SettingRequest request);

    SettingResponse toResponse(Setting setting);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateSettingFromRequest(SettingRequest request, @MappingTarget Setting setting);
}

