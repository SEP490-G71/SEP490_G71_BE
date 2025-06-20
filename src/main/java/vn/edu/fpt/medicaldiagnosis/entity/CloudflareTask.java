package vn.edu.fpt.medicaldiagnosis.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.edu.fpt.medicaldiagnosis.enums.Status;

@Entity
@Table(name = "cloudflare_tasks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloudflareTask extends AuditableEntity {
    @Id
    private String id;

    @Column(name = "subdomain", nullable = false)
    private String subdomain;

    @Enumerated(EnumType.STRING)
    private Status status;

    @Column(name = "retry_count")
    private int retryCount;

}
