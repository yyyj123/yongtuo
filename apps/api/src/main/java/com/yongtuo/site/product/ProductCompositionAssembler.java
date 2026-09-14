package com.yongtuo.site.product;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yongtuo.site.attribute.CategoryAttributeDto;
import com.yongtuo.site.attribute.CategoryAttributeService;
import com.yongtuo.site.attribute.ProductAttributeValue;
import com.yongtuo.site.attribute.ProductAttributeValueMapper;
import com.yongtuo.site.product.variant.ProductVariant;
import com.yongtuo.site.product.variant.ProductVariantMapper;
import com.yongtuo.site.product.variant.ProductVariantValue;
import com.yongtuo.site.product.variant.ProductVariantValueMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** Batch-loads and assembles normalized admin composition DTOs. */
@Service
public class ProductCompositionAssembler {
    private final ProductAttributeValueMapper productValues;
    private final ProductVariantMapper variants;
    private final ProductVariantValueMapper variantValues;
    private final CategoryAttributeService categoryAttributes;

    public ProductCompositionAssembler(ProductAttributeValueMapper productValues, ProductVariantMapper variants,
                                       ProductVariantValueMapper variantValues,
                                       CategoryAttributeService categoryAttributes) {
        this.productValues = productValues;
        this.variants = variants;
        this.variantValues = variantValues;
        this.categoryAttributes = categoryAttributes;
    }

    public ProductCompositionService.CompositionDto assemble(long productId, long categoryId) {
        Map<Long, CategoryAttributeDto> rules = new HashMap<>();
        categoryAttributes.getCategoryAttributes(categoryId).forEach(rule -> rules.put(rule.attributeId(), rule));
        List<ProductAttributeValue> rawAttributes = productValues.selectList(new LambdaQueryWrapper<ProductAttributeValue>()
                .eq(ProductAttributeValue::getProductId, productId)
                .orderByAsc(ProductAttributeValue::getAttributeId, ProductAttributeValue::getId));
        List<ProductAttributeDto> attributes = aggregateProductValues(rawAttributes, rules);
        List<ProductVariant> rawVariants = variants.selectList(new LambdaQueryWrapper<ProductVariant>()
                .eq(ProductVariant::getProductId, productId)
                .orderByAsc(ProductVariant::getSortOrder, ProductVariant::getId));
        List<ProductVariantDto> result = new ArrayList<>();
        if (!rawVariants.isEmpty()) {
            List<Long> ids = rawVariants.stream().map(ProductVariant::getId).toList();
            List<ProductVariantValue> rawValues = variantValues.selectList(new LambdaQueryWrapper<ProductVariantValue>()
                    .in(ProductVariantValue::getVariantId, ids)
                    .orderByAsc(ProductVariantValue::getVariantId, ProductVariantValue::getAttributeId,
                            ProductVariantValue::getId));
            Map<Long, List<ProductVariantValue>> byVariant = new LinkedHashMap<>();
            rawValues.forEach(value -> byVariant.computeIfAbsent(value.getVariantId(), ignored -> new ArrayList<>()).add(value));
            for (ProductVariant variant : rawVariants) {
                result.add(new ProductVariantDto(variant.getId(), variant.getVariantCode(), variant.getNameZh(),
                        variant.getNameEn(), safeOrder(variant.getSortOrder()), variant.getStatus(),
                        aggregateVariantValues(byVariant.getOrDefault(variant.getId(), List.of()), rules)));
            }
        }
        return new ProductCompositionService.CompositionDto(attributes, result);
    }

    private static List<ProductAttributeDto> aggregateProductValues(List<ProductAttributeValue> values,
                                                                     Map<Long, CategoryAttributeDto> rules) {
        Map<Long, List<ProductAttributeValue>> groups = new LinkedHashMap<>();
        values.forEach(value -> groups.computeIfAbsent(value.getAttributeId(), ignored -> new ArrayList<>()).add(value));
        return groups.values().stream().map(group -> dto(group, rules.get(group.getFirst().getAttributeId())))
                .sorted(Comparator.comparingInt(ProductAttributeDto::sortOrder)
                        .thenComparing(value -> value.code() == null ? "" : value.code())).toList();
    }

    private static List<ProductAttributeDto> aggregateVariantValues(List<ProductVariantValue> values,
                                                                     Map<Long, CategoryAttributeDto> rules) {
        Map<Long, List<ProductVariantValue>> groups = new LinkedHashMap<>();
        values.forEach(value -> groups.computeIfAbsent(value.getAttributeId(), ignored -> new ArrayList<>()).add(value));
        return groups.values().stream().map(group -> dtoVariant(group, rules.get(group.getFirst().getAttributeId()))).toList();
    }

    private static ProductAttributeDto dto(List<ProductAttributeValue> values, CategoryAttributeDto rule) {
        ProductAttributeValue first = values.getFirst();
        List<Long> optionIds = values.stream().map(ProductAttributeValue::getOptionId)
                .filter(java.util.Objects::nonNull).sorted().toList();
        return dtoBase(first.getAttributeId(), rule,
                rule != null && rule.dataType() == com.yongtuo.site.attribute.AttributeDataType.MULTI_SELECT ? null : first.getValueZh(),
                rule != null && rule.dataType() == com.yongtuo.site.attribute.AttributeDataType.MULTI_SELECT ? null : first.getValueEn(),
                first.getNumericValue(), rule != null && rule.dataType() == com.yongtuo.site.attribute.AttributeDataType.MULTI_SELECT ? null : first.getOptionId(),
                optionIds, first.getSortOrder());
    }

    private static ProductAttributeDto dtoVariant(List<ProductVariantValue> values, CategoryAttributeDto rule) {
        ProductVariantValue first = values.getFirst();
        List<Long> optionIds = values.stream().map(ProductVariantValue::getOptionId)
                .filter(java.util.Objects::nonNull).sorted().toList();
        return dtoBase(first.getAttributeId(), rule,
                rule != null && rule.dataType() == com.yongtuo.site.attribute.AttributeDataType.MULTI_SELECT ? null : first.getValueZh(),
                rule != null && rule.dataType() == com.yongtuo.site.attribute.AttributeDataType.MULTI_SELECT ? null : first.getValueEn(),
                first.getNumericValue(), rule != null && rule.dataType() == com.yongtuo.site.attribute.AttributeDataType.MULTI_SELECT ? null : first.getOptionId(),
                optionIds, 0);
    }

    private static ProductAttributeDto dtoBase(Long id, CategoryAttributeDto rule, String zh, String en,
                                               java.math.BigDecimal number, Long optionId, List<Long> optionIds,
                                               Integer order) {
        return new ProductAttributeDto(id, rule == null ? null : rule.code(), rule == null ? null : rule.nameZh(),
                rule == null ? null : rule.nameEn(), rule == null ? null : rule.dataType(), rule == null ? null : rule.unit(),
                zh, en, number, optionId, optionIds, safeOrder(order));
    }

    private static int safeOrder(Integer value) { return value == null ? 0 : value; }
}
