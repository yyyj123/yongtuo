package com.yongtuo.site.db;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
class FlywayMigrationTest {

    private static final Set<String> ADMIN_TABLES =
            Set.of("admin_user", "admin_login_log", "admin_operation_log");

    @Container
    private static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.45");

    @Container
    private static final MySQLContainer PHASE1_UPGRADE_MYSQL = new MySQLContainer("mysql:8.0.45");

    @Container
    private static final MySQLContainer ATTRIBUTE_MYSQL = new MySQLContainer("mysql:8.0.45");

    @Test
    void migratesEmptySchemaToAdminAuditBaseline() throws SQLException {
        try (Connection connection = MYSQL.createConnection("")) {
            assertThat(baseTables(connection)).isEmpty();

            MigrateResult result = Flyway.configure()
                    .dataSource(MYSQL.getJdbcUrl(), MYSQL.getUsername(), MYSQL.getPassword())
                    .target("1")
                    .load()
                    .migrate();

            assertThat(result.success).isTrue();
            assertThat(result.migrationsExecuted).isEqualTo(1);
            assertThat(businessTables(connection)).isEqualTo(ADMIN_TABLES);
            assertPrimaryKeys(connection);
            assertAdminUserColumns(connection);
            assertAdminLoginLogColumns(connection);
            assertAdminOperationLogColumns(connection);
            assertForeignKeys(connection);
            assertIndexes(connection);
            assertTableProperties(connection);
        }
    }

    @Test
    void upgradesDisposablePhase1V1SchemaThroughCompletePhase2Set() throws SQLException {
        String jdbcUrl = PHASE1_UPGRADE_MYSQL.getJdbcUrl();
        Flyway.configure().dataSource(jdbcUrl, PHASE1_UPGRADE_MYSQL.getUsername(), PHASE1_UPGRADE_MYSQL.getPassword())
                .target("1").load().migrate();

        MigrateResult result = Flyway.configure()
                .dataSource(jdbcUrl, PHASE1_UPGRADE_MYSQL.getUsername(), PHASE1_UPGRADE_MYSQL.getPassword())
                .load().migrate();

        assertThat(result.success).isTrue();
        try (Connection connection = PHASE1_UPGRADE_MYSQL.createConnection("")) {
            assertThat(appliedVersions(connection)).containsExactly(
                    "1", "2", "3", "4", "5", "9", "10", "12", "13", "14", "15", "16", "17", "18", "19", "20", "21");
            var sessions = columns(connection, "admin_refresh_session");
            assertThat(sessions.keySet()).containsExactlyInAnyOrder("session_id", "admin_user_id",
                    "token_hash", "token_version", "expires_at", "revoked_at",
                    "replacement_session_id", "created_at");
            assertThat(sessions.get("token_hash").columnType()).isEqualTo("char(64)");
            assertThat(sessions.get("token_hash").nullable()).isEqualTo("NO");
            assertThat(sessions.get("expires_at").nullable()).isEqualTo("NO");
            assertThat(sessions.get("revoked_at").nullable()).isEqualTo("YES");
            assertThat(uniqueIndexes(connection, "admin_refresh_session"))
                    .containsValue(List.of("token_hash"));
            assertThat(foreignKeys(connection, "admin_refresh_session"))
                    .contains(new ForeignKey("admin_user_id", "admin_user", "id",
                            DatabaseMetaData.importedKeyCascade, DatabaseMetaData.importedKeyRestrict));
            assertThat(foreignKeys(connection, "product_variant_value"))
                    .contains(new ForeignKey("attribute_id", "attribute_definition", "id",
                            DatabaseMetaData.importedKeyRestrict, DatabaseMetaData.importedKeyRestrict))
                    .contains(new ForeignKey("option_id", "attribute_option", "id",
                            DatabaseMetaData.importedKeyRestrict, DatabaseMetaData.importedKeyRestrict));
            assertThat(compositeForeignKeyColumns(connection, "product_variant_value", "attribute_option"))
                    .contains(List.of("option_id", "attribute_id"));
        }
    }

