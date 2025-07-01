package vn.edu.fpt.medicaldiagnosis.dto.response;

import lombok.Builder;
import lombok.Data;
import vn.edu.fpt.medicaldiagnosis.enums.TemplateFileType;

@Data
@Builder
public class TemplateFileResponse {
    private String id;
    private String name;
    private TemplateFileType type;
    private String fileUrl;
    private String previewUrl;
    private Boolean isDefault;
}

