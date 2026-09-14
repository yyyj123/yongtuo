package com.yongtuo.site.media;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
@SpringBootTest @AutoConfigureMockMvc @Testcontainers
class AdminMediaTest {
 @Container static final MySQLContainer MYSQL=new MySQLContainer("mysql:8.0.45");
 @DynamicPropertySource static void props(DynamicPropertyRegistry r){r.add("spring.datasource.url",MYSQL::getJdbcUrl);r.add("spring.datasource.username",MYSQL::getUsername);r.add("spring.datasource.password",MYSQL::getPassword);}
 @Autowired MockMvc mvc;@Autowired JdbcTemplate jdbc;
 @Test void rejectsExecutableAndReportsDisabledStorageWithoutWritingMetadata() throws Exception {
  int before=jdbc.queryForObject("SELECT COUNT(*) FROM media_file",Integer.class);
  mvc.perform(multipart("/api/v1/admin/media").file(new MockMultipartFile("file","bad.exe","application/octet-stream",new byte[]{1,2})).with(user("admin"))).andExpect(status().isBadRequest());
  mvc.perform(multipart("/api/v1/admin/media").file(new MockMultipartFile("file","test.pdf","application/pdf","%PDF-1.7\n%%EOF".getBytes())).with(user("admin"))).andExpect(status().isServiceUnavailable());
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM media_file",Integer.class)).isEqualTo(before);
 }
 @Test void ordersGallerySetsCoverAndKeepsAttachmentPermissionsIndependent() throws Exception {
  long category=jdbc.queryForObject("SELECT MIN(id) FROM product_category",Long.class);
  jdbc.update("INSERT INTO product(category_id,product_code,slug,name_zh,status,english_status) VALUES (?,'SYNTHETIC-MEDIA','synthetic-media','测试媒体产品','DRAFT','EMPTY')",category);
  long product=jdbc.queryForObject("SELECT id FROM product WHERE product_code='SYNTHETIC-MEDIA'",Long.class);
  jdbc.update("INSERT INTO media_file(storage_key,original_name,public_url,file_type,mime_type,file_size,status) VALUES ('synthetic/a','test.jpg','https://example.com/test.jpg','IMAGE','image/jpeg',10,'ACTIVE')");
  long media=jdbc.queryForObject("SELECT id FROM media_file WHERE storage_key='synthetic/a'",Long.class);
  mvc.perform(put("/api/v1/admin/products/"+product+"/images").with(user("admin")).contentType("application/json").content("[{\"mediaId\":"+media+",\"altZh\":\"测试图片\",\"sortOrder\":0,\"isCover\":true}]")).andExpect(status().isOk());
  assertThat(jdbc.queryForObject("SELECT cover_image FROM product WHERE id=?",String.class,product)).isEqualTo("https://example.com/test.jpg");
  mvc.perform(put("/api/v1/admin/products/"+product+"/attachments").with(user("admin")).contentType("application/json").content("[{\"mediaId\":"+media+",\"displayNameZh\":\"测试附件\",\"isPublic\":true,\"allowDownload\":false,\"sortOrder\":0}]")).andExpect(status().isOk());
  mvc.perform(get("/api/v1/admin/products/"+product+"/attachments").with(user("admin"))).andExpect(jsonPath("$.data[0].isPublic").value(true)).andExpect(jsonPath("$.data[0].allowDownload").value(false));
  mvc.perform(put("/api/v1/admin/products/"+product+"/images").with(user("admin")).contentType("application/json").content("[{\"mediaId\":999999,\"sortOrder\":0,\"isCover\":true}]")).andExpect(status().isBadRequest());
  assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM product_image WHERE product_id=?",Integer.class,product)).isEqualTo(1);
 }
}
