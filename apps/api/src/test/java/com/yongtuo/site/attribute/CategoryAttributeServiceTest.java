package com.yongtuo.site.attribute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.yongtuo.site.category.AdminCategoryWriteRequest;
import com.yongtuo.site.category.CategoryMode;
import com.yongtuo.site.category.CategoryService;
import com.yongtuo.site.category.CategoryStatus;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CategoryAttributeServiceTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.45")
            .withCommand("--log-bin-trust-function-creators=1");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired AttributeDefinitionService definitionService;
    @Autowired CategoryAttributeService bindingService;
    @Autowired CategoryService categoryService;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;

    @BeforeEach
    void clearAttributes() {
        jdbc.update("DELETE FROM product_variant_value");
        jdbc.update("DELETE FROM product_attribute_value");
        jdbc.update("DELETE FROM category_attribute");
        jdbc.update("DELETE FROM attribute_option");
        jdbc.update("DELETE FROM attribute_definition");
        jdbc.update("DELETE FROM product_variant");
        jdbc.update("DELETE FROM product");
    }

    @Test
    void globalDefaultsAndCategoryBindingsAreCombinedAndBindingOverridesDefaults() {
        long category = category("binding-category");
        long otherCategory = category("other-category");
        AdminAttributeDto material = definitionService.create(attribute("material", AttributeDataType.TEXT,
                true, true, false, List.of()));
        AdminAttributeDto finish = definitionService.create(attribute("finish", AttributeDataType.SELECT,
                false, false, false, List.of(option("matte"), option("polished"))));

        bindingService.replaceCategoryBindings(category, List.of(
                new CategoryAttributeBindingRequest(material.id(), false, true, true, 10),
                new CategoryAttributeBindingRequest(finish.id(), true, false, true, 20)));

        assertThat(bindingService.getCategoryAttributes(category)).extracting(CategoryAttributeDto::code)
                .containsExactly("material", "finish");
        CategoryAttributeDto overridden = bindingService.getCategoryAttributes(category).getFirst();
        assertThat(overridden.isFilterable()).isFalse();
        assertThat(overridden.isRequired()).isTrue();
        assertThat(overridden.showInDetail()).isTrue();
        assertThat(bindingService.getCategoryAttributes(otherCategory)).extracting(CategoryAttributeDto::code)
                .containsExactly("material");
        assertThat(bindingService.getCategoryAttributes(otherCategory).getFirst().isFilterable()).isTrue();
    }

    @Test
    void validatesDataTypeOptionsAndActiveReferences() {
        long category = category("validation-category");
        AdminAttributeDto select = definitionService.create(attribute("choice", AttributeDataType.SELECT,
                false, false, false, List.of(option("one"))));
        assertThatThrownBy(() -> definitionService.create(attribute("choice", AttributeDataType.TEXT,
                false, false, false, List.of())))
                .isInstanceOf(AttributeBusinessException.class).extracting("code").isEqualTo(22002);
        assertThatThrownBy(() -> definitionService.create(attribute("bad-text", AttributeDataType.TEXT,
                false, false, false, List.of(option("not-allowed")))))
                .isInstanceOf(AttributeBusinessException.class).extracting("code").isEqualTo(22005);
        assertThatThrownBy(() -> bindingService.replaceCategoryBindings(category, List.of(
                new CategoryAttributeBindingRequest(select.id(), true, false, true, 0),
                new CategoryAttributeBindingRequest(select.id(), false, false, true, 1))))
                .isInstanceOf(AttributeBusinessException.class).extracting("code").isEqualTo(22004);

        AdminAttributeOptionDto one = select.options().getFirst();
        assertThatThrownBy(() -> definitionService.update(select.id(), attribute("choice", AttributeDataType.SELECT,
                false, false, false, List.of(optionWithId(999999L, "one")))))
                .isInstanceOf(AttributeBusinessException.class).extracting("code").isEqualTo(22009);
        definitionService.update(select.id(), attribute("choice", AttributeDataType.SELECT,
                false, false, false, List.of(optionWithId(one.id(), "one"), option("two"))));
        assertThat(definitionService.get(select.id()).options()).extracting(AdminAttributeOptionDto::valueCode)
                .containsExactly("one", "two");
        definitionService.update(select.id(), attribute("choice", AttributeDataType.SELECT,
                false, false, false, List.of(optionWithId(one.id(), "one"))));
        assertThat(definitionService.get(select.id()).options()).extracting(AdminAttributeOptionDto::valueCode)
                .containsExactly("one");
    }

    @Test
    void inactiveCategoryAndAttributeCannotBeBoundAndReplacementIsAtomic() {
        long category = category("inactive-category");
        AdminAttributeDto active = definitionService.create(attribute("active", AttributeDataType.NUMBER,
                false, true, false, List.of()));
        AdminAttributeDto inactive = definitionService.create(attribute("inactive", AttributeDataType.TEXT,
                false, false, false, List.of()));
        definitionService.update(inactive.id(), attribute("inactive", AttributeDataType.TEXT,
                false, false, true, List.of()));

        bindingService.replaceCategoryBindings(category, List.of(
                new CategoryAttributeBindingRequest(active.id(), true, false, true, 0)));
        assertThatThrownBy(() -> bindingService.replaceCategoryBindings(category, List.of(
                new CategoryAttributeBindingRequest(active.id(), true, false, true, 0),
                new CategoryAttributeBindingRequest(inactive.id(), true, false, true, 1))))
                .isInstanceOf(AttributeBusinessException.class).extracting("code").isEqualTo(22006);
        assertThat(bindingService.getCategoryAttributes(category)).extracting(CategoryAttributeDto::code)
                .containsExactly("active");
    }

    @Test
    void categoryAndAttributeDeletionProtectExistingBindingsAndValues() {
        long category = category("delete-protection-category");
        AdminAttributeDto select = definitionService.create(attribute("used-choice", AttributeDataType.SELECT,
                false, false, false, List.of(option("used"))));
        bindingService.replaceCategoryBindings(category, List.of(
                new CategoryAttributeBindingRequest(select.id(), true, false, true, 0)));
        assertThatThrownBy(() -> categoryService.softDelete(category))
                .isInstanceOf(com.yongtuo.site.category.CategoryBusinessException.class)
                .extracting("code").isEqualTo(21002);

        jdbc.update("INSERT INTO product(category_id, product_code, name_zh, slug) VALUES (?, ?, ?, ?)",
                category, "YT-ATTRIBUTE-USED", "使用属性的产品", "attribute-used-product");
        long product = jdbc.queryForObject("SELECT id FROM product WHERE product_code = 'YT-ATTRIBUTE-USED'", Long.class);
        long option = select.options().getFirst().id();
        jdbc.update("INSERT INTO product_attribute_value(product_id, attribute_id, option_id) VALUES (?, ?, ?)",
                product, select.id(), option);

        definitionService.update(select.id(), attribute("used-choice", AttributeDataType.SELECT,
                false, false, false, List.of(option("replacement"))));
        assertThat(jdbc.queryForObject("SELECT status FROM attribute_option WHERE id = ?", String.class, option))
                .isEqualTo("INACTIVE");
        assertThatThrownBy(() -> definitionService.delete(select.id()))
                .isInstanceOf(AttributeBusinessException.class).extracting("code").isEqualTo(22007);
    }

    @Test
    void bindingWaitsForAttributeDeactivationThenRejectsInactiveAttribute() throws Exception {
        long category = category("attribute-lock-category");
        AdminAttributeDto definition = definitionService.create(attribute("lockable", AttributeDataType.TEXT,
                false, false, false, List.of()));
        String lockName = "attribute-deactivate-binding-race";
        jdbc.execute("DROP TRIGGER IF EXISTS attribute_update_gate");
        jdbc.execute("""
                CREATE TRIGGER attribute_update_gate BEFORE UPDATE ON attribute_definition
                FOR EACH ROW BEGIN
                    IF OLD.id = %d THEN
                        SET @attribute_update_gate = GET_LOCK('%s', 10);
                        SET @attribute_update_release = RELEASE_LOCK('%s');
                    END IF;
                END
                """.formatted(definition.id(), lockName, lockName));
        try (Connection gate = MYSQL.createConnection("");
             ExecutorService executor = Executors.newFixedThreadPool(2)) {
            assertThat(namedLock(gate, "SELECT GET_LOCK(?, 2)", lockName)).isEqualTo(1);
            Future<AdminAttributeDto> deactivate = executor.submit(() -> definitionService.update(definition.id(),
                    attribute("lockable", AttributeDataType.TEXT, false, false, true, List.of())));
            assertBlocked(deactivate);
            Future<List<CategoryAttributeDto>> bind = executor.submit(() -> bindingService.replaceCategoryBindings(category,
                    List.of(new CategoryAttributeBindingRequest(definition.id(), true, false, true, 0))));
            assertBlocked(bind);
            assertThat(namedLock(gate, "SELECT RELEASE_LOCK(?)", lockName)).isEqualTo(1);
            assertThat(deactivate.get(5, TimeUnit.SECONDS).status()).isEqualTo(AttributeStatus.INACTIVE);
            assertBusinessFailure(bind, 22006);
        } finally {
            jdbc.execute("DROP TRIGGER IF EXISTS attribute_update_gate");
        }
    }

    @Test
    void bindingWaitsForCategoryDeactivationThenRejectsInactiveCategory() throws Exception {
        long category = category("category-lock-category");
        AdminAttributeDto definition = definitionService.create(attribute("category-lock-attribute", AttributeDataType.TEXT,
                false, false, false, List.of()));
        String lockName = "category-deactivate-binding-race";
        jdbc.execute("DROP TRIGGER IF EXISTS category_update_gate");
        jdbc.execute("""
                CREATE TRIGGER category_update_gate BEFORE UPDATE ON product_category
                FOR EACH ROW BEGIN
                    IF OLD.id = %d THEN
                        SET @category_update_gate = GET_LOCK('%s', 10);
                        SET @category_update_release = RELEASE_LOCK('%s');
                    END IF;
                END
                """.formatted(category, lockName, lockName));
        try (Connection gate = MYSQL.createConnection("");
             ExecutorService executor = Executors.newFixedThreadPool(2)) {
            assertThat(namedLock(gate, "SELECT GET_LOCK(?, 2)", lockName)).isEqualTo(1);
            Future<com.yongtuo.site.category.AdminCategoryDto> deactivate = executor.submit(() ->
                    categoryService.update(category, new AdminCategoryWriteRequest(null, "已停用", "Inactive",
                            "category-lock-category", null, null, null, CategoryMode.NORMAL, 0,
                            CategoryStatus.INACTIVE, false, null, null, null, null)));
            assertBlocked(deactivate);
            Future<List<CategoryAttributeDto>> bind = executor.submit(() -> bindingService.replaceCategoryBindings(category,
                    List.of(new CategoryAttributeBindingRequest(definition.id(), true, false, true, 0))));
            assertBlocked(bind);
            assertThat(namedLock(gate, "SELECT RELEASE_LOCK(?)", lockName)).isEqualTo(1);
            assertThat(deactivate.get(5, TimeUnit.SECONDS).status()).isEqualTo(CategoryStatus.INACTIVE);
            assertBusinessFailure(bind, 22006);
        } finally {
            jdbc.execute("DROP TRIGGER IF EXISTS category_update_gate");
        }
    }

    @Test
    void adminRoutesAreAuthenticatedAndExposeDefinitionAndBindingContracts() throws Exception {
        long category = category("route-category");
        String body = """
                {"nameZh":"材质","nameEn":"Material","code":"route-material",
                 "dataType":"TEXT","isGlobal":true,"defaultFilterable":true,
                 "defaultRequired":false,"sortOrder":0,"status":"ACTIVE","options":[]}
                """;
        mvc.perform(get("/api/v1/admin/attributes")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/admin/attributes").with(user("synthetic-admin"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.code").value("route-material"));
        long id = jdbc.queryForObject("SELECT id FROM attribute_definition WHERE code = 'route-material'", Long.class);
        mvc.perform(put("/api/v1/admin/categories/{id}/attributes", category).with(user("synthetic-admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"attributeId\":" + id + ",\"isFilterable\":false,\"isRequired\":true,\"showInDetail\":true,\"sortOrder\":0}]") )
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].code").value("route-material"));
        mvc.perform(get("/api/v1/admin/categories/{id}/attributes", category).with(user("synthetic-admin")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].isRequired").value(true));
    }

    private long category(String slug) {
        return categoryService.create(new AdminCategoryWriteRequest(null, "测试分类", "Synthetic category",
                slug, null, null, null, CategoryMode.NORMAL, 0, CategoryStatus.ACTIVE, false,
                null, null, null, null)).id();
    }

    private static AdminAttributeWriteRequest attribute(String code, AttributeDataType type,
                                                         boolean global, boolean filterable,
                                                         boolean inactive, List<AdminAttributeOptionRequest> options) {
        return new AdminAttributeWriteRequest("属性 " + code, "Attribute " + code, code, type,
                null, global, filterable, false, 0,
                inactive ? AttributeStatus.INACTIVE : AttributeStatus.ACTIVE, options);
    }

    private static AdminAttributeOptionRequest option(String code) {
        return optionWithId(null, code);
    }

    private static AdminAttributeOptionRequest optionWithId(Long id, String code) {
        return new AdminAttributeOptionRequest(id, code, "选项 " + code, "Option " + code,
                0, AttributeStatus.ACTIVE);
    }

    private static void assertBlocked(Future<?> future) {
        assertThatThrownBy(() -> future.get(750, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);
    }

    private static void assertBusinessFailure(Future<?> future, int code) {
        assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class).cause().isInstanceOf(AttributeBusinessException.class)
                .extracting("code").isEqualTo(code);
    }

    private static int namedLock(Connection connection, String sql, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, name);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                return result.getInt(1);
            }
        }
    }
}
