package com.yongtuo.site.product;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yongtuo.site.attribute.AttributeDataType;
import com.yongtuo.site.attribute.AttributeOption;
import com.yongtuo.site.attribute.AttributeOptionMapper;
import com.yongtuo.site.attribute.CategoryAttributeDto;
import com.yongtuo.site.attribute.CategoryAttributeService;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.stereotype.Service;

/** Locks and validates the effective, data-driven product composition rules. */
@Service
public class ProductCompositionValidator {
    private final CategoryAttributeService categoryAttributes;
    private final AttributeOptionMapper options;

    public ProductCompositionValidator(CategoryAttributeService categoryAttributes, AttributeOptionMapper options) {
        this.categoryAttributes = categoryAttributes;
        this.options = options;
    }

    public ValidationResult validate(long categoryId, List<AdminProductAttributeRequest> productInputs,
                                     List<AdminProductVariantRequest> variantInputs) {
        List<CategoryAttributeDto> effective = categoryAttributes.lockEffectiveAttributesForProduct(categoryId);
        Map<Long, CategoryAttributeDto> rules = new LinkedHashMap<>();
        effective.forEach(rule -> rules.put(rule.attributeId(), rule));
        variantInputs.forEach(request -> {
            if (request == null) throw ProductBusinessException.compositionViolation();
        });
        lockOptionReferences(productInputs, variantInputs, rules);
        Map<Long, List<Long>> productMulti = validateValues(productInputs, rules);
        Set<String> codes = new HashSet<>();
        List<Map<Long, List<Long>>> variantMulti = new java.util.ArrayList<>();
        for (AdminProductVariantRequest request : variantInputs) {
            if (request.variantCode() == null || request.variantCode().isBlank()
                    || !codes.add(request.variantCode()) || request.status() == null) {
                throw ProductBusinessException.compositionViolation();
            }
            variantMulti.add(validateValues(request.values(), rules));
        }
        validateRequired(productInputs, variantInputs, rules);
        return new ValidationResult(rules, productMulti, variantMulti);
    }

    private Map<Long, List<Long>> validateValues(List<?> raw, Map<Long, CategoryAttributeDto> rules) {
        Map<Long, List<Long>> multi = new HashMap<>();
        Set<Long> ids = new HashSet<>();
        for (Object item : raw == null ? List.of() : raw) {
            if (item == null) throw ProductBusinessException.compositionViolation();
            Long id;
            String textZh;
            String textEn;
            BigDecimal number;
            Long optionId;
            List<Long> optionIds;
            if (item instanceof AdminProductAttributeRequest input) {
                id = input.attributeId(); textZh = input.valueZh(); textEn = input.valueEn(); number = input.numericValue();
                optionId = input.optionId(); optionIds = input.optionIds();
            } else if (item instanceof AdminProductVariantValueRequest input) {
                id = input.attributeId(); textZh = input.valueZh(); textEn = input.valueEn(); number = input.numericValue();
                optionId = input.optionId(); optionIds = input.optionIds();
            } else {
                throw ProductBusinessException.compositionViolation();
            }
            CategoryAttributeDto rule = rules.get(id);
            if (id == null || rule == null || !ids.add(id) || !validShape(rule, textZh, textEn, number, optionId, optionIds)) {
                throw ProductBusinessException.compositionViolation();
            }
            if (rule.dataType() == AttributeDataType.MULTI_SELECT) {
                List<Long> selected = optionIds == null ? List.of() : optionIds;
                if (new HashSet<>(selected).size() != selected.size() || selected.isEmpty()
                        || !activeOwnedOptions(rule.attributeId(), selected)) {
                    throw ProductBusinessException.compositionViolation();
                }
                multi.put(id, selected.stream().sorted().toList());
            } else if (rule.dataType() == AttributeDataType.SELECT
                    && !activeOwnedOptions(rule.attributeId(), List.of(optionId))) {
                throw ProductBusinessException.compositionViolation();
            }
        }
        return multi;
    }

