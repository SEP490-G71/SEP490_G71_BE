package vn.edu.fpt.medicaldiagnosis.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterOrderConfig {

    @Bean
    public FilterRegistrationBean<TenantFilter> tenantFilterRegistration(TenantFilter tenantFilter) {
        FilterRegistrationBean<TenantFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(tenantFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(-100);
        return registration;
    }
}
