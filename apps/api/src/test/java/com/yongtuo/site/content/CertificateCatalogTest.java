package com.yongtuo.site.content;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest @AutoConfigureMockMvc @Testcontainers
class CertificateCatalogTest {
    @Container static final MySQLContainer MYSQL=new MySQLContainer("mysql:8.0.45");
    @DynamicPropertySource static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",MYSQL::getJdbcUrl); r.add("spring.datasource.username",MYSQL::getUsername); r.add("spring.datasource.password",MYSQL::getPassword);
    }
    @Autowired JdbcTemplate jdbc; @Autowired MockMvc mvc;
    @BeforeEach void reset() { jdbc.update("DELETE FROM certificate");jdbc.update("DELETE FROM catalog"); }
    @Test void certificatePublicAndDownloadFlagsRemainIndependent() throws Exception {
        for(boolean pub:new boolean[]{false,true}) for(boolean download:new boolean[]{false,true}) {
            String body="""
                    {"type":"SYNTHETIC_TEST","nameZh":"测试证书","nameEn":"Synthetic certificate",
                    "englishStatus":"CONFIRMED","coverImage":"https://example.com/preview.jpg",
                    "fileUrl":"https://example.com/%s-%s.pdf","isPublic":%s,"allowDownload":%s,
                    "isFeatured":false,"sortOrder":0,"status":"PUBLISHED"}
                    """.formatted(pub,download,pub,download);
            mvc.perform(post("/api/v1/admin/certificates").with(user("admin")).contentType("application/json").content(body)).andExpect(status().isOk());
        }
        String body=mvc.perform(get("/api/v1/public/certificates").header("Accept-Language","en"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).contains("true-true.pdf").doesNotContain("true-false.pdf","false-true.pdf","false-false.pdf","fileUrl");
        jdbc.update("UPDATE certificate SET english_status='AI_DRAFT'");
        mvc.perform(get("/api/v1/public/certificates").header("Accept-Language","en")).andExpect(jsonPath("$.data.length()").value(0));
    }
    @Test void primaryCatalogIsUniquePerLanguageAndOldVersionsRemain() throws Exception {
        long old=createCatalog("old","ZH_ONLY"), latest=createCatalog("latest","ZH_ONLY"), en=createCatalog("english","EN_ONLY");
        primary(old);primary(en);primary(latest);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM catalog",Integer.class)).isEqualTo(3);
        assertThat(jdbc.queryForObject("SELECT is_primary FROM catalog WHERE id=?",Boolean.class,old)).isFalse();
        mvc.perform(get("/api/v1/public/catalogs").header("Accept-Language","zh-CN"))
                .andExpect(jsonPath("$.data.length()").value(2)).andExpect(jsonPath("$.data[0].version").value("latest"));
        mvc.perform(get("/api/v1/public/catalogs").header("Accept-Language","en"))
                .andExpect(jsonPath("$.data.length()").value(1)).andExpect(jsonPath("$.data[0].isPrimary").value(true));
        long bilingual=createCatalog("both","BILINGUAL");primary(bilingual);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM catalog WHERE is_primary=1",Integer.class)).isEqualTo(1);
        mvc.perform(delete("/api/v1/admin/catalogs/"+bilingual).with(user("admin"))).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM catalog",Integer.class)).isEqualTo(4);
    }
    private long createCatalog(String version,String mode) throws Exception {
        String response=mvc.perform(post("/api/v1/admin/catalogs").with(user("admin")).contentType("application/json").content("""
                {"titleZh":"测试目录","titleEn":"Synthetic catalog","languageMode":"%s","englishStatus":"CONFIRMED",
                "version":"%s","fileUrl":"https://example.com/synthetic.pdf","status":"PUBLISHED"}
                """.formatted(mode,version))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return ((Number)JsonPath.read(response,"$.data.id")).longValue();
    }
    @Test void concurrentPrimaryChangesKeepOneLanguageSlot() throws Exception {
        long first=createCatalog("concurrent-a","ZH_ONLY"),second=createCatalog("concurrent-b","ZH_ONLY");
        var start=new java.util.concurrent.CountDownLatch(1);
        try(var executor=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var a=executor.submit(()->{start.await();primary(first);return true;});
            var b=executor.submit(()->{start.await();primary(second);return true;});
            start.countDown();
            assertThat(a.get(15,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            assertThat(b.get(15,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM catalog WHERE primary_zh=1",Integer.class)).isEqualTo(1);
    }
    private void primary(long id) throws Exception { mvc.perform(put("/api/v1/admin/catalogs/"+id+"/primary").with(user("admin"))).andExpect(status().isOk()); }
}
