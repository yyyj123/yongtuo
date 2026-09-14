package com.yongtuo.site.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Locale;
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
import org.springframework.dao.DuplicateKeyException;
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
class CategoryServiceTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.45")
            .withCommand("--log-bin-trust-function-creators=1");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Autowired CategoryService service;
    @Autowired JdbcTemplate jdbc;
    @Autowired MockMvc mvc;

    @BeforeEach
    void clearCategories() {
        jdbc.update("UPDATE product_category SET parent_id = NULL");
        jdbc.update("DELETE FROM product_category");
    }

    @Test
    void publicTreeIsLocalizedOrderedAndExcludesInactiveOrDeletedRows() {
        long later = create(null, "后显示", "Later", "later", CategoryMode.NORMAL, 20, CategoryStatus.ACTIVE);
        long first = create(null, "先显示", "First", "first", CategoryMode.SHOWCASE, 10, CategoryStatus.ACTIVE);
        create(first, "子项乙", "Child B", "child-b", CategoryMode.NORMAL, 2, CategoryStatus.ACTIVE);
        create(first, "子项甲", "Child A", "child-a", CategoryMode.NORMAL, 1, CategoryStatus.ACTIVE);
        create(null, "隐藏", "Hidden", "hidden", CategoryMode.NORMAL, 0, CategoryStatus.INACTIVE);
        long deleted = create(null, "删除", "Deleted", "deleted", CategoryMode.NORMAL, 0, CategoryStatus.ACTIVE);
        service.softDelete(deleted);

        List<PublicCategoryDto> english = service.getPublicTree(Locale.ENGLISH);

        assertThat(english).extracting(PublicCategoryDto::name).containsExactly("First", "Later");
        assertThat(english.getFirst().mode()).isEqualTo(CategoryMode.SHOWCASE);
        assertThat(english.getFirst().children()).extracting(PublicCategoryDto::name)
                .containsExactly("Child A", "Child B");
        assertThat(service.getPublicTree(Locale.SIMPLIFIED_CHINESE))
                .extracting(PublicCategoryDto::name).containsExactly("先显示", "后显示");
        assertThat(later).isPositive();
    }

    @Test
    void publicSlugDetailUsesRequestedLanguageAndHidesUnavailableRows() {
        create(null, "展示分类", "Showcase category", "showcase", CategoryMode.SHOWCASE, 0, CategoryStatus.ACTIVE);
        create(null, "隐藏分类", "Hidden category", "hidden", CategoryMode.NORMAL, 0, CategoryStatus.INACTIVE);

        PublicCategoryDto category = service.getPublicBySlug("showcase", Locale.ENGLISH);

        assertThat(category.name()).isEqualTo("Showcase category");
        assertThatThrownBy(() -> service.getPublicBySlug("hidden", Locale.ENGLISH))
                .isInstanceOf(CategoryBusinessException.class)
                .extracting("code").isEqualTo(21001);
    }

    @Test
    void createUpdateAndSortRejectInvalidParentsAndCycles() {
        long root = create(null, "根", "Root", "root", CategoryMode.NORMAL, 0, CategoryStatus.ACTIVE);
        long child = create(root, "子", "Child", "child", CategoryMode.NORMAL, 0, CategoryStatus.ACTIVE);

        assertThatThrownBy(() -> create(999999L, "孤儿", "Orphan", "orphan", CategoryMode.NORMAL, 0, CategoryStatus.ACTIVE))
                .isInstanceOf(CategoryBusinessException.class)
                .extracting("code").isEqualTo(21004);
        assertThatThrownBy(() -> service.update(root, request(child, "根", "Root", "root", CategoryMode.NORMAL, 0, CategoryStatus.ACTIVE)))
                .isInstanceOf(CategoryBusinessException.class)
                .extracting("code").isEqualTo(21005);
        assertThatThrownBy(() -> service.sort(List.of(new CategorySortItem(child, child, 1))))
                .isInstanceOf(CategoryBusinessException.class)
                .extracting("code").isEqualTo(21005);
    }

    @Test
    void softDeleteRejectsActiveChildrenAndAllowsSlugReuseAfterDeletion() {
        long root = create(null, "根", "Root", "reusable", CategoryMode.NORMAL, 0, CategoryStatus.ACTIVE);
        long child = create(root, "子", "Child", "child", CategoryMode.NORMAL, 0, CategoryStatus.ACTIVE);

        assertThatThrownBy(() -> service.softDelete(root))
                .isInstanceOf(CategoryBusinessException.class)
                .extracting("code").isEqualTo(21002);

        service.softDelete(child);
        service.softDelete(root);
        long replacement = create(null, "新根", "New root", "reusable", CategoryMode.NORMAL, 0, CategoryStatus.ACTIVE);

        assertThat(replacement).isPositive();
        assertThatThrownBy(() -> create(null, "重复", "Duplicate", "reusable", CategoryMode.NORMAL, 0, CategoryStatus.ACTIVE))
                .isInstanceOfAny(CategoryBusinessException.class, DuplicateKeyException.class);
    }

    @Test
    void publicRoutesAreAnonymousWhileAdminWritesRequireAuthentication() throws Exception {
        create(null, "公开", "Public", "public", CategoryMode.NORMAL, 0, CategoryStatus.ACTIVE);

        mvc.perform(get("/api/v1/public/categories").header("Accept-Language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Public"))
                .andExpect(jsonPath("$.data[0].nameZh").doesNotExist());
        mvc.perform(get("/api/v1/public/categories/public").header("Accept-Language", "zh-CN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("公开"));
        mvc.perform(post("/api/v1/admin/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(null, "受限", "Restricted", "restricted")))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/admin/categories")
                        .with(user("synthetic-admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(null, "管理", "Admin", "admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nameZh").value("管理"))
                .andExpect(jsonPath("$.data.nameEn").value("Admin"));
    }

    @Test
    void publicRoutesDefaultToSimplifiedChineseWithoutAcceptLanguage() throws Exception {
        create(null, "默认中文", "Default English", "default-language",
                CategoryMode.NORMAL, 0, CategoryStatus.ACTIVE);

        mvc.perform(get("/api/v1/public/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("默认中文"));
    }

    @Test
    void deletingParentCannotRaceWithCreatingChild() throws Exception {
        long parent = create(null, "并发父级", "Concurrent parent", "concurrent-parent",
                CategoryMode.NORMAL, 0, CategoryStatus.ACTIVE);
        String lockName = "category-create-parent-race";
        jdbc.execute("DROP TRIGGER IF EXISTS category_insert_gate");
        jdbc.execute("""
                CREATE TRIGGER category_insert_gate BEFORE INSERT ON product_category
                FOR EACH ROW BEGIN
                    SET @category_insert_gate = GET_LOCK('category-create-parent-race', 10);
                    SET @category_insert_release = RELEASE_LOCK('category-create-parent-race');
                END
                """);

        try (Connection gate = MYSQL.createConnection("");
             ExecutorService executor = Executors.newFixedThreadPool(2)) {
            acquireLock(gate, lockName);
            Future<AdminCategoryDto> createChild = executor.submit(() -> service.create(request(
                    parent, "并发子级", "Concurrent child", "concurrent-child",
                    CategoryMode.NORMAL, 0, CategoryStatus.ACTIVE)));
            assertBlocked(createChild);

            Future<Void> deleteParent = executor.submit(() -> {
                service.softDelete(parent);
                return null;
            });
            assertBlocked(deleteParent);

            releaseLock(gate, lockName);
            assertThat(createChild.get(5, TimeUnit.SECONDS).parentId()).isEqualTo(parent);
            assertBusinessFailure(deleteParent, 21002);
            assertThat(service.getAdminTree()).singleElement()
                    .satisfies(root -> assertThat(root.children()).hasSize(1));
        } finally {
            jdbc.execute("DROP TRIGGER IF EXISTS category_insert_gate");
        }
    }

    @Test
    void concurrentOppositeMovesCannotCreateCycle() throws Exception {
        long categoryA = create(null, "并发甲", "Concurrent A", "concurrent-a",
                CategoryMode.NORMAL, 0, CategoryStatus.ACTIVE);
        long categoryB = create(null, "并发乙", "Concurrent B", "concurrent-b",
                CategoryMode.NORMAL, 1, CategoryStatus.ACTIVE);
        String lockName = "category-cycle-race";
        jdbc.execute("DROP TRIGGER IF EXISTS category_update_gate");
        jdbc.execute("""
                CREATE TRIGGER category_update_gate BEFORE UPDATE ON product_category
                FOR EACH ROW BEGIN
                    IF OLD.id = %d THEN
                        SET @category_update_gate = GET_LOCK('category-cycle-race', 10);
                        SET @category_update_release = RELEASE_LOCK('category-cycle-race');
                    END IF;
                END
                """.formatted(categoryA));

        try (Connection gate = MYSQL.createConnection("");
             ExecutorService executor = Executors.newFixedThreadPool(2)) {
            acquireLock(gate, lockName);
            Future<AdminCategoryDto> moveAToB = executor.submit(() -> service.update(categoryA,
                    request(categoryB, "并发甲", "Concurrent A", "concurrent-a",
                            CategoryMode.NORMAL, 0, CategoryStatus.ACTIVE)));
            assertBlocked(moveAToB);

            Future<AdminCategoryDto> moveBToA = executor.submit(() -> service.update(categoryB,
                    request(categoryA, "并发乙", "Concurrent B", "concurrent-b",
                            CategoryMode.NORMAL, 1, CategoryStatus.ACTIVE)));
            assertBlocked(moveBToA);

            releaseLock(gate, lockName);
            assertThat(moveAToB.get(5, TimeUnit.SECONDS).parentId()).isEqualTo(categoryB);
            assertBusinessFailure(moveBToA, 21005);
            assertThat(service.getAdminTree()).singleElement()
                    .satisfies(root -> {
                        assertThat(root.id()).isEqualTo(categoryB);
                        assertThat(root.children()).extracting(AdminCategoryDto::id)
                                .containsExactly(categoryA);
                    });
        } finally {
            jdbc.execute("DROP TRIGGER IF EXISTS category_update_gate");
        }
    }

    @Test
    void deletingParentCannotRaceWithMovingChildIntoIt() throws Exception {
        long targetParent = create(null, "目标父级", "Target parent", "target-parent",
                CategoryMode.NORMAL, 0, CategoryStatus.ACTIVE);
        long sourceParent = create(null, "来源父级", "Source parent", "source-parent",
                CategoryMode.NORMAL, 1, CategoryStatus.ACTIVE);
        long child = create(sourceParent, "移动子级", "Moving child", "moving-child",
                CategoryMode.NORMAL, 0, CategoryStatus.ACTIVE);
        String lockName = "category-move-parent-race";
        jdbc.execute("DROP TRIGGER IF EXISTS category_move_gate");
        jdbc.execute("""
                CREATE TRIGGER category_move_gate BEFORE UPDATE ON product_category
                FOR EACH ROW BEGIN
                    IF OLD.id = %d THEN
                        SET @category_move_gate = GET_LOCK('category-move-parent-race', 10);
                        SET @category_move_release = RELEASE_LOCK('category-move-parent-race');
                    END IF;
                END
                """.formatted(child));

        try (Connection gate = MYSQL.createConnection("");
             ExecutorService executor = Executors.newFixedThreadPool(2)) {
            acquireLock(gate, lockName);
            Future<AdminCategoryDto> moveChild = executor.submit(() -> service.update(child,
                    request(targetParent, "移动子级", "Moving child", "moving-child",
                            CategoryMode.NORMAL, 0, CategoryStatus.ACTIVE)));
            assertBlocked(moveChild);

            Future<Void> deleteTarget = executor.submit(() -> {
                service.softDelete(targetParent);
                return null;
            });
            assertBlocked(deleteTarget);

            releaseLock(gate, lockName);
            assertThat(moveChild.get(5, TimeUnit.SECONDS).parentId()).isEqualTo(targetParent);
            assertBusinessFailure(deleteTarget, 21002);
            assertThat(service.getAdminTree()).filteredOn(root -> root.id() == targetParent)
                    .singleElement().satisfies(root -> assertThat(root.children())
                            .extracting(AdminCategoryDto::id).containsExactly(child));
        } finally {
            jdbc.execute("DROP TRIGGER IF EXISTS category_move_gate");
        }
    }

    @Test
    void authenticatedAdminCanReadUpdateSortAndDeleteThroughTheContractRoutes() throws Exception {
        long root = create(null, "根", "Root", "root", CategoryMode.NORMAL, 5, CategoryStatus.ACTIVE);
        long child = create(root, "子", "Child", "child", CategoryMode.NORMAL, 5, CategoryStatus.ACTIVE);

        mvc.perform(get("/api/v1/admin/categories/tree").with(user("synthetic-admin")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].nameZh").value("根"))
                .andExpect(jsonPath("$.data[0].children[0].nameEn").value("Child"));
        mvc.perform(put("/api/v1/admin/categories/{id}", child)
                        .with(user("synthetic-admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(null, "更新子项", "Updated child", "updated-child")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.slug").value("updated-child"));
        assertThat(service.getAdminTree()).hasSize(2);
        mvc.perform(put("/api/v1/admin/categories/sort")
                        .with(user("synthetic-admin"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"id\":%d,\"parentId\":null,\"sortOrder\":1}]".formatted(child)))
                .andExpect(status().isOk());
        assertThat(service.getAdminTree()).extracting(AdminCategoryDto::id).containsExactly(child, root);
        mvc.perform(delete("/api/v1/admin/categories/{id}", child).with(user("synthetic-admin")))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/v1/admin/categories/{id}", root).with(user("synthetic-admin")))
                .andExpect(status().isOk());
        assertThat(service.getAdminTree()).isEmpty();
    }

    private long create(Long parentId, String nameZh, String nameEn, String slug,
                        CategoryMode mode, int sortOrder, CategoryStatus status) {
        return service.create(request(parentId, nameZh, nameEn, slug, mode, sortOrder, status)).id();
    }

    private static AdminCategoryWriteRequest request(Long parentId, String nameZh, String nameEn,
                                                      String slug, CategoryMode mode, int sortOrder,
                                                      CategoryStatus status) {
        return new AdminCategoryWriteRequest(parentId, nameZh, nameEn, slug, null,
                "中文说明", "English description", mode, sortOrder, status, false,
                "中文 SEO", "English SEO", "中文 SEO 描述", "English SEO description");
    }

    private static String json(Long parentId, String nameZh, String nameEn, String slug) {
        String parent = parentId == null ? "null" : parentId.toString();
        return """
                {"parentId":%s,"nameZh":"%s","nameEn":"%s","slug":"%s",
                 "categoryMode":"NORMAL","sortOrder":0,"status":"ACTIVE","showOnHome":false}
                """.formatted(parent, nameZh, nameEn, slug);
    }

    private static void acquireLock(Connection connection, String name) throws SQLException {
        assertThat(namedLock(connection, "SELECT GET_LOCK(?, 2)", name)).isEqualTo(1);
    }

    private static void releaseLock(Connection connection, String name) throws SQLException {
        assertThat(namedLock(connection, "SELECT RELEASE_LOCK(?)", name)).isEqualTo(1);
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

    private static void assertBlocked(Future<?> future) {
        assertThatThrownBy(() -> future.get(750, TimeUnit.MILLISECONDS))
                .isInstanceOf(TimeoutException.class);
    }

    private static void assertBusinessFailure(Future<?> future, int code) {
        assertThatThrownBy(() -> future.get(5, TimeUnit.SECONDS))
                .isInstanceOf(ExecutionException.class)
                .cause().isInstanceOf(CategoryBusinessException.class)
                .extracting("code").isEqualTo(code);
    }
}
