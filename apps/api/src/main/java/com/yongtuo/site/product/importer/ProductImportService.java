package com.yongtuo.site.product.importer;

import com.yongtuo.site.product.ProductStatus;
import com.yongtuo.site.site.UrlRedirectService;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
public class ProductImportService {
    static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    static final int MAX_ROWS = 1000;
    private static final int MAX_ZIP_ENTRIES = 100;
    private static final long MAX_UNCOMPRESSED_ENTRY_SIZE = 10L * 1024 * 1024;
    private static final long MAX_TOTAL_UNCOMPRESSED_SIZE = 20L * 1024 * 1024;
    private static final long MAX_XML_TEXT_SIZE = 10L * 1024 * 1024;
    private static final Pattern SQL_IDENTIFIER = Pattern.compile("[A-Za-z0-9_]+");
    private static final String XLSX_MIME =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final List<String> REQUIRED_HEADERS =
            List.of("productCode", "slug", "categorySlug", "nameZh", "status");
    private static final String SLUG_PATTERN = "[a-z0-9]+(?:-[a-z0-9]+)*";

    private final JdbcTemplate jdbc;
    private final ObjectMapper objectMapper;
    private final ImportPreviewStore previewStore;
    private final UrlRedirectService redirects;

    static {
        ZipSecureFile.setMaxEntrySize(MAX_UNCOMPRESSED_ENTRY_SIZE);
        ZipSecureFile.setMaxTextSize(MAX_XML_TEXT_SIZE);
    }

    public ProductImportService(JdbcTemplate jdbc, ObjectMapper objectMapper,
                                ImportPreviewStore previewStore, UrlRedirectService redirects) {
        this.jdbc = jdbc;
        this.objectMapper = objectMapper;
        this.previewStore = previewStore;
        this.redirects = redirects;
    }

    @Transactional(readOnly = true)
    public ProductImportPreview preview(MultipartFile file) {
        byte[] content = validateAndRead(file);
        preflightZip(content);
        ParsedWorkbook parsed = parse(content);
        List<RowCandidate> candidates = parsed.rows();
        if (candidates.isEmpty()) throw ProductImportException.invalidTemplate();

        String candidateJson = candidateJson(candidates);
        ColumnSemantics codeSemantics = columnSemantics("product", "product_code");
        ColumnSemantics slugSemantics = columnSemantics("product", "slug");
        ColumnSemantics categorySemantics = columnSemantics("product_category", "slug");
        Set<Integer> duplicateCodes = duplicateRows(candidateJson, "productCode", codeSemantics);
        Set<Integer> duplicateSlugs = duplicateRows(candidateJson, "slug", slugSemantics);
        Set<Integer> existingCodes = existingProductRows(candidateJson, "productCode",
                "product_code", codeSemantics);
        Set<Integer> existingSlugs = existingProductRows(candidateJson, "slug", "slug", slugSemantics);
        Map<Integer, Long> categories = categoryRows(candidateJson, categorySemantics);

        List<ProductImportRowResult> results = new ArrayList<>();
        List<NormalizedProductRow> normalized = new ArrayList<>();
        for (RowCandidate candidate : candidates) {
            List<String> messages = new ArrayList<>(candidate.parseMessages());
            require(candidate.productCode(), "REQUIRED_PRODUCT_CODE", messages);
            require(candidate.slug(), "REQUIRED_SLUG", messages);
            require(candidate.categorySlug(), "REQUIRED_CATEGORY_SLUG", messages);
            require(candidate.nameZh(), "REQUIRED_NAME_ZH", messages);
            require(candidate.status(), "REQUIRED_STATUS", messages);
            length(candidate.productCode(), 100, "PRODUCT_CODE_TOO_LONG", messages);
            length(candidate.slug(), 191, "SLUG_TOO_LONG", messages);
            length(candidate.nameZh(), 200, "NAME_ZH_TOO_LONG", messages);
            if (!candidate.slug().isBlank() && !candidate.slug().matches(SLUG_PATTERN)) {
                messages.add("INVALID_SLUG");
            }
            if (duplicateCodes.contains(candidate.rowNumber())) {
                messages.add("DUPLICATE_PRODUCT_CODE");
            }
            if (duplicateSlugs.contains(candidate.rowNumber())) {
                messages.add("DUPLICATE_SLUG");
            }
            if (existingCodes.contains(candidate.rowNumber())) messages.add("PRODUCT_CODE_EXISTS");
            if (existingSlugs.contains(candidate.rowNumber())) messages.add("PRODUCT_SLUG_EXISTS");
            Long categoryId = categories.get(candidate.rowNumber());
            if (!candidate.categorySlug().isBlank() && categoryId == null) messages.add("UNKNOWN_CATEGORY");
            ProductStatus status = parseStatus(candidate.status(), messages);

            List<String> uniqueMessages = List.copyOf(new java.util.LinkedHashSet<>(messages));
            ImportRowLevel level = uniqueMessages.isEmpty() ? ImportRowLevel.VALID : ImportRowLevel.ERROR;
            results.add(new ProductImportRowResult(candidate.rowNumber(), candidate.productCode(),
                    candidate.slug(), candidate.categorySlug(), candidate.nameZh(), candidate.status(),
                    level, uniqueMessages));
            if (level == ImportRowLevel.VALID) {
                normalized.add(new NormalizedProductRow(candidate.rowNumber(), categoryId,
                        candidate.productCode(), candidate.slug(), candidate.nameZh(), status));
            }
        }

        int valid = (int) results.stream().filter(row -> row.level() == ImportRowLevel.VALID).count();
        int warning = (int) results.stream().filter(row -> row.level() == ImportRowLevel.WARNING).count();
        int error = results.size() - valid - warning;
        String token = previewStore.put(normalized, error == 0);
        return new ProductImportPreview(token, results.size(), valid, warning, error, results);
    }

