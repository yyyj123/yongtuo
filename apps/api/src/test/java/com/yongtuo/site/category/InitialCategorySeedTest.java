package com.yongtuo.site.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
class InitialCategorySeedTest {

    private static final List<String> HARDWARE_NAMES = List.of(
            "石膏板膨胀螺丝", "插销", "螺钉", "牙条", "垫圈", "螺母", "铆钉", "双头螺柱",
            "自攻螺丝", "螺栓", "混凝土锚钉", "锚");

    private static final List<String> MACHINED_PART_NAMES = List.of("顶针", "法兰盘", "螺丝扣");

    private static final List<String> EXPECTED_SLUGS = List.of(
            "wujin-chanpin", "jixie-jiagongjian", "shigao-ban-pengzhang-luosi", "chaxiao",
            "luoding", "yatia", "dianquan", "luomu", "maoding", "shuangtou-luozhu",
            "zigong-luosi", "luoshuan", "hunningtu-maoding", "mao", "dingzhen", "falanpan",
            "luosikou");

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.45");

    @Container
    static final MySQLContainer PARTIAL_MYSQL = new MySQLContainer("mysql:8.0.45");

    @Container
    static final MySQLContainer CONFLICT_MYSQL = new MySQLContainer("mysql:8.0.45");

    @Test
    void migratesEmptySchemaAndSeedsDeterministicCategoryTree() throws SQLException {
        try (Connection connection = MYSQL.createConnection("")) {
            assertThat(baseTables(connection)).isEmpty();

            MigrateResult result = Flyway.configure()
                    .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                    .load()
                    .migrate();

            assertThat(result.success).isTrue();
            assertThat(result.migrationsExecuted).isGreaterThanOrEqualTo(3);
            assertThat(appliedVersions(connection)).contains("1", "2", "10");

            Map<Long, CategoryRow> categories = categories(connection);
            assertThat(categories).hasSize(17);
            assertThat(categories.values().stream().map(CategoryRow::slug).distinct().toList())
                    .containsExactlyInAnyOrderElementsOf(EXPECTED_SLUGS);
            assertThat(categories.values().stream().map(CategoryRow::nameEn).toList())
                    .allMatch(name -> name == null);

            List<CategoryRow> roots = categories.values().stream()
                    .filter(category -> category.parentId() == null)
                    .toList();
            assertThat(roots).extracting(CategoryRow::nameZh)
                    .containsExactly("五金产品", "机械加工件");
            assertThat(roots).extracting(CategoryRow::slug)
                    .containsExactly("wujin-chanpin", "jixie-jiagongjian");
            assertThat(roots).extracting(CategoryRow::categoryMode)
                    .containsExactly("NORMAL", "NORMAL");
            assertThat(roots).extracting(CategoryRow::sortOrder).containsExactly(10, 20);
            assertThat(roots).allMatch(category -> category.status().equals("ACTIVE"));
            assertThat(roots).allMatch(category -> category.showOnHome() == 0);

            long hardwareRootId = roots.get(0).id();
            long machinedPartRootId = roots.get(1).id();
            assertThat(childrenOf(categories, hardwareRootId)).extracting(CategoryRow::nameZh)
                    .containsExactlyElementsOf(HARDWARE_NAMES);
            assertThat(childrenOf(categories, hardwareRootId)).extracting(CategoryRow::sortOrder)
                    .containsExactly(10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120);
            assertThat(childrenOf(categories, machinedPartRootId)).extracting(CategoryRow::nameZh)
                    .containsExactlyElementsOf(MACHINED_PART_NAMES);
            assertThat(childrenOf(categories, machinedPartRootId)).extracting(CategoryRow::sortOrder)
                    .containsExactly(10, 20, 30);
            assertThat(categories.values().stream()
                    .filter(category -> category.parentId() != null)
                    .allMatch(category -> category.categoryMode().equals("NORMAL")))
                    .isTrue();
            assertThat(categories.values().stream()
                    .filter(category -> category.parentId() != null)
                    .allMatch(category -> category.status().equals("ACTIVE") && category.showOnHome() == 0))
                    .isTrue();
            assertThat(categories.values().stream().map(CategoryRow::nameZh))
                    .doesNotContain("CNC 定制件", "非标件", "来图加工", "来样加工");
        }
    }