    @Test
    void attributeSchemaHasTypedFieldsChecksIndexesAndRestrictiveForeignKeys() throws SQLException {
        try (Connection connection = ATTRIBUTE_MYSQL.createConnection("")) {
            Flyway.configure().dataSource(ATTRIBUTE_MYSQL.getJdbcUrl(), ATTRIBUTE_MYSQL.getUsername(), ATTRIBUTE_MYSQL.getPassword())
                    .load().migrate();
            Map<String, ColumnDefinition> definitions = columns(connection, "attribute_definition");
            assertThat(definitions.keySet()).containsExactlyInAnyOrder("id", "name_zh", "name_en", "code",
                    "data_type", "unit", "is_global", "default_filterable", "default_required", "sort_order", "status");
            assertExactColumns(definitions, Map.ofEntries(
                    Map.entry("id", column("bigint", "bigint", "NO", null, "auto_increment")),
                    Map.entry("name_zh", column("varchar", "varchar(200)", "NO", null, "")),
                    Map.entry("name_en", column("varchar", "varchar(200)", "YES", null, "")),
                    Map.entry("code", column("varchar", "varchar(100)", "NO", null, "")),
                    Map.entry("data_type", column("varchar", "varchar(16)", "NO", null, "")),
                    Map.entry("unit", column("varchar", "varchar(64)", "YES", null, "")),
                    Map.entry("is_global", column("tinyint", "tinyint", "NO", "0", "")),
                    Map.entry("default_filterable", column("tinyint", "tinyint", "NO", "0", "")),
                    Map.entry("default_required", column("tinyint", "tinyint", "NO", "0", "")),
                    Map.entry("sort_order", column("int", "int", "NO", "0", "")),
                    Map.entry("status", column("varchar", "varchar(16)", "NO", "ACTIVE", ""))));
            assertVarchar(definitions, "code", 100, "NO");
            assertVarchar(definitions, "data_type", 16, "NO");
            assertColumn(definitions, "is_global", "tinyint", "NO", "0");
            assertThat(definitions.get("is_global").columnType()).isEqualTo("tinyint");
            assertThat(definitions.get("is_global").defaultValue()).isEqualTo("0");
            assertThat(definitions.get("status").defaultValue()).isEqualTo("ACTIVE");
            assertThat(uniqueIndexes(connection, "attribute_definition")).containsValue(List.of("code"));
            assertThat(checkClauses(connection, "attribute_definition")).anyMatch(clause -> clause.contains("data_type"));
            assertExactIndexes(connection, "attribute_definition", Map.of(
                    "PRIMARY", List.of("id"),
                    "uk_attribute_definition_code", List.of("code"),
                    "idx_attribute_definition_status_sort", List.of("status", "sort_order", "id")));
            assertExactChecks(connection, "attribute_definition", Set.of(
                    "data_type IN ('TEXT', 'NUMBER', 'SELECT', 'MULTI_SELECT')",
                    "is_global IN (0, 1)",
                    "default_filterable IN (0, 1)",
                    "default_required IN (0, 1)",
                    "sort_order >= 0",
                    "status IN ('ACTIVE', 'INACTIVE')"));

            Map<String, ColumnDefinition> optionColumns = columns(connection, "attribute_option");
            assertThat(optionColumns.keySet()).containsExactlyInAnyOrder("id", "attribute_id", "value_code",
                    "label_zh", "label_en", "sort_order", "status");
            assertExactColumns(optionColumns, Map.ofEntries(
                    Map.entry("id", column("bigint", "bigint", "NO", null, "auto_increment")),
                    Map.entry("attribute_id", column("bigint", "bigint", "NO", null, "")),
                    Map.entry("value_code", column("varchar", "varchar(100)", "NO", null, "")),
                    Map.entry("label_zh", column("varchar", "varchar(200)", "NO", null, "")),
                    Map.entry("label_en", column("varchar", "varchar(200)", "YES", null, "")),
                    Map.entry("sort_order", column("int", "int", "NO", "0", "")),
                    Map.entry("status", column("varchar", "varchar(16)", "NO", "ACTIVE", ""))));
            assertBigInt(optionColumns, "attribute_id", "NO");
            assertVarchar(optionColumns, "value_code", 100, "NO");
            assertVarchar(optionColumns, "label_zh", 200, "NO");
            assertVarchar(optionColumns, "label_en", 200, "YES");
            assertInt(optionColumns, "sort_order", "NO", "0");
            assertColumn(optionColumns, "status", "varchar", "NO", "ACTIVE");
            assertThat(optionColumns.get("status").characterMaximumLength()).isEqualTo(16L);
            assertThat(uniqueIndexes(connection, "attribute_option"))
                    .containsValue(List.of("attribute_id", "value_code"))
                    .containsValue(List.of("id", "attribute_id"));
            assertThat(indexColumns(connection, "attribute_option", "idx_attribute_option_attribute_sort"))
                    .containsExactly("attribute_id", "status", "sort_order", "id");
            assertThat(checkClauses(connection, "attribute_option")).anyMatch(clause -> clause.contains("status"));
            assertExactIndexes(connection, "attribute_option", Map.of(
                    "PRIMARY", List.of("id"),
                    "uk_attribute_option_attribute_value", List.of("attribute_id", "value_code"),
                    "uk_attribute_option_id_attribute", List.of("id", "attribute_id"),
                    "idx_attribute_option_attribute_sort", List.of("attribute_id", "status", "sort_order", "id")));
            assertExactChecks(connection, "attribute_option", Set.of(
                    "sort_order >= 0", "status IN ('ACTIVE', 'INACTIVE')"));

            Map<String, ColumnDefinition> categoryAttributes = columns(connection, "category_attribute");
            assertThat(categoryAttributes.keySet()).containsExactlyInAnyOrder("id", "category_id", "attribute_id",
                    "is_filterable", "is_required", "show_in_detail", "sort_order");
            assertExactColumns(categoryAttributes, Map.ofEntries(
                    Map.entry("id", column("bigint", "bigint", "NO", null, "auto_increment")),
                    Map.entry("category_id", column("bigint", "bigint", "NO", null, "")),
                    Map.entry("attribute_id", column("bigint", "bigint", "NO", null, "")),
                    Map.entry("is_filterable", column("tinyint", "tinyint", "NO", "0", "")),
                    Map.entry("is_required", column("tinyint", "tinyint", "NO", "0", "")),
                    Map.entry("show_in_detail", column("tinyint", "tinyint", "NO", "1", "")),
                    Map.entry("sort_order", column("int", "int", "NO", "0", ""))));
            assertBigInt(categoryAttributes, "category_id", "NO");
            assertBigInt(categoryAttributes, "attribute_id", "NO");
            assertColumn(categoryAttributes, "is_filterable", "tinyint", "NO", "0");
            assertColumn(categoryAttributes, "is_required", "tinyint", "NO", "0");
            assertColumn(categoryAttributes, "show_in_detail", "tinyint", "NO", "1");
            assertInt(categoryAttributes, "sort_order", "NO", "0");
            assertThat(uniqueIndexes(connection, "category_attribute"))
                    .containsValue(List.of("category_id", "attribute_id"));
            assertThat(checkClauses(connection, "category_attribute"))
                    .anyMatch(clause -> clause.contains("is_filterable"))
                    .anyMatch(clause -> clause.contains("show_in_detail"));
            assertExactIndexes(connection, "category_attribute", Map.of(
                    "PRIMARY", List.of("id"),
                    "uk_category_attribute_category_attribute", List.of("category_id", "attribute_id"),
                    "idx_category_attribute_attribute_category", List.of("attribute_id", "category_id")));
            assertExactChecks(connection, "category_attribute", Set.of(
                    "is_filterable IN (0, 1)", "is_required IN (0, 1)",
                    "show_in_detail IN (0, 1)", "sort_order >= 0"));

            Map<String, ColumnDefinition> values = columns(connection, "product_attribute_value");
            assertThat(values.keySet()).containsExactlyInAnyOrder("id", "product_id", "attribute_id", "value_zh",
                    "value_en", "numeric_value", "option_id", "sort_order", "value_key");
            assertExactColumns(values, Map.ofEntries(
                    Map.entry("id", column("bigint", "bigint", "NO", null, "auto_increment")),
                    Map.entry("product_id", column("bigint", "bigint", "NO", null, "")),
                    Map.entry("attribute_id", column("bigint", "bigint", "NO", null, "")),
                    Map.entry("value_zh", column("text", "text", "YES", null, "")),
                    Map.entry("value_en", column("text", "text", "YES", null, "")),
                    Map.entry("numeric_value", column("decimal", "decimal(20,6)", "YES", null, "")),
                    Map.entry("option_id", column("bigint", "bigint", "YES", null, "")),
                    Map.entry("sort_order", column("int", "int", "NO", "0", "")),
                    Map.entry("value_key", column("bigint", "bigint", "NO", "0", ""))));
            assertThat(values.get("option_id").nullable()).isEqualTo("YES");
            assertBigInt(values, "product_id", "NO");
            assertBigInt(values, "attribute_id", "NO");
            assertColumn(values, "value_zh", "text", "YES", null);
            assertColumn(values, "value_en", "text", "YES", null);
            assertColumn(values, "numeric_value", "decimal", "YES", null);
            assertInt(values, "sort_order", "NO", "0");
            assertColumn(values, "value_key", "bigint", "NO", "0");
            assertThat(indexColumns(connection, "product_attribute_value", "idx_product_attribute_value_attribute_numeric"))
                    .containsExactly("attribute_id", "numeric_value");
            assertExactIndexes(connection, "product_attribute_value", Map.of(
                    "PRIMARY", List.of("id"),
                    "idx_product_attribute_value_product_attribute_id", List.of("product_id", "attribute_id", "id"),
                    "uk_product_attribute_value_product_attribute_value_key", List.of("product_id", "attribute_id", "value_key"),
                    "idx_product_attribute_value_attribute_numeric", List.of("attribute_id", "numeric_value"),
                    "idx_product_attribute_value_attribute_option", List.of("attribute_id", "option_id"),
                    "fk_product_attribute_value_option_attribute", List.of("option_id", "attribute_id")));
            assertExactChecks(connection, "product_attribute_value", Set.of("sort_order >= 0", "value_key >= 0"));
            assertThat(foreignKeys(connection, "category_attribute"))
                    .contains(new ForeignKey("category_id", "product_category", "id",
                            DatabaseMetaData.importedKeyRestrict, DatabaseMetaData.importedKeyRestrict))
                    .contains(new ForeignKey("attribute_id", "attribute_definition", "id",
                            DatabaseMetaData.importedKeyRestrict, DatabaseMetaData.importedKeyRestrict));
            assertThat(foreignKeys(connection, "product_attribute_value"))
                    .contains(new ForeignKey("option_id", "attribute_option", "id",
                            DatabaseMetaData.importedKeyRestrict, DatabaseMetaData.importedKeyRestrict));
            assertThat(compositeForeignKeyColumns(connection, "product_attribute_value", "attribute_option"))
                    .contains(List.of("option_id", "attribute_id"));
            assertExactForeignKeys(connection, "attribute_option", Set.of(
                    new ForeignKeyContract("fk_attribute_option_definition", List.of("attribute_id"),
                            "attribute_definition", List.of("id"), "RESTRICT", "RESTRICT")));
            assertExactForeignKeys(connection, "category_attribute", Set.of(
                    new ForeignKeyContract("fk_category_attribute_category", List.of("category_id"),
                            "product_category", List.of("id"), "RESTRICT", "RESTRICT"),
                    new ForeignKeyContract("fk_category_attribute_definition", List.of("attribute_id"),
                            "attribute_definition", List.of("id"), "RESTRICT", "RESTRICT")));
            assertExactForeignKeys(connection, "product_attribute_value", Set.of(
                    new ForeignKeyContract("fk_product_attribute_value_product", List.of("product_id"),
                            "product", List.of("id"), "RESTRICT", "RESTRICT"),
                    new ForeignKeyContract("fk_product_attribute_value_attribute", List.of("attribute_id"),
                            "attribute_definition", List.of("id"), "RESTRICT", "RESTRICT"),
                    new ForeignKeyContract("fk_product_attribute_value_option_attribute",
                            List.of("option_id", "attribute_id"), "attribute_option", List.of("id", "attribute_id"),
                            "RESTRICT", "RESTRICT")));
            assertExactForeignKeys(connection, "product_variant_value", Set.of(
                    new ForeignKeyContract("fk_product_variant_value_variant", List.of("variant_id"),
                            "product_variant", List.of("id"), "CASCADE", "RESTRICT"),
                    new ForeignKeyContract("fk_product_variant_value_attribute", List.of("attribute_id"),
                            "attribute_definition", List.of("id"), "RESTRICT", "RESTRICT"),
                    new ForeignKeyContract("fk_product_variant_value_option_attribute",
                            List.of("option_id", "attribute_id"), "attribute_option", List.of("id", "attribute_id"),
                            "RESTRICT", "RESTRICT")));
            Map<String, ColumnDefinition> variantValues = columns(connection, "product_variant_value");
            assertThat(variantValues.keySet()).contains("value_key");
            assertColumn(variantValues, "value_key", "bigint", "NO", "0");
            assertExactIndexes(connection, "product_variant_value", Map.of(
                    "PRIMARY", List.of("id"),
                    "idx_product_variant_value_variant_attribute_id", List.of("variant_id", "attribute_id", "id"),
                    "uk_product_variant_value_variant_attribute_value_key", List.of("variant_id", "attribute_id", "value_key"),
                    "idx_product_variant_value_attribute", List.of("attribute_id"),
                    "fk_product_variant_value_option_attribute", List.of("option_id", "attribute_id")));
            assertExactChecks(connection, "product_variant_value", Set.of("value_key >= 0"));

            long categoryId;
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT id FROM product_category WHERE deleted_at IS NULL ORDER BY id LIMIT 1");
                 ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                categoryId = result.getLong(1);
            }
            long attributeId;
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO attribute_definition(name_zh, code, data_type)
                    VALUES ('迁移属性', 'migration_attribute', 'SELECT')
                    """, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    assertThat(keys.next()).isTrue();
                    attributeId = keys.getLong(1);
                }
            }
            long secondAttributeId;
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO attribute_definition(name_zh, code, data_type)
                    VALUES ('迁移属性二', 'migration_attribute_two', 'SELECT')
                    """, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    assertThat(keys.next()).isTrue();
                    secondAttributeId = keys.getLong(1);
                }
            }
            long secondOptionId;
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO attribute_option(attribute_id, value_code, label_zh)
                    VALUES (?, 'second', '第二选项')
                    """, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                statement.setLong(1, secondAttributeId);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    assertThat(keys.next()).isTrue();
                    secondOptionId = keys.getLong(1);
                }
            }
            long productId;
            long variantId;
            try (PreparedStatement product = connection.prepareStatement("""
                    INSERT INTO product(category_id, product_code, name_zh, slug)
                    VALUES (?, 'YT-MIGRATION-V12', '迁移产品', 'migration-v12')
                    """, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                product.setLong(1, categoryId);
                product.executeUpdate();
                try (ResultSet productKeys = product.getGeneratedKeys()) {
                    assertThat(productKeys.next()).isTrue();
                    productId = productKeys.getLong(1);
                    try (PreparedStatement variant = connection.prepareStatement("""
                            INSERT INTO product_variant(product_id, variant_code, name_zh)
                            VALUES (?, 'default', '默认规格')
                            """, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                        variant.setLong(1, productId);
                        variant.executeUpdate();
                        try (ResultSet variantKeys = variant.getGeneratedKeys()) {
                            assertThat(variantKeys.next()).isTrue();
                            variantId = variantKeys.getLong(1);
                        }
                    }
                }
            }
            long finalVariantId = variantId;
            assertThatThrownBy(() -> insertVariantValue(connection, finalVariantId, -1, null))
                    .isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> insertVariantValue(connection, finalVariantId, attributeId, -1L))
                    .isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> insertVariantValue(connection, finalVariantId, attributeId, secondOptionId))
                    .isInstanceOf(SQLException.class);
            long finalProductId = productId;
            assertThatThrownBy(() -> insertProductValue(connection, finalProductId, attributeId, secondOptionId))
                    .isInstanceOf(SQLException.class);
            long optionId;
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO attribute_option(attribute_id, value_code, label_zh) VALUES (?, 'first', '第一选项')
                    """, java.sql.Statement.RETURN_GENERATED_KEYS)) {
                statement.setLong(1, attributeId);
                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    assertThat(keys.next()).isTrue();
                    optionId = keys.getLong(1);
                }
            }
            insertCategoryAttribute(connection, categoryId, attributeId);
            insertVariantValue(connection, finalVariantId, attributeId, null);
            assertThatThrownBy(() -> insertVariantValue(connection, finalVariantId, attributeId, null))
                    .isInstanceOf(SQLException.class);
            insertProductValue(connection, finalProductId, attributeId, optionId);
            assertThatThrownBy(() -> insertProductValue(connection, finalProductId, attributeId, optionId))
                    .isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> execute(connection, """
                    INSERT INTO product_attribute_value(product_id, attribute_id, value_key)
                    VALUES (?, ?, -1)
                    """, finalProductId, secondAttributeId)).isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> execute(connection, """
                    INSERT INTO product_variant_value(variant_id, attribute_id, value_key)
                    VALUES (?, ?, -1)
                    """, finalVariantId, secondAttributeId)).isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> execute(connection,
                    "DELETE FROM attribute_definition WHERE id = ?", attributeId)).isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> execute(connection,
                    "UPDATE attribute_definition SET id = ? WHERE id = ?", attributeId + 1000, attributeId))
                    .isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> execute(connection,
                    "DELETE FROM attribute_option WHERE id = ?", optionId)).isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> execute(connection,
                    "UPDATE attribute_option SET id = ? WHERE id = ?", optionId + 1000, optionId))
                    .isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> execute(connection,
                    "DELETE FROM product_category WHERE id = ?", categoryId)).isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> execute(connection,
                    "UPDATE product_category SET id = ? WHERE id = ?", categoryId + 1000, categoryId))
                    .isInstanceOf(SQLException.class);
        }
    }

    private static void insertCategoryAttribute(Connection connection, long categoryId, long attributeId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO category_attribute(category_id, attribute_id) VALUES (?, ?)
                """)) {
            statement.setLong(1, categoryId);
            statement.setLong(2, attributeId);
            statement.executeUpdate();
        }
    }

    private static void execute(Connection connection, String sql, long... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) statement.setLong(i + 1, parameters[i]);
            statement.executeUpdate();
        }
    }

    private static void insertVariantValue(Connection connection, long variantId,
                                           long attributeId, Long optionId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO product_variant_value(variant_id, attribute_id, option_id)
                VALUES (?, ?, ?)
                """)) {
            statement.setLong(1, variantId);
            statement.setLong(2, attributeId);
            if (optionId == null) statement.setNull(3, java.sql.Types.BIGINT);
            else statement.setLong(3, optionId);
            statement.executeUpdate();
        }
    }

    private static void insertProductValue(Connection connection, long productId,
                                           long attributeId, long optionId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO product_attribute_value(product_id, attribute_id, option_id)
                VALUES (?, ?, ?)
                """)) {
            statement.setLong(1, productId);
            statement.setLong(2, attributeId);
            statement.setLong(3, optionId);
            statement.executeUpdate();
        }
    }

    private static void assertPrimaryKeys(Connection connection) throws SQLException {
        assertThat(primaryKeys(connection, "admin_user")).containsExactly("id");
        assertThat(primaryKeys(connection, "admin_login_log")).containsExactly("id");
        assertThat(primaryKeys(connection, "admin_operation_log")).containsExactly("id");
    }

    private static void assertAdminUserColumns(Connection connection) throws SQLException {
        Map<String, ColumnDefinition> columns = columns(connection, "admin_user");
        assertThat(columns.keySet())
                .containsExactlyInAnyOrder(
                        "id",
                        "username",
                        "password_hash",
                        "token_version",
                        "last_login_at",
                        "created_at",
                        "updated_at");
        assertAutoIncrementBigInt(columns, "id");
        assertVarchar(columns, "username", 64, "NO");
        assertVarchar(columns, "password_hash", 255, "NO");
        assertInt(columns, "token_version", "NO", "0");
        assertDateTime(columns, "last_login_at", "YES", null);
        assertDateTime(columns, "created_at", "NO", "CURRENT_TIMESTAMP");
        assertDateTime(columns, "updated_at", "NO", "CURRENT_TIMESTAMP");
        assertThat(columns.get("updated_at").extra()).containsIgnoringCase("on update current_timestamp");
    }

    private static void assertAdminLoginLogColumns(Connection connection) throws SQLException {
        Map<String, ColumnDefinition> columns = columns(connection, "admin_login_log");
        assertThat(columns.keySet())
                .containsExactlyInAnyOrder(
                        "id",
                        "admin_user_id",
                        "username",
                        "login_success",
                        "ip_address",
                        "user_agent",
                        "failure_reason",
                        "created_at");
        assertAutoIncrementBigInt(columns, "id");
        assertBigInt(columns, "admin_user_id", "YES");
        assertVarchar(columns, "username", 100, "NO");
        assertTinyInt(columns, "login_success", "NO");
        assertVarchar(columns, "ip_address", 45, "YES");
        assertVarchar(columns, "user_agent", 512, "YES");
        assertVarchar(columns, "failure_reason", 255, "YES");
        assertDateTime(columns, "created_at", "NO", "CURRENT_TIMESTAMP");
    }

    private static void assertAdminOperationLogColumns(Connection connection) throws SQLException {
        Map<String, ColumnDefinition> columns = columns(connection, "admin_operation_log");
        assertThat(columns.keySet())
                .containsExactlyInAnyOrder(
                        "id",
                        "admin_user_id",
                        "operation",
                        "resource_type",
                        "resource_id",
                        "detail",
                        "ip_address",
                        "created_at");
        assertAutoIncrementBigInt(columns, "id");
        assertBigInt(columns, "admin_user_id", "YES");
        assertVarchar(columns, "operation", 100, "NO");
        assertVarchar(columns, "resource_type", 100, "NO");
        assertBigInt(columns, "resource_id", "YES");
        assertColumn(columns, "detail", "json", "YES", null);
        assertVarchar(columns, "ip_address", 45, "YES");
        assertDateTime(columns, "created_at", "NO", "CURRENT_TIMESTAMP");
    }

    private static void assertForeignKeys(Connection connection) throws SQLException {
        ForeignKey expected = new ForeignKey(
                "admin_user_id",
                "admin_user",
                "id",
                DatabaseMetaData.importedKeySetNull,
                DatabaseMetaData.importedKeyRestrict);
        assertThat(foreignKeys(connection, "admin_login_log")).containsExactly(expected);
        assertThat(foreignKeys(connection, "admin_operation_log")).containsExactly(expected);
    }

    private static void assertIndexes(Connection connection) throws SQLException {
        assertThat(uniqueIndexes(connection, "admin_user")).containsValue(List.of("username"));
        assertThat(indexes(connection, "admin_login_log"))
                .containsValue(List.of("username", "created_at"))
                .containsValue(List.of("ip_address", "created_at"));
        assertThat(indexes(connection, "admin_operation_log"))
                .containsValue(List.of("admin_user_id", "created_at"));
    }

    private static void assertColumn(
            Map<String, ColumnDefinition> columns,
            String name,
            String dataType,
            String nullable,
            String defaultValue) {
        assertThat(columns).containsKey(name);
        assertThat(columns.get(name).dataType()).isEqualTo(dataType);
        assertThat(columns.get(name).nullable()).isEqualTo(nullable);
        assertThat(columns.get(name).defaultValue()).isEqualTo(defaultValue);
    }

    private static void assertAutoIncrementBigInt(
            Map<String, ColumnDefinition> columns, String name) {
        assertBigInt(columns, name, "NO");
        assertThat(columns.get(name).extra()).containsIgnoringCase("auto_increment");
    }

    private static void assertBigInt(
            Map<String, ColumnDefinition> columns, String name, String nullable) {
        assertColumn(columns, name, "bigint", nullable, null);
        assertThat(columns.get(name).columnType()).isEqualTo("bigint");
        assertThat(columns.get(name).numericPrecision()).isEqualTo(19L);
    }

    private static void assertVarchar(
            Map<String, ColumnDefinition> columns, String name, long length, String nullable) {
        assertColumn(columns, name, "varchar", nullable, null);
        assertThat(columns.get(name).characterMaximumLength()).isEqualTo(length);
    }

    private static void assertInt(
            Map<String, ColumnDefinition> columns,
            String name,
            String nullable,
            String defaultValue) {
        assertColumn(columns, name, "int", nullable, defaultValue);
        assertThat(columns.get(name).columnType()).isEqualTo("int");
        assertThat(columns.get(name).numericPrecision()).isEqualTo(10L);
    }

    private static void assertTinyInt(
            Map<String, ColumnDefinition> columns, String name, String nullable) {
        assertColumn(columns, name, "tinyint", nullable, null);
        assertThat(columns.get(name).columnType()).isEqualTo("tinyint");
        assertThat(columns.get(name).numericPrecision()).isEqualTo(3L);
    }

    private static void assertDateTime(
            Map<String, ColumnDefinition> columns,
            String name,
            String nullable,
            String defaultValue) {
        assertColumn(columns, name, "datetime", nullable, defaultValue);
        assertThat(columns.get(name).datetimePrecision()).isZero();
    }

    private static void assertTableProperties(Connection connection) throws SQLException {
        for (String table : ADMIN_TABLES) {
            TableDefinition definition = tableDefinition(connection, table);
            assertThat(definition.engine()).isEqualToIgnoringCase("InnoDB");
            assertThat(definition.tableCollation()).startsWithIgnoringCase("utf8mb4_");
        }
    }

    private static Set<String> baseTables(Connection connection) throws SQLException {
        Set<String> tables = new TreeSet<>();
        try (ResultSet result = connection.getMetaData()
                .getTables(connection.getCatalog(), null, "%", new String[] {"TABLE"})) {
            while (result.next()) {
                tables.add(result.getString("TABLE_NAME"));
            }
        }
        return tables;
    }

    private static Set<String> businessTables(Connection connection) throws SQLException {
        Set<String> tables = baseTables(connection);
        tables.remove("flyway_schema_history");
        return tables;
    }

    private static List<String> primaryKeys(Connection connection, String table) throws SQLException {
        List<String> columns = new ArrayList<>();
        try (ResultSet result = connection.getMetaData()
                .getPrimaryKeys(connection.getCatalog(), null, table)) {
            while (result.next()) {
                columns.add(result.getString("COLUMN_NAME"));
            }
        }
        return columns;
    }

    private static Map<String, ColumnDefinition> columns(Connection connection, String table)
            throws SQLException {
        Map<String, ColumnDefinition> columns = new LinkedHashMap<>();
        String sql = """
                SELECT COLUMN_NAME,
                       IS_NULLABLE,
                       COLUMN_DEFAULT,
                       EXTRA,
                       DATA_TYPE,
                       COLUMN_TYPE,
                       CHARACTER_MAXIMUM_LENGTH,
                       NUMERIC_PRECISION,
                       DATETIME_PRECISION
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
                ORDER BY ORDINAL_POSITION
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    columns.put(
                            result.getString("COLUMN_NAME"),
                            new ColumnDefinition(
                                    result.getString("IS_NULLABLE"),
                                    result.getString("COLUMN_DEFAULT"),
                                    result.getString("EXTRA"),
                                    result.getString("DATA_TYPE"),
                                    result.getString("COLUMN_TYPE"),
                                    nullableLong(result, "CHARACTER_MAXIMUM_LENGTH"),
                                    nullableLong(result, "NUMERIC_PRECISION"),
                                    nullableLong(result, "DATETIME_PRECISION")));
                }
            }
        }
        return columns;
    }

    private static Long nullableLong(ResultSet result, String column) throws SQLException {
        long value = result.getLong(column);
        return result.wasNull() ? null : value;
    }

    private static TableDefinition tableDefinition(Connection connection, String table)
            throws SQLException {
        String sql = """
                SELECT ENGINE, TABLE_COLLATION
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            try (ResultSet result = statement.executeQuery()) {
                assertThat(result.next()).isTrue();
                return new TableDefinition(result.getString("ENGINE"), result.getString("TABLE_COLLATION"));
            }
        }
    }

    private static List<ForeignKey> foreignKeys(Connection connection, String table) throws SQLException {
        List<ForeignKey> foreignKeys = new ArrayList<>();
        try (ResultSet result = connection.getMetaData()
                .getImportedKeys(connection.getCatalog(), null, table)) {
            while (result.next()) {
                foreignKeys.add(new ForeignKey(
                        result.getString("FKCOLUMN_NAME"),
                        result.getString("PKTABLE_NAME"),
                        result.getString("PKCOLUMN_NAME"),
                        result.getInt("DELETE_RULE"),
                        result.getInt("UPDATE_RULE")));
            }
        }
        return foreignKeys;
    }

    private static List<List<String>> compositeForeignKeyColumns(
            Connection connection, String table, String referencedTable) throws SQLException {
        Map<String, List<String>> columnsByConstraint = new LinkedHashMap<>();
        try (ResultSet result = connection.getMetaData()
                .getImportedKeys(connection.getCatalog(), null, table)) {
            while (result.next()) {
                if (referencedTable.equals(result.getString("PKTABLE_NAME"))) {
                    columnsByConstraint.computeIfAbsent(result.getString("FK_NAME"), ignored -> new ArrayList<>())
                            .add(result.getString("FKCOLUMN_NAME"));
                }
            }
        }
        return new ArrayList<>(columnsByConstraint.values());
    }

    private static void assertExactIndexes(Connection connection, String table,
                                           Map<String, List<String>> expected) throws SQLException {
        assertThat(indexes(connection, table)).isEqualTo(expected);
    }

    private static void assertExactColumns(Map<String, ColumnDefinition> actual,
                                           Map<String, ColumnContract> expected) {
        assertThat(actual.keySet()).containsExactlyInAnyOrderElementsOf(expected.keySet());
        expected.forEach((name, contract) -> {
            ColumnDefinition column = actual.get(name);
            assertThat(new ColumnContract(column.dataType(), column.columnType(), column.nullable(),
                    column.defaultValue(), column.extra())).isEqualTo(contract);
        });
    }

    private static ColumnContract column(String dataType, String columnType, String nullable,
                                         String defaultValue, String extra) {
        return new ColumnContract(dataType, columnType, nullable, defaultValue, extra);
    }

    private static void assertExactChecks(Connection connection, String table,
                                          Set<String> expected) throws SQLException {
        assertThat(checkClauses(connection, table).stream()
                .map(FlywayMigrationTest::normalizeCheck)
                .collect(java.util.stream.Collectors.toSet()))
                .isEqualTo(expected.stream().map(FlywayMigrationTest::normalizeCheck)
                        .collect(java.util.stream.Collectors.toSet()));
    }

    private static String normalizeCheck(String clause) {
        return clause.replace("`", "")
                .replaceAll("(?i)_utf8mb4", "")
                .replace("\\", "")
                .replaceAll("\\s+", "")
                .replaceAll("^\\((.*)\\)$", "$1")
                .toUpperCase(java.util.Locale.ROOT);
    }

    private static void assertExactForeignKeys(Connection connection, String table,
                                                Set<ForeignKeyContract> expected) throws SQLException {
        Set<ForeignKeyContract> actual = new java.util.HashSet<>();
        String sql = """
                SELECT kcu.CONSTRAINT_NAME,
                       kcu.COLUMN_NAME,
                       kcu.REFERENCED_TABLE_NAME,
                       kcu.REFERENCED_COLUMN_NAME,
                       kcu.ORDINAL_POSITION,
                       rc.DELETE_RULE,
                       rc.UPDATE_RULE
                FROM information_schema.KEY_COLUMN_USAGE kcu
                JOIN information_schema.REFERENTIAL_CONSTRAINTS rc
                  ON rc.CONSTRAINT_SCHEMA = kcu.CONSTRAINT_SCHEMA
                 AND rc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME
                 AND rc.TABLE_NAME = kcu.TABLE_NAME
                WHERE kcu.CONSTRAINT_SCHEMA = DATABASE()
                  AND kcu.TABLE_NAME = ?
                  AND kcu.REFERENCED_TABLE_NAME IS NOT NULL
                ORDER BY kcu.CONSTRAINT_NAME, kcu.ORDINAL_POSITION
                """;
        Map<String, ForeignKeyParts> parts = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, table);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    String name = result.getString("CONSTRAINT_NAME");
                    String referencedTable = result.getString("REFERENCED_TABLE_NAME");
                    String deleteRule = result.getString("DELETE_RULE");
                    String updateRule = result.getString("UPDATE_RULE");
                    ForeignKeyParts key = parts.computeIfAbsent(name, ignored ->
                            new ForeignKeyParts(referencedTable, deleteRule, updateRule));
                    key.columns().add(result.getString("COLUMN_NAME"));
                    key.referencedColumns().add(result.getString("REFERENCED_COLUMN_NAME"));
                }
            }
        }
        for (Map.Entry<String, ForeignKeyParts> entry : parts.entrySet()) {
            ForeignKeyParts key = entry.getValue();
            actual.add(new ForeignKeyContract(entry.getKey(), key.columns(), key.referencedTable(),
                    key.referencedColumns(), key.deleteRule(), key.updateRule()));
        }
        assertThat(actual).isEqualTo(expected);
    }

    private static final class ForeignKeyParts {
        private final String referencedTable;
        private final String deleteRule;
        private final String updateRule;
        private final List<String> columns = new ArrayList<>();
        private final List<String> referencedColumns = new ArrayList<>();

        private ForeignKeyParts(String referencedTable, String deleteRule, String updateRule) {
            this.referencedTable = referencedTable;
            this.deleteRule = deleteRule;
            this.updateRule = updateRule;
        }

        String referencedTable() { return referencedTable; }
        String deleteRule() { return deleteRule; }
        String updateRule() { return updateRule; }
        List<String> columns() { return columns; }
        List<String> referencedColumns() { return referencedColumns; }
    }

    private static Map<String, List<String>> uniqueIndexes(Connection connection, String table)
            throws SQLException {
        return indexes(connection, table, true);
    }

    private static Map<String, List<String>> indexes(Connection connection, String table)
            throws SQLException {
        return indexes(connection, table, false);
    }

    private static Map<String, List<String>> indexes(
            Connection connection, String table, boolean uniqueOnly) throws SQLException {
        Map<String, List<String>> indexes = new LinkedHashMap<>();
        try (ResultSet result = connection.getMetaData()
                .getIndexInfo(connection.getCatalog(), null, table, uniqueOnly, false)) {
            while (result.next()) {
                String indexName = result.getString("INDEX_NAME");
                String columnName = result.getString("COLUMN_NAME");
                if (indexName != null && columnName != null) {
                    indexes.computeIfAbsent(indexName, ignored -> new ArrayList<>()).add(columnName);
                }
            }
        }
        return indexes;
    }

    private static List<String> indexColumns(Connection connection, String table, String index) throws SQLException {
        List<String> columns = new ArrayList<>();
        try (ResultSet result = connection.getMetaData()
                .getIndexInfo(connection.getCatalog(), null, table, false, false)) {
            while (result.next()) {
                if (index.equals(result.getString("INDEX_NAME")) && result.getString("COLUMN_NAME") != null) {
                    columns.add(result.getString("COLUMN_NAME"));
                }
            }
        }
        return columns;
    }

    private static List<String> checkClauses(Connection connection, String table) throws SQLException {
        List<String> clauses = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT cc.check_clause FROM information_schema.check_constraints cc
                JOIN information_schema.table_constraints tc
                  ON tc.constraint_schema = cc.constraint_schema AND tc.constraint_name = cc.constraint_name
                WHERE cc.constraint_schema = DATABASE() AND tc.table_name = ? AND tc.constraint_type = 'CHECK'
                """)) {
            statement.setString(1, table);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) clauses.add(result.getString(1));
            }
        }
        return clauses;
    }

    private static List<String> appliedVersions(Connection connection) throws SQLException {
        List<String> versions = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT version FROM flyway_schema_history WHERE success = 1 ORDER BY installed_rank
                """); ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                versions.add(result.getString("version"));
            }
        }
        return versions;
    }

    private record ColumnDefinition(
            String nullable,
            String defaultValue,
            String extra,
            String dataType,
            String columnType,
            Long characterMaximumLength,
            Long numericPrecision,
            Long datetimePrecision) {}

    private record ColumnContract(
            String dataType,
            String columnType,
            String nullable,
            String defaultValue,
            String extra) {}

    private record TableDefinition(String engine, String tableCollation) {}

    private record ForeignKey(
            String column,
            String referencedTable,
            String referencedColumn,
            int deleteRule,
            int updateRule) {}

    private record ForeignKeyContract(
            String name,
            List<String> columns,
            String referencedTable,
            List<String> referencedColumns,
            String deleteRule,
            String updateRule) {}
}
