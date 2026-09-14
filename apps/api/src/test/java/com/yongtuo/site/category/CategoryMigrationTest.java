package com.yongtuo.site.category;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
class CategoryMigrationTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.45");

    @BeforeAll
    static void migrate() {
        assertThat(Flyway.configure()
                .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                .target("2")
                .load().migrate().migrationsExecuted).isEqualTo(2);
    }

    @Test
    void definesEveryColumnWithExpectedTypeNullabilityDefaultsAndGeneration() throws Exception {
        try (Connection connection = MYSQL.createConnection("")) {
            Map<String, Column> columns = columns(connection);
            assertThat(columns.keySet()).containsExactly(
                    "id", "parent_id", "name_zh", "name_en", "slug", "cover_image",
                    "description_zh", "description_en", "category_mode", "sort_order", "status",
                    "show_on_home", "seo_title_zh", "seo_title_en", "seo_description_zh",
                    "seo_description_en", "created_at", "updated_at", "deleted_at", "active_slug");
            assertColumn(columns, "id", "bigint", "NO", null, null);
            assertThat(columns.get("id").extra()).containsIgnoringCase("auto_increment");
            assertColumn(columns, "parent_id", "bigint", "YES", null, null);
            assertVarchar(columns, "name_zh", 200, "NO");
            assertVarchar(columns, "name_en", 200, "YES");
            assertVarchar(columns, "slug", 191, "NO");
            assertVarchar(columns, "cover_image", 1024, "YES");
            assertColumn(columns, "description_zh", "text", "YES", null, 65535L);
            assertColumn(columns, "description_en", "text", "YES", null, 65535L);
            assertColumn(columns, "category_mode", "varchar", "NO", "NORMAL", 16L);
            assertThat(unquoted(columns.get("category_mode").defaultValue())).isEqualTo("NORMAL");
            assertColumn(columns, "sort_order", "int", "NO", "0", null);
            assertColumn(columns, "status", "varchar", "NO", "ACTIVE", 16L);
            assertThat(unquoted(columns.get("status").defaultValue())).isEqualTo("ACTIVE");
            assertColumn(columns, "show_on_home", "tinyint", "NO", "0", null);
            assertVarchar(columns, "seo_title_zh", 255, "YES");
            assertVarchar(columns, "seo_title_en", 255, "YES");
            assertVarchar(columns, "seo_description_zh", 500, "YES");
            assertVarchar(columns, "seo_description_en", 500, "YES");
            assertColumn(columns, "created_at", "datetime", "NO", "CURRENT_TIMESTAMP", null);
            assertColumn(columns, "updated_at", "datetime", "NO", "CURRENT_TIMESTAMP", null);
            assertThat(columns.get("updated_at").extra()).containsIgnoringCase("on update current_timestamp");
            assertColumn(columns, "deleted_at", "datetime", "YES", null, null);
            assertVarchar(columns, "active_slug", 191, "YES");
            assertThat(columns.get("active_slug").extra()).containsIgnoringCase("stored generated");
            assertThat(columns.get("active_slug").generationExpression())
                    .containsIgnoringCase("deleted_at").containsIgnoringCase("slug");
        }
    }

    @Test
    void enforcesChecksSelfReferenceRestrictAndRequiredIndexes() throws Exception {
        try (Connection connection = MYSQL.createConnection("")) {
            assertThat(checkClauses(connection))
                    .anySatisfy(clause -> assertThat(clause).contains("NORMAL", "SHOWCASE"))
                    .anySatisfy(clause -> assertThat(clause).contains("ACTIVE", "INACTIVE"));
            assertThat(foreignKey(connection)).isEqualTo(
                    new ForeignKey("parent_id", "product_category", "id", "RESTRICT", "RESTRICT"));
            Map<String, Index> indexes = indexes(connection);
            assertThat(indexes).containsEntry("uk_product_category_active_slug",
                    new Index(false, List.of("active_slug")));
            assertThat(indexes).containsEntry("idx_product_category_parent_sort",
                    new Index(true, List.of("parent_id", "sort_order", "id")));
            assertThat(indexes).containsEntry("idx_product_category_status_deleted",
                    new Index(true, List.of("status", "deleted_at")));

            assertThatThrownBy(() -> insert(connection, null, "invalid-mode", "BROKEN", "ACTIVE"))
                    .isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> insert(connection, null, "invalid-status", "NORMAL", "BROKEN"))
                    .isInstanceOf(SQLException.class);
            long parent = insert(connection, null, "hard-delete-parent", "NORMAL", "ACTIVE");
            insert(connection, parent, "hard-delete-child", "NORMAL", "ACTIVE");
            assertThatThrownBy(() -> execute(connection,
                    "DELETE FROM product_category WHERE id = " + parent)).isInstanceOf(SQLException.class);
        }
    }

    @Test
    void activeSlugIsUniqueAndReusableOnlyAfterSoftDeletion() throws Exception {
        try (Connection connection = MYSQL.createConnection("")) {
            long first = insert(connection, null, "unique-live-slug", "NORMAL", "ACTIVE");
            assertThatThrownBy(() -> insert(connection, null, "unique-live-slug", "NORMAL", "INACTIVE"))
                    .isInstanceOf(SQLException.class);
            execute(connection, "UPDATE product_category SET deleted_at = CURRENT_TIMESTAMP WHERE id = " + first);
            assertThat(insert(connection, null, "unique-live-slug", "SHOWCASE", "ACTIVE")).isPositive();
        }
    }

    private static Map<String, Column> columns(Connection connection) throws SQLException {
        Map<String, Column> columns = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT, EXTRA,
                       CHARACTER_MAXIMUM_LENGTH, GENERATION_EXPRESSION
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_category'
                ORDER BY ORDINAL_POSITION
                """); ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                columns.put(result.getString(1), new Column(result.getString(2), result.getString(3),
                        result.getString(4), result.getString(5), nullableLong(result, 6), result.getString(7)));
            }
        }
        return columns;
    }

    private static void assertVarchar(Map<String, Column> columns, String name, long length,
                                      String nullable) {
        assertColumn(columns, name, "varchar", nullable, null, length);
    }

    private static void assertColumn(Map<String, Column> columns, String name, String type,
                                     String nullable, String defaultValue, Long length) {
        assertThat(columns).containsKey(name);
        Column column = columns.get(name);
        assertThat(column.dataType()).isEqualTo(type);
        assertThat(column.nullable()).isEqualTo(nullable);
        assertThat(column.defaultValue()).isEqualTo(defaultValue);
        assertThat(column.length()).isEqualTo(length);
    }

    private static List<String> checkClauses(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT cc.CHECK_CLAUSE
                FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
                JOIN INFORMATION_SCHEMA.CHECK_CONSTRAINTS cc
                  ON cc.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA
                 AND cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME
                WHERE tc.TABLE_SCHEMA = DATABASE() AND tc.TABLE_NAME = 'product_category'
                  AND tc.CONSTRAINT_TYPE = 'CHECK'
                ORDER BY tc.CONSTRAINT_NAME
                """); ResultSet result = statement.executeQuery()) {
            java.util.ArrayList<String> clauses = new java.util.ArrayList<>();
            while (result.next()) clauses.add(result.getString(1));
            return clauses;
        }
    }

    private static ForeignKey foreignKey(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT kcu.COLUMN_NAME, kcu.REFERENCED_TABLE_NAME, kcu.REFERENCED_COLUMN_NAME,
                       rc.DELETE_RULE, rc.UPDATE_RULE
                FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu
                JOIN INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS rc
                  ON rc.CONSTRAINT_SCHEMA = kcu.CONSTRAINT_SCHEMA
                 AND rc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME
                WHERE kcu.TABLE_SCHEMA = DATABASE() AND kcu.TABLE_NAME = 'product_category'
                  AND kcu.REFERENCED_TABLE_NAME IS NOT NULL
                """); ResultSet result = statement.executeQuery()) {
            assertThat(result.next()).isTrue();
            return new ForeignKey(result.getString(1), result.getString(2), result.getString(3),
                    result.getString(4), result.getString(5));
        }
    }

    private static Map<String, Index> indexes(Connection connection) throws SQLException {
        Map<String, Boolean> nonUnique = new LinkedHashMap<>();
        Map<String, List<String>> columns = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT INDEX_NAME, NON_UNIQUE, COLUMN_NAME
                FROM INFORMATION_SCHEMA.STATISTICS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product_category'
                ORDER BY INDEX_NAME, SEQ_IN_INDEX
                """); ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                nonUnique.put(result.getString(1), result.getBoolean(2));
                columns.computeIfAbsent(result.getString(1), ignored -> new java.util.ArrayList<>())
                        .add(result.getString(3));
            }
        }
        Map<String, Index> indexes = new LinkedHashMap<>();
        columns.forEach((name, value) -> indexes.put(name, new Index(nonUnique.get(name), List.copyOf(value))));
        return indexes;
    }

    private static long insert(Connection connection, Long parentId, String slug,
                               String mode, String status) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO product_category (parent_id, name_zh, slug, category_mode, status)
                VALUES (?, '合成迁移测试分类', ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            if (parentId == null) statement.setNull(1, java.sql.Types.BIGINT);
            else statement.setLong(1, parentId);
            statement.setString(2, slug);
            statement.setString(3, mode);
            statement.setString(4, status);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                assertThat(keys.next()).isTrue();
                return keys.getLong(1);
            }
        }
    }

    private static void execute(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static Long nullableLong(ResultSet result, int column) throws SQLException {
        long value = result.getLong(column);
        return result.wasNull() ? null : value;
    }

    private static String unquoted(String value) {
        return value == null ? null : value.replace("'", "");
    }

    private record Column(String dataType, String nullable, String defaultValue, String extra,
                          Long length, String generationExpression) {}
    private record ForeignKey(String column, String table, String referencedColumn,
                              String deleteRule, String updateRule) {}
    private record Index(boolean nonUnique, List<String> columns) {}
}