    @Test
    void completesPartialPreseedWithoutOverwritingExistingCategoryContent() throws SQLException {
        String jdbcUrl = PARTIAL_MYSQL.getJdbcUrl();
        Flyway.configure().dataSource(jdbcUrl, PARTIAL_MYSQL.getUsername(), PARTIAL_MYSQL.getPassword())
                .target("2").load().migrate();
        long rootId;
        long childId;
        try (Connection connection = PARTIAL_MYSQL.createConnection("")) {
            rootId = insert(connection, null, "五金产品", "wujin-chanpin", 10,
                    "保留的人工说明", null);
            childId = insert(connection, rootId, "石膏板膨胀螺丝", "shigao-ban-pengzhang-luosi", 10,
                    null, "人工英文草稿");
        }

        MigrateResult result = Flyway.configure()
                .dataSource(jdbcUrl, PARTIAL_MYSQL.getUsername(), PARTIAL_MYSQL.getPassword())
                .load().migrate();

        assertThat(result.success).isTrue();
        try (Connection connection = PARTIAL_MYSQL.createConnection("")) {
            assertThat(count(connection)).isEqualTo(17);
            assertThat(category(connection, rootId).descriptionZh()).isEqualTo("保留的人工说明");
            assertThat(category(connection, childId).nameEn()).isEqualTo("人工英文草稿");
            assertThat(category(connection, childId).parentId()).isEqualTo(rootId);
            assertThat(countBySlug(connection, "wujin-chanpin")).isEqualTo(1);
            assertThat(countBySlug(connection, "shigao-ban-pengzhang-luosi")).isEqualTo(1);
        }
    }

    @Test
    void rejectsConflictingActiveChildSlugAndRollsBackTheSeedMigration() throws SQLException {
        String jdbcUrl = CONFLICT_MYSQL.getJdbcUrl();
        Flyway.configure().dataSource(jdbcUrl, CONFLICT_MYSQL.getUsername(), CONFLICT_MYSQL.getPassword())
                .target("2").load().migrate();
        long manualParentId;
        long conflictingChildId;
        try (Connection connection = CONFLICT_MYSQL.createConnection("")) {
            manualParentId = insert(connection, null, "人工父分类", "manual-parent", 999,
                    "合成冲突测试父分类", null);
            conflictingChildId = insert(connection, manualParentId, "人工冲突分类",
                    "shigao-ban-pengzhang-luosi", 999, "合成冲突测试子分类", null);
        }

        assertThatThrownBy(() -> Flyway.configure()
                .dataSource(jdbcUrl, CONFLICT_MYSQL.getUsername(), CONFLICT_MYSQL.getPassword())
                .load().migrate())
                .satisfies(error -> assertThat(causeMessages(error))
                        .anyMatch(message -> message.contains("shigao-ban-pengzhang-luosi")));

        try (Connection connection = CONFLICT_MYSQL.createConnection("")) {
            assertThat(count(connection)).isEqualTo(2);
            assertThat(category(connection, manualParentId).descriptionZh())
                    .isEqualTo("合成冲突测试父分类");
            assertThat(category(connection, conflictingChildId).parentId()).isEqualTo(manualParentId);
            assertThat(countBySlug(connection, "wujin-chanpin")).isZero();
            assertThat(countBySlug(connection, "jixie-jiagongjian")).isZero();
            // V3/V4/V5/V9 are now part of the fresh-schema migration set; Flyway applies them
            // before V10, while the failed seed itself remains fully rolled back.
            assertThat(appliedVersions(connection)).containsExactly("1", "2", "3", "4", "5", "9");
        }
    }

