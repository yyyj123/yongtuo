package com.yongtuo.site.site;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class CompanyProfileSeedTest {
    @Container static final MySQLContainer MYSQL=new MySQLContainer("mysql:8.0.45");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {r.add("spring.datasource.url",MYSQL::getJdbcUrl);r.add("spring.datasource.username",MYSQL::getUsername);r.add("spring.datasource.password",MYSQL::getPassword);}
    @Autowired JdbcTemplate jdbc; @Autowired MockMvc mvc;
    @Test void seedsOnlyApprovedFactsAndKeepsEnglishDraftUnpublished() throws Exception {
        String profile=jdbc.queryForObject("SELECT value_zh FROM site_config WHERE config_key='company_profile'",String.class);
        assertThat(profile).contains("勇拓","2011","普通车床","CNC","图纸","样品","不锈钢","铜","铝","铁","轴","螺丝","连接","车削","成型","精加工","尺寸校准")
                .doesNotContain("ISO","认证","±","产能","出口","台设备");
        assertThat(jdbc.queryForObject("SELECT value_en FROM site_config WHERE config_key='brand'",String.class)).isEqualTo("YONGTUO");
        assertThat(jdbc.queryForObject("SELECT value_en FROM site_config WHERE config_key='company_legal_name'",String.class)).isNull();
        assertThat(jdbc.queryForObject("SELECT english_status FROM site_config WHERE config_key='company_profile'",String.class)).isEqualTo("AI_DRAFT");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM certificate",Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM contact_info",Integer.class)).isZero();
        mvc.perform(get("/api/v1/public/site").header("Accept-Language","en"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.config.company_profile").doesNotExist());
    }
}
