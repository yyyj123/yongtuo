package com.yongtuo.site.product;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yongtuo.site.attribute.ProductAttributeValue;
import com.yongtuo.site.attribute.ProductAttributeValueMapper;
import com.yongtuo.site.product.variant.ProductVariant;
import com.yongtuo.site.product.variant.ProductVariantMapper;
import com.yongtuo.site.product.variant.ProductVariantValue;
import com.yongtuo.site.product.variant.ProductVariantValueMapper;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

/** Transactional persistence boundary; ProductService owns the outer transaction. */
@Service
public class ProductCompositionService {
    private final ProductAttributeValueMapper productValues;
    private final ProductVariantMapper variants;
    private final ProductVariantValueMapper variantValues;
    private final ProductCompositionValidator validator;
    private final ProductCompositionAssembler assembler;

    public ProductCompositionService(ProductAttributeValueMapper productValues, ProductVariantMapper variants,
                                     ProductVariantValueMapper variantValues, ProductCompositionValidator validator,
                                     ProductCompositionAssembler assembler) {
        this.productValues = productValues;
        this.variants = variants;
        this.variantValues = variantValues;
        this.validator = validator;
        this.assembler = assembler;
    }

    public void replace(long productId, long categoryId, List<AdminProductAttributeRequest> attributes,
                        List<AdminProductVariantRequest> variantRequests) {
        List<AdminProductAttributeRequest> productInputs = attributes == null ? List.of() : attributes;
        List<AdminProductVariantRequest> variantInputs = variantRequests == null ? List.of() : variantRequests;
        ProductCompositionValidator.ValidationResult validation = validator.validate(categoryId, productInputs, variantInputs);
        try {
            productValues.delete(new LambdaQueryWrapper<ProductAttributeValue>()
                    .eq(ProductAttributeValue::getProductId, productId));
            variants.delete(new LambdaQueryWrapper<ProductVariant>()
                    .eq(ProductVariant::getProductId, productId));
            for (AdminProductAttributeRequest input : productInputs) {
                List<Long> selected = validation.productMulti().getOrDefault(input.attributeId(), List.of());
                if (selected.isEmpty()) insertProductValue(productId, input, null);
                else for (Long optionId : selected) insertProductValue(productId, input, optionId);
            }
            for (int index = 0; index < variantInputs.size(); index++) {
                AdminProductVariantRequest request = variantInputs.get(index);
                ProductVariant variant = new ProductVariant();
                variant.setProductId(productId);
                variant.setVariantCode(request.variantCode());
                variant.setNameZh(request.nameZh());
                variant.setNameEn(request.nameEn());
                variant.setSortOrder(request.sortOrder());
                variant.setStatus(request.status());
                variants.insert(variant);
                Map<Long, List<Long>> multi = validation.variantMulti().get(index);
                for (AdminProductVariantValueRequest input : request.values()) {
                    List<Long> selected = multi.getOrDefault(input.attributeId(), List.of());
                    if (selected.isEmpty()) insertVariantValue(variant.getId(), input, null);
                    else for (Long optionId : selected) insertVariantValue(variant.getId(), input, optionId);
                }
            }
        } catch (DataAccessException exception) {
            throw ProductBusinessException.compositionViolation();
        }
    }

    public CompositionDto assemble(long productId, long categoryId) {
        return assembler.assemble(productId, categoryId);
    }

    private void insertProductValue(long productId, AdminProductAttributeRequest input, Long optionId) {
        ProductAttributeValue value = new ProductAttributeValue();
        value.setProductId(productId);
        value.setAttributeId(input.attributeId());
        value.setValueZh(input.valueZh());
        value.setValueEn(input.valueEn());
        value.setNumericValue(input.numericValue());
        value.setOptionId(optionId);
        value.setValueKey(input.optionIds().isEmpty() ? 0L : optionId);
        value.setSortOrder(input.sortOrder());
        productValues.insert(value);
    }

    private void insertVariantValue(long variantId, AdminProductVariantValueRequest input, Long optionId) {
        ProductVariantValue value = new ProductVariantValue();
        value.setVariantId(variantId);
        value.setAttributeId(input.attributeId());
        value.setValueZh(input.valueZh());
        value.setValueEn(input.valueEn());
        value.setNumericValue(input.numericValue());
        value.setOptionId(optionId);
        value.setValueKey(input.optionIds().isEmpty() ? 0L : optionId);
        variantValues.insert(value);
    }

    public record CompositionDto(List<ProductAttributeDto> attributes, List<ProductVariantDto> variants) {}
}