    private static List<CategoryRow> childrenOf(Map<Long, CategoryRow> categories, long parentId) {
        return categories.values().stream()
                .filter(category -> Long.valueOf(parentId).equals(category.parentId()))
                .toList();
    }

    private static Map<Long, CategoryRow> categories(Connection connection) throws SQLException {
        Map<Long, CategoryRow> categories = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id, parent_id, name_zh, name_en, slug, category_mode, sort_order, status, show_on_home,
                       description_zh
                FROM product_category
                ORDER BY parent_id IS NOT NULL, parent_id, sort_order, id
                """); ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                categories.put(result.getLong("id"), new CategoryRow(
                        result.getLong("id"), nullableLong(result, "parent_id"), result.getString("name_zh"),
                        result.getString("name_en"), result.getString("slug"), result.getString("category_mode"),
                        result.getInt("sort_order"), result.getString("status"), result.getInt("show_on_home"),
                        result.getString("description_zh")));
            }
        }
        return categories;
    }

    private static List<String> appliedVersions(Connection connection) throws SQLException {
        List<String> versions = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT version
                FROM flyway_schema_history
                WHERE success = 1
                ORDER BY installed_rank
                """); ResultSet result = statement.executeQuery()) {
            while (result.next()) versions.add(result.getString("version"));
        }
        return versions;
    }

    private static Set<String> baseTables(Connection connection) throws SQLException {
        Set<String> tables = new TreeSet<>();
        try (ResultSet result = connection.getMetaData()
                .getTables(connection.getCatalog(), null, "%", new String[] {"TABLE"})) {
            while (result.next()) tables.add(result.getString("TABLE_NAME"));
        }
        return tables;
    }

    private static Long nullableLong(ResultSet result, String column) throws SQLException {
        long value = result.getLong(column);
        return result.wasNull() ? null : value;
    }

    private static long insert(Connection connection, Long parentId, String nameZh, String slug,
                               int sortOrder, String descriptionZh, String nameEn) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO product_category (parent_id, name_zh, name_en, slug, description_zh, sort_order)
                VALUES (?, ?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            if (parentId == null) statement.setNull(1, java.sql.Types.BIGINT);
            else statement.setLong(1, parentId);
            statement.setString(2, nameZh);
            statement.setString(3, nameEn);
            statement.setString(4, slug);
            statement.setString(5, descriptionZh);
            statement.setInt(6, sortOrder);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                assertThat(keys.next()).isTrue();
                return keys.getLong(1);
            }
        }
    }

    private static CategoryRow category(Connection connection, long id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id, parent_id, name_zh, name_en, slug, category_mode, sort_order, status, show_on_home,
                       description_zh
                FROM product_category WHERE id = ?
                """)) {
            statement.setLong(1, id);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                return new CategoryRow(result.getLong("id"), nullableLong(result, "parent_id"),
                        result.getString("name_zh"), result.getString("name_en"), result.getString("slug"),
                        result.getString("category_mode"), result.getInt("sort_order"),
                        result.getString("status"), result.getInt("show_on_home"),
                        result.getString("description_zh"));
            }
        }
    }

    private static int count(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM product_category");
             ResultSet result = statement.executeQuery()) {
            assertThat(result.next()).isTrue();
            return result.getInt(1);
        }
    }

    private static int countBySlug(Connection connection, String slug) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM product_category WHERE slug = ? AND deleted_at IS NULL")) {
            statement.setString(1, slug);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                return result.getInt(1);
            }
        }
    }

    private static List<String> causeMessages(Throwable error) {
        List<String> messages = new ArrayList<>();
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current.getMessage() != null) messages.add(current.getMessage());
        }
        return messages;
    }

    private record CategoryRow(
            long id,
            Long parentId,
            String nameZh,
            String nameEn,
            String slug,
            String categoryMode,
            int sortOrder,
            String status,
            int showOnHome,
            String descriptionZh) {}
}
