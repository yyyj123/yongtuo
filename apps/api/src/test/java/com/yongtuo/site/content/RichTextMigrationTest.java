package com.yongtuo.site.content;
import static org.assertj.core.api.Assertions.assertThat;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
@Testcontainers
class RichTextMigrationTest {
    @Container static final MySQLContainer MYSQL=new MySQLContainer("mysql:8.0.45");
    @Test void upgradingCleansPreviouslyStoredRichText() {
        Flyway.configure().dataSource(MYSQL.getJdbcUrl(),MYSQL.getUsername(),MYSQL.getPassword()).target("20").load().migrate();
        var jdbc=new JdbcTemplate(new DriverManagerDataSource(MYSQL.getJdbcUrl(),MYSQL.getUsername(),MYSQL.getPassword()));
        jdbc.update("UPDATE home_section SET content_zh='<p onclick=evil()>Safe</p><script>alert(1)</script>' WHERE section_code='HERO'");
        Flyway.configure().dataSource(MYSQL.getJdbcUrl(),MYSQL.getUsername(),MYSQL.getPassword()).load().migrate();
        assertThat(jdbc.queryForObject("SELECT content_zh FROM home_section WHERE section_code='HERO'",String.class)).isEqualTo("<p>Safe</p>");
    }
}