    private void lockOptionReferences(List<AdminProductAttributeRequest> products,
                                      List<AdminProductVariantRequest> variants,
                                      Map<Long, CategoryAttributeDto> rules) {
        Map<Long, Set<Long>> references = new TreeMap<>();
        products.forEach(input -> {
            if (input == null) throw ProductBusinessException.compositionViolation();
            collectOptions(references, input.attributeId(), input.optionId(), input.optionIds());
        });
        variants.forEach(variant -> variant.values().forEach(input -> {
            if (input == null) throw ProductBusinessException.compositionViolation();
            collectOptions(references, input.attributeId(), input.optionId(), input.optionIds());
        }));
        references.forEach((attributeId, ids) -> {
            if (!rules.containsKey(attributeId)) throw ProductBusinessException.compositionViolation();
            if (ids.isEmpty()) return;
            options.selectList(new LambdaQueryWrapper<AttributeOption>()
                    .eq(AttributeOption::getAttributeId, attributeId).in(AttributeOption::getId, ids)
                    .orderByAsc(AttributeOption::getId).last("FOR UPDATE"));
        });
    }

    private static void collectOptions(Map<Long, Set<Long>> references, Long attributeId, Long optionId,
                                       List<Long> optionIds) {
        if (attributeId == null) return;
        Set<Long> ids = references.computeIfAbsent(attributeId, ignored -> new HashSet<>());
        if (optionId != null) ids.add(optionId);
        if (optionIds != null) optionIds.stream().filter(java.util.Objects::nonNull).forEach(ids::add);
    }

    private void validateRequired(List<AdminProductAttributeRequest> products,
                                  List<AdminProductVariantRequest> variants,
                                  Map<Long, CategoryAttributeDto> rules) {
        Set<Long> productIds = products.stream().map(AdminProductAttributeRequest::attributeId)
                .collect(java.util.stream.Collectors.toSet());
        if (variants.isEmpty()) {
            if (rules.values().stream().anyMatch(rule -> rule.isRequired() && !productIds.contains(rule.attributeId()))) {
                throw ProductBusinessException.compositionViolation();
            }
            return;
        }
        for (CategoryAttributeDto rule : rules.values()) {
            if (!rule.isRequired() || productIds.contains(rule.attributeId())) continue;
            if (variants.stream().anyMatch(variant -> variant.values().stream()
                    .noneMatch(value -> rule.attributeId().equals(value.attributeId())))) {
                throw ProductBusinessException.compositionViolation();
            }
        }
    }

    private boolean validShape(CategoryAttributeDto rule, String zh, String en, BigDecimal number,
                               Long optionId, List<Long> optionIds) {
        List<Long> ids = optionIds == null ? List.of() : optionIds;
        return switch (rule.dataType()) {
            case TEXT -> (hasText(zh) || hasText(en)) && number == null && optionId == null && ids.isEmpty();
            case NUMBER -> number != null && !hasText(zh) && !hasText(en) && optionId == null && ids.isEmpty();
            case SELECT -> optionId != null && number == null && !hasText(zh) && !hasText(en) && ids.isEmpty();
            case MULTI_SELECT -> !ids.isEmpty() && number == null && !hasText(zh) && !hasText(en) && optionId == null;
        };
    }

    private boolean activeOwnedOptions(long attributeId, List<Long> ids) {
        if (ids == null || ids.isEmpty()) return false;
        List<AttributeOption> selected = options.selectList(new LambdaQueryWrapper<AttributeOption>()
                .eq(AttributeOption::getAttributeId, attributeId).in(AttributeOption::getId, ids)
                .eq(AttributeOption::getStatus, com.yongtuo.site.attribute.AttributeStatus.ACTIVE)
                .orderByAsc(AttributeOption::getId).last("FOR UPDATE"));
        return selected.size() == new HashSet<>(ids).size();
    }

    private static boolean hasText(String value) { return value != null && !value.isBlank(); }

    public record ValidationResult(Map<Long, CategoryAttributeDto> rules,
                                   Map<Long, List<Long>> productMulti,
                                   List<Map<Long, List<Long>>> variantMulti) {}
}
