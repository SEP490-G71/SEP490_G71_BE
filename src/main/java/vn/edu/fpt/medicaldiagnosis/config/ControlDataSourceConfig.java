package vn.edu.fpt.medicaldiagnosis.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class ControlDataSourceConfig {

    @Value("${spring.datasource.control.url}")
    private String url;

    @Value("${spring.datasource.control.username}")
    private String username;

    @Value("${spring.datasource.control.password}")
    private String password;

    @Bean("controlDataSource")
    public DataSource controlDataSource() {
        return DataSourceBuilder.create()
                .url(url)
                .username(username)
                .password(password)
                .driverClassName("com.mysql.cj.jdbc.Driver")
                .build();
    }
}
