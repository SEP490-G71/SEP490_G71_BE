package vn.edu.fpt.medicaldiagnosis.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.util.HashMap;

@Configuration
public class DataSourceConfig {

    private final DataSource controlDataSource;

    public DataSourceConfig(@Qualifier("controlDataSource") DataSource controlDataSource) {
        this.controlDataSource = controlDataSource;
    }

    @Bean
    @Primary
    public DataSource dataSource(DataSourceProvider provider) {
        TenantRoutingDataSource routingDataSource = new TenantRoutingDataSource(provider, controlDataSource);
        routingDataSource.setTargetDataSources(new HashMap<>());
        routingDataSource.setDefaultTargetDataSource(controlDataSource);
        routingDataSource.afterPropertiesSet();
        return routingDataSource;
    }
}