    @Transactional
    public ProductImportConfirmation confirm(String token) {
        ImportPreviewStore.Snapshot snapshot = previewStore.claim(token)
                .orElseThrow(ProductImportException::invalidToken);
        if (!snapshot.confirmable() || snapshot.rows().isEmpty()) {
            previewStore.release(token, snapshot);
            throw ProductImportException.notConfirmable();
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            previewStore.release(token, snapshot);
            throw new IllegalStateException("Product import confirmation requires an active transaction");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) previewStore.complete(token, snapshot);
                else previewStore.release(token, snapshot);
            }
        });
        try {
            List<Long> categoryIds = snapshot.rows().stream()
                    .map(NormalizedProductRow::categoryId)
                    .distinct()
                    .sorted()
                    .toList();
            String placeholders = String.join(", ", Collections.nCopies(categoryIds.size(), "?"));
            List<Long> activeCategoryIds = jdbc.queryForList("""
                    SELECT id
                    FROM product_category
                    WHERE id IN (%s)
                      AND status = 'ACTIVE'
                      AND deleted_at IS NULL
                    ORDER BY id
                    FOR UPDATE
                    """.formatted(placeholders), Long.class, categoryIds.toArray());
            if (!activeCategoryIds.equals(categoryIds)) {
                throw ProductImportException.changedSincePreview();
            }
            for (NormalizedProductRow row : snapshot.rows()) {
                jdbc.update("""
                        INSERT INTO product(
                          category_id, product_code, name_zh, slug,
                          english_status, is_featured, sort_order, status
                        ) VALUES (?, ?, ?, ?, 'EMPTY', 0, 0, ?)
                        """, row.categoryId(), row.productCode(), row.nameZh(), row.slug(), row.status().name());
                redirects.prepareProductCanonical(row.slug(), row.status() == ProductStatus.PUBLISHED, false);
            }
        } catch (DataIntegrityViolationException exception) {
            throw ProductImportException.changedSincePreview();
        }
        return new ProductImportConfirmation(snapshot.rows().size());
    }

    private static byte[] validateAndRead(MultipartFile file) {
        if (file == null || file.isEmpty()) throw ProductImportException.invalidFile();
        if (file.getSize() > MAX_FILE_SIZE) throw ProductImportException.fileTooLarge();
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(".xlsx")
                || !XLSX_MIME.equalsIgnoreCase(file.getContentType())) {
            throw ProductImportException.invalidFile();
        }
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length < 4 || bytes[0] != 'P' || bytes[1] != 'K'
                    || bytes[2] != 3 || bytes[3] != 4) {
                throw ProductImportException.invalidFile();
            }
            return bytes;
        } catch (IOException exception) {
            throw ProductImportException.invalidFile();
        }
    }

    /**
     * Streams every ZIP entry before POI allocates workbook objects. POI's JVM-wide limits below
     * remain defense in depth; these per-request totals are the import template's primary bound.
     */
    private static void preflightZip(byte[] content) {
        int entries = 0;
        long totalBytes = 0;
        byte[] buffer = new byte[8192];
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(content))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > MAX_ZIP_ENTRIES) throw ProductImportException.invalidFile();
                long entryBytes = 0;
                int read;
                while ((read = zip.read(buffer)) != -1) {
                    entryBytes += read;
                    totalBytes += read;
                    if (entryBytes > MAX_UNCOMPRESSED_ENTRY_SIZE
                            || totalBytes > MAX_TOTAL_UNCOMPRESSED_SIZE) {
                        throw ProductImportException.invalidFile();
                    }
                }
                zip.closeEntry();
            }
            if (entries == 0) throw ProductImportException.invalidFile();
        } catch (ProductImportException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw ProductImportException.invalidFile();
        }
    }

    private static ParsedWorkbook parse(byte[] content) {
        try (ByteArrayInputStream input = new ByteArrayInputStream(content);
             XSSFWorkbook workbook = new XSSFWorkbook(input)) {
            if (workbook.getNumberOfSheets() == 0) throw ProductImportException.invalidTemplate();
            Sheet sheet = workbook.getSheetAt(0);
            Row header = sheet.getRow(sheet.getFirstRowNum());
            Map<String, Integer> columns = headerColumns(header);
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            List<RowCandidate> rows = new ArrayList<>();
            int firstDataRow = sheet.getFirstRowNum() + 1;
            for (int rowIndex = firstDataRow; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isBlank(row, columns, formatter)) continue;
                if (rows.size() >= MAX_ROWS) throw ProductImportException.tooManyRows();
                List<String> parseMessages = new ArrayList<>();
                Map<String, String> values = new LinkedHashMap<>();
                for (String headerName : REQUIRED_HEADERS) {
                    Cell cell = row.getCell(columns.get(headerName), Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    if (cell != null && cell.getCellType() == CellType.FORMULA) {
                        parseMessages.add("FORMULA_NOT_ALLOWED");
                        values.put(headerName, "");
                    } else {
                        values.put(headerName, cell == null ? "" : formatter.formatCellValue(cell).strip());
                    }
                }
                rows.add(new RowCandidate(rowIndex + 1, values.get("productCode"), values.get("slug"),
                        values.get("categorySlug"), values.get("nameZh"), values.get("status"),
                        parseMessages));
            }
            return new ParsedWorkbook(rows);
        } catch (ProductImportException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw ProductImportException.invalidFile();
        }
    }

    private static Map<String, Integer> headerColumns(Row header) {
        if (header == null) throw ProductImportException.invalidTemplate();
        DataFormatter formatter = new DataFormatter(Locale.ROOT);
        Map<String, Integer> columns = new HashMap<>();
        for (Cell cell : header) {
            if (cell.getCellType() == CellType.FORMULA) throw ProductImportException.invalidTemplate();
            String value = formatter.formatCellValue(cell).strip();
            if (!value.isBlank() && columns.putIfAbsent(value, cell.getColumnIndex()) != null) {
                throw ProductImportException.invalidTemplate();
            }
        }
        if (!columns.keySet().containsAll(REQUIRED_HEADERS)) throw ProductImportException.invalidTemplate();
        return columns;
    }

    private String candidateJson(List<RowCandidate> candidates) {
        List<Map<String, Object>> values = new ArrayList<>();
        for (RowCandidate candidate : candidates) {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("rowNumber", candidate.rowNumber());
            value.put("productCode", candidate.productCode());
            value.put("slug", candidate.slug());
            value.put("categorySlug", candidate.categorySlug());
            values.add(value);
        }
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JacksonException exception) {
            throw new IllegalStateException("Unable to prepare product import candidates", exception);
        }
    }

    private Set<Integer> duplicateRows(String candidateJson, String jsonField,
                                       ColumnSemantics semantics) {
        String table = jsonTable(jsonField, semantics);
        String sql = """
                SELECT DISTINCT left_rows.source_row
                FROM %s left_rows
                JOIN %s right_rows
                  ON left_rows.source_row <> right_rows.source_row
                 AND left_rows.candidate = right_rows.candidate
                WHERE left_rows.candidate <> ''
                """.formatted(table, table);
        return Set.copyOf(jdbc.queryForList(sql, Integer.class, candidateJson, candidateJson));
    }

    private Set<Integer> existingProductRows(String candidateJson, String jsonField,
                                             String productColumn, ColumnSemantics semantics) {
        requireSqlIdentifier(productColumn);
        String sql = """
                SELECT DISTINCT candidates.source_row
                FROM %s candidates
                JOIN product existing_product
                  ON existing_product.%s = candidates.candidate
                WHERE candidates.candidate <> ''
                  AND existing_product.deleted_at IS NULL
                """.formatted(jsonTable(jsonField, semantics), productColumn);
        return Set.copyOf(jdbc.queryForList(sql, Integer.class, candidateJson));
    }

    private Map<Integer, Long> categoryRows(String candidateJson, ColumnSemantics semantics) {
        String sql = """
                SELECT candidates.source_row, category.id
                FROM %s candidates
                JOIN product_category category ON category.slug = candidates.candidate
                WHERE candidates.candidate <> ''
                  AND category.status = 'ACTIVE'
                  AND category.deleted_at IS NULL
                """.formatted(jsonTable("categorySlug", semantics));
        Map<Integer, Long> result = new HashMap<>();
        jdbc.query(sql, resultSet -> {
            result.put(resultSet.getInt("source_row"), resultSet.getLong("id"));
        }, candidateJson);
        return Map.copyOf(result);
    }

    private ColumnSemantics columnSemantics(String table, String column) {
        ColumnSemantics semantics = jdbc.queryForObject("""
                SELECT character_set_name, collation_name, character_maximum_length
                FROM information_schema.columns
                WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?
                """, (resultSet, rowNumber) -> new ColumnSemantics(
                resultSet.getString("character_set_name"), resultSet.getString("collation_name"),
                resultSet.getInt("character_maximum_length")), table, column);
        if (semantics == null) throw new IllegalStateException("Product import column metadata is unavailable");
        requireSqlIdentifier(semantics.characterSet());
        requireSqlIdentifier(semantics.collation());
        if (semantics.maximumLength() < 1 || semantics.maximumLength() > 1000) {
            throw new IllegalStateException("Product import column length is unsafe");
        }
        return semantics;
    }

    private static String jsonTable(String jsonField, ColumnSemantics semantics) {
        requireSqlIdentifier(jsonField);
        return """
                JSON_TABLE(CAST(? AS JSON), '$[*]' COLUMNS (
                  source_row INT PATH '$.rowNumber',
                  candidate VARCHAR(%d) CHARACTER SET %s COLLATE %s PATH '$.%s'
                ))
                """.formatted(semantics.maximumLength(), semantics.characterSet(),
                semantics.collation(), jsonField).strip();
    }

    private static void requireSqlIdentifier(String identifier) {
        if (identifier == null || !SQL_IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalStateException("Unsafe database text metadata");
        }
    }

    private static boolean isBlank(Row row, Map<String, Integer> columns, DataFormatter formatter) {
        return REQUIRED_HEADERS.stream().map(columns::get).map(row::getCell)
                .allMatch(cell -> cell == null || formatter.formatCellValue(cell).isBlank());
    }

    private static void require(String value, String message, List<String> messages) {
        if (value.isBlank()) messages.add(message);
    }

    private static void length(String value, int maximum, String message, List<String> messages) {
        if (value.codePointCount(0, value.length()) > maximum) messages.add(message);
    }

    private static ProductStatus parseStatus(String value, List<String> messages) {
        if (value.isBlank()) return null;
        try {
            return ProductStatus.valueOf(value);
        } catch (IllegalArgumentException exception) {
            messages.add("INVALID_STATUS");
            return null;
        }
    }

    private record ParsedWorkbook(List<RowCandidate> rows) {
        private ParsedWorkbook {
            rows = List.copyOf(rows);
        }
    }

    private record RowCandidate(int rowNumber, String productCode, String slug, String categorySlug,
                                String nameZh, String status, List<String> parseMessages) {
        private RowCandidate {
            parseMessages = List.copyOf(parseMessages);
        }
    }

    private record ColumnSemantics(String characterSet, String collation, int maximumLength) {}
}
