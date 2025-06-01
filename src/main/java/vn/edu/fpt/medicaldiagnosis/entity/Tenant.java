package vn.edu.fpt.medicaldiagnosis.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tenants")
public class Tenant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;
    private String name;
    private String code;
    private String dbHost;
    private String dbPort;
    private String dbName;
    private String dbUsername;
    private String dbPassword;
    private String status;

    public String getDbUrl() {
        return "jdbc:mysql://" + dbHost + ":" + dbPort + "/" + dbName;
    }
}
