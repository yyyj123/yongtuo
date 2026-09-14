package com.yongtuo.site.audit;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
@Configuration
public class OperationAuditConfiguration implements WebMvcConfigurer {
    private final org.springframework.beans.factory.ObjectProvider<org.springframework.jdbc.core.JdbcTemplate> jdbc;
    private final tools.jackson.databind.ObjectMapper mapper;
    public OperationAuditConfiguration(org.springframework.beans.factory.ObjectProvider<org.springframework.jdbc.core.JdbcTemplate> jdbc,
                                       tools.jackson.databind.ObjectMapper mapper) { this.jdbc=jdbc;this.mapper=mapper; }
    @Override public void addInterceptors(InterceptorRegistry registry) {
        var database=jdbc.getIfAvailable();
        if(database!=null) registry.addInterceptor(new OperationAuditInterceptor(database,mapper)).addPathPatterns("/api/v1/admin/**");
    }
}
