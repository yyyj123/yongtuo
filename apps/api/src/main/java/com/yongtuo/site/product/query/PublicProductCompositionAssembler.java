package com.yongtuo.site.product.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yongtuo.site.attribute.AdminAttributeOptionDto;
import com.yongtuo.site.attribute.AttributeDataType;
import com.yongtuo.site.attribute.CategoryAttributeDto;
import com.yongtuo.site.attribute.CategoryAttributeService;
import com.yongtuo.site.attribute.ProductAttributeValue;
import com.yongtuo.site.attribute.ProductAttributeValueMapper;
import com.yongtuo.site.product.ProductStatus;
import com.yongtuo.site.product.variant.ProductVariant;
import com.yongtuo.site.product.variant.ProductVariantMapper;
import com.yongtuo.site.product.variant.ProductVariantValue;
import com.yongtuo.site.product.variant.ProductVariantValueMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/** Builds a locale-specific public view without exposing admin composition DTOs. */
@Service
public class PublicProductCompositionAssembler {
    private final ProductAttributeValueMapper productValues;
    private final ProductVariantMapper variants;
    private final ProductVariantValueMapper variantValues;
    private final CategoryAttributeService categoryAttributes;
    private final PublicProductAttachmentAssembler attachments;
    private final com.yongtuo.site.casestudy.CaseStudyService cases;

    public PublicProductCompositionAssembler(ProductAttributeValueMapper productValues, ProductVariantMapper variants,
                                             ProductVariantValueMapper variantValues,
                                             CategoryAttributeService categoryAttributes,
                                             PublicProductAttachmentAssembler attachments,
                                             com.yongtuo.site.casestudy.CaseStudyService cases) {
        this.productValues = productValues;
        this.variants = variants;
        this.variantValues = variantValues;
        this.categoryAttributes = categoryAttributes;
        this.attachments = attachments;
        this.cases = cases;
    }

    public Composition assemble(long productId, long categoryId, boolean english) {
        List<CategoryAttributeDto> visibleRules = categoryAttributes.getCategoryAttributes(categoryId).stream()
                .filter(CategoryAttributeDto::showInDetail).toList();
        Map<Long, CategoryAttributeDto> rules = visibleRules.stream()
                .collect(Collectors.toMap(CategoryAttributeDto::attributeId, Function.identity()));
        Set<Long> attributeIds = rules.keySet();

        List<PublicProductAttributeDto> attributes = attributeIds.isEmpty() ? List.of()
                : aggregate(productValues.selectList(new LambdaQueryWrapper<ProductAttributeValue>()
                        .eq(ProductAttributeValue::getProductId, productId)
                        .in(ProductAttributeValue::getAttributeId, attributeIds)
                        .orderByAsc(ProductAttributeValue::getAttributeId, ProductAttributeValue::getId)),
                        ProductAttributeValue::getAttributeId, ProductAttributeValue::getValueZh,
                        ProductAttributeValue::getValueEn, ProductAttributeValue::getNumericValue,
                        ProductAttributeValue::getOptionId, visibleRules, english);

        List<ProductVariant> publicVariants = variants.selectList(new LambdaQueryWrapper<ProductVariant>()
                .eq(ProductVariant::getProductId, productId)
                .eq(ProductVariant::getStatus, ProductStatus.PUBLISHED)
                .orderByAsc(ProductVariant::getSortOrder, ProductVariant::getId));
        List<PublicProductAttachmentDto> publicAttachments = attachments.assemble(productId, english);
        var relatedCases = cases.related(productId, categoryId, english);
        if (publicVariants.isEmpty()) return new Composition(attributes, List.of(), publicAttachments, relatedCases);

        List<Long> variantIds = publicVariants.stream().map(ProductVariant::getId).toList();
        Map<Long, List<ProductVariantValue>> byVariant = variantValues.selectList(
                        new LambdaQueryWrapper<ProductVariantValue>().in(ProductVariantValue::getVariantId, variantIds)
                                .orderByAsc(ProductVariantValue::getVariantId, ProductVariantValue::getAttributeId,
                                        ProductVariantValue::getId))
                .stream().collect(Collectors.groupingBy(ProductVariantValue::getVariantId, LinkedHashMap::new,
                        Collectors.toList()));
        List<PublicProductVariantDto> result = new ArrayList<>();
        for (ProductVariant variant : publicVariants) {
            List<PublicProductAttributeDto> values = attributeIds.isEmpty() ? List.of()
                    : aggregate(byVariant.getOrDefault(variant.getId(), List.of()).stream()
                                    .filter(value -> attributeIds.contains(value.getAttributeId())).toList(),
                            ProductVariantValue::getAttributeId, ProductVariantValue::getValueZh,
                            ProductVariantValue::getValueEn, ProductVariantValue::getNumericValue,
                            ProductVariantValue::getOptionId, visibleRules, english);
            result.add(new PublicProductVariantDto(variant.getVariantCode(),
                    english ? variant.getNameEn() : variant.getNameZh(), values));
        }
        return new Composition(attributes, result, publicAttachments, relatedCases);
    }

    private static <T> List<PublicProductAttributeDto> aggregate(
            List<T> values, Function<T, Long> attributeId, Function<T, String> valueZh,
            Function<T, String> valueEn, Function<T, java.math.BigDecimal> numericValue,
            Function<T, Long> optionId, List<CategoryAttributeDto> orderedRules, boolean english) {
        Map<Long, List<T>> grouped = values.stream().collect(Collectors.groupingBy(attributeId,
                LinkedHashMap::new, Collectors.toList()));
        List<PublicProductAttributeDto> result = new ArrayList<>();
        for (CategoryAttributeDto rule : orderedRules) {
            List<T> group = grouped.get(rule.attributeId());
            if (group == null || group.isEmpty()) continue;
            T first = group.getFirst();
            Set<Long> selected = group.stream().map(optionId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
            List<PublicProductOptionDto> options = rule.options().stream().filter(option -> selected.contains(option.id()))
                    .map(option -> publicOption(option, english)).toList();
            String value = rule.dataType() == AttributeDataType.TEXT
                    ? (english ? valueEn.apply(first) : valueZh.apply(first)) : null;
            result.add(new PublicProductAttributeDto(rule.code(), english ? rule.nameEn() : rule.nameZh(),
                    rule.dataType(), rule.unit(), value, numericValue.apply(first), options));
        }
        return List.copyOf(result);
    }

    private static PublicProductOptionDto publicOption(AdminAttributeOptionDto option, boolean english) {
        return new PublicProductOptionDto(option.valueCode(), english ? option.labelEn() : option.labelZh());
    }

    public record Composition(List<PublicProductAttributeDto> attributes,
                              List<PublicProductVariantDto> variants,
                              List<PublicProductAttachmentDto> publicAttachments,
                              List<com.yongtuo.site.casestudy.CaseSummary> relatedCases) {
        public Composition(List<PublicProductAttributeDto> attributes, List<PublicProductVariantDto> variants,
                           List<PublicProductAttachmentDto> attachments) {
            this(attributes, variants, attachments, List.of());
        }
        public Composition {
            attributes = List.copyOf(attributes);
            variants = List.copyOf(variants);
            publicAttachments = List.copyOf(publicAttachments);
            relatedCases = List.copyOf(relatedCases);
        }
    }
}
