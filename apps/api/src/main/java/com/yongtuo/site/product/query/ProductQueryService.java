package com.yongtuo.site.product.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yongtuo.site.attribute.AttributeDataType;
import com.yongtuo.site.attribute.AttributeDefinition;
import com.yongtuo.site.attribute.AttributeDefinitionMapper;
import com.yongtuo.site.attribute.AttributeStatus;
import com.yongtuo.site.product.Product;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductQueryService {
    private final ProductQueryMapper mapper;
    private final AttributeDefinitionMapper attributes;
    private final JdbcTemplate jdbc;
    private final PublicProductCompositionAssembler composition;

    public ProductQueryService(ProductQueryMapper mapper, AttributeDefinitionMapper attributes,
                               JdbcTemplate jdbc, PublicProductCompositionAssembler composition) {
        this.mapper = mapper;
        this.attributes = attributes;
        this.jdbc = jdbc;
        this.composition = composition;
    }

    @Transactional(readOnly = true)
    public PublicProductPage list(ProductFilter filter, Locale locale) {
        return list(filter, locale, null);
    }

    @Transactional(readOnly = true)
    public PublicProductPage list(ProductFilter filter, Locale locale, String keyword) {
        String term = keyword == null || keyword.isBlank() ? null : keyword.trim();
        if (term != null && term.length() > 200) throw ProductQueryException.invalidFilter();
        ResolvedProductQuery query = resolve(filter, locale, term);
        long total = mapper.count(query);
        boolean english = isEnglish(locale);
        List<PublicProductDto> items = total == 0 ? List.of() : mapper.list(query).stream()
                .map(product -> PublicProductDtoFactory.summary(product, english)).toList();
        int pages = total == 0 ? 0 : (int) ((total + filter.pageSize() - 1) / filter.pageSize());
        return new PublicProductPage(items, filter.page(), filter.pageSize(), total, pages);
    }

    @Transactional(readOnly = true)
    public PublicProductDto detail(String slug, Locale locale) {
        boolean english = isEnglish(locale);
        Product product = mapper.detail(slug, english);
        if (product == null) throw ProductQueryException.notFound();
        return PublicProductDtoFactory.complete(product, english,
                composition.assemble(product.getId(), product.getCategoryId(), english),
                jdbc.queryForObject("SELECT slug FROM product_category WHERE id = ?", String.class, product.getCategoryId()),
                jdbc.query("SELECT pi.image_url, pi.alt_zh, pi.alt_en FROM product_image pi LEFT JOIN media_file m ON m.id = pi.media_id "
                        + "WHERE pi.product_id = ? AND (pi.media_id IS NULL OR (m.deleted_at IS NULL AND m.status = 'ACTIVE')) ORDER BY pi.is_cover DESC, pi.sort_order, pi.id",
                        (rs, row) -> new PublicProductDto.Image(rs.getString("image_url"), rs.getString(english ? "alt_en" : "alt_zh")), product.getId()));
    }

    private ResolvedProductQuery resolve(ProductFilter filter, Locale locale, String keyword) {
        boolean newest = switch (filter.sort() == null ? "default" : filter.sort()) {
            case "default", "sortOrder", "sort_order" -> false;
            case "newest" -> true;
            default -> throw ProductQueryException.invalidFilter();
        };
        List<ResolvedProductQuery.Condition> conditions = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : filter.attributes().entrySet()) {
            if (entry.getKey() == null || entry.getValue().isEmpty() || entry.getValue().stream().anyMatch(value -> value == null)) {
                throw ProductQueryException.invalidFilter();
            }
            AttributeDefinition definition = attributes.selectOne(new LambdaQueryWrapper<AttributeDefinition>()
                    .eq(AttributeDefinition::getCode, entry.getKey())
                    .eq(AttributeDefinition::getStatus, AttributeStatus.ACTIVE));
            if (definition == null || !isFilterable(definition)) throw ProductQueryException.invalidFilter();
            boolean numeric = definition.getDataType() == AttributeDataType.NUMBER;
            boolean select = definition.getDataType() == AttributeDataType.SELECT
                    || definition.getDataType() == AttributeDataType.MULTI_SELECT;
            List<?> values;
            try {
                values = numeric ? entry.getValue().stream().map(BigDecimal::new).toList() : entry.getValue();
            } catch (NumberFormatException exception) {
                throw ProductQueryException.invalidFilter();
            }
            boolean fallbackFilterable = Boolean.TRUE.equals(definition.getIsGlobal())
                    && Boolean.TRUE.equals(definition.getDefaultFilterable());
            conditions.add(new ResolvedProductQuery.Condition(definition.getId(), numeric, select,
                    fallbackFilterable, values));
        }
        return new ResolvedProductQuery(filter.category(), List.copyOf(conditions), filter.pageSize(),
                (long) (filter.page() - 1) * filter.pageSize(), isEnglish(locale), newest, keyword);
    }

    private boolean isFilterable(AttributeDefinition definition) {
        if (Boolean.TRUE.equals(definition.getIsGlobal()) && Boolean.TRUE.equals(definition.getDefaultFilterable())) return true;
        return jdbc.queryForObject("SELECT COUNT(*) FROM category_attribute WHERE attribute_id = ? AND is_filterable = 1",
                Integer.class, definition.getId()) > 0;
    }

    private static boolean isEnglish(Locale locale) {
        return locale != null && Locale.ENGLISH.getLanguage().equalsIgnoreCase(locale.getLanguage());
    }

}
