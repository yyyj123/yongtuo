package com.yongtuo.site.translation;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
@SpringBootTest @AutoConfigureMockMvc @Testcontainers
class DisabledTranslationTest {
    @Container static final MySQLContainer MYSQL=new MySQLContainer("mysql:8.0.45");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {r.add("spring.datasource.url",MYSQL::getJdbcUrl);r.add("spring.datasource.username",MYSQL::getUsername);r.add("spring.datasource.password",MYSQL::getPassword);}
    @Autowired JdbcTemplate jdbc;@Autowired MockMvc mvc;
    @Test void defaultApplicationReturnsFeatureNotConfigured() throws Exception {
        long id=jdbc.queryForObject("SELECT id FROM site_config WHERE config_key='company_profile'",Long.class);
        mvc.perform(post("/api/v1/admin/translation/draft").with(user("admin")).contentType("application/json")
                .content("{\"resourceType\":\"SITE_CONFIG\",\"resourceId\":"+id+",\"fields\":[\"value\"],\"replaceConfirmed\":false}"))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.code").value(50001))
                .andExpect(jsonPath("$.message").value("FEATURE_NOT_CONFIGURED"));
    }
}
