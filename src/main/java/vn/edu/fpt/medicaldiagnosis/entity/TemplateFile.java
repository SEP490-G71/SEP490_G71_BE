package vn.edu.fpt.medicaldiagnosis.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Where;
import vn.edu.fpt.medicaldiagnosis.enums.TemplateFileType;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Where(clause = "deleted_at IS NULL")
@Table(name = "template_files")
public class TemplateFile extends AuditableEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Enumerated(EnumType.STRING)
    private TemplateFileType type;

    private String name;

    @Column(name = "file_url")
    private String fileUrl;

    private String description;

    @Column(name = "preview_url")
    private String previewUrl;

    @Column(name = "is_default")
    private Boolean isDefault;
}
