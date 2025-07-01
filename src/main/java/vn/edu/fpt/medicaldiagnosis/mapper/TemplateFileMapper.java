package vn.edu.fpt.medicaldiagnosis.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import vn.edu.fpt.medicaldiagnosis.dto.response.TemplateFileResponse;
import vn.edu.fpt.medicaldiagnosis.entity.TemplateFile;

@Mapper(componentModel = "spring", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public interface TemplateFileMapper {
    @Mapping(target = "id", source = "id")
    TemplateFileResponse toTemplateFileResponse(TemplateFile templateFile);
}
