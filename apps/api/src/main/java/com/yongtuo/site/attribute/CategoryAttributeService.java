package com.yongtuo.site.attribute;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yongtuo.site.category.CategoryBusinessException;
import com.yongtuo.site.category.CategoryService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryAttributeService {
    private final AttributeDefinitionMapper definitions;
    private final CategoryAttributeMapper bindings;
    private final AttributeOptionService optionService;
    private final CategoryService categories;
    private final JdbcTemplate jdbc;

    public CategoryAttributeService(AttributeDefinitionMapper definitions, CategoryAttributeMapper bindings,
                                    AttributeOptionService optionService, CategoryService categories,
                                    JdbcTemplate jdbc) {
        this.definitions = definitions;
        this.bindings = bindings;
        this.optionService = optionService;
        this.categories = categories;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<CategoryAttributeDto> getCategoryAttributes(long categoryId) {
        return getCategoryAttributes(categoryId, false);
    }

    private List<CategoryAttributeDto> getCategoryAttributes(long categoryId, boolean lockDefinitions) {
        requireActiveCategory(categoryId);
        LambdaQueryWrapper<AttributeDefinition> activeQuery = new LambdaQueryWrapper<AttributeDefinition>()
                .eq(AttributeDefinition::getStatus, AttributeStatus.ACTIVE);
        if (lockDefinitions) activeQuery.orderByAsc(AttributeDefinition::getId).last("FOR UPDATE");
        List<AttributeDefinition> active = definitions.selectList(activeQuery);
        List<CategoryAttribute> explicit = bindings.selectList(new LambdaQueryWrapper<CategoryAttribute>()
                .eq(CategoryAttribute::getCategoryId, categoryId));
        Map<Long, CategoryAttribute> byAttribute = new HashMap<>();
        for (CategoryAttribute binding : explicit) byAttribute.put(binding.getAttributeId(), binding);
        Map<Long, List<AdminAttributeOptionDto>> options = optionService.byAttributeIds(
                active.stream().map(AttributeDefinition::getId).toList(), true);
        List<CategoryAttributeDto> result = new ArrayList<>();
        for (AttributeDefinition definition : active) {
            CategoryAttribute binding = byAttribute.get(definition.getId());
            if (!Boolean.TRUE.equals(definition.getIsGlobal()) && binding == null) continue;
            result.add(toDto(definition, binding, categoryId, options));
        }
        result.sort(Comparator.comparingInt(CategoryAttributeDto::sortOrder)
                .thenComparing(CategoryAttributeDto::code));
        return List.copyOf(result);
    }

    /** Locks the category first, then all ACTIVE definitions in ascending id order. */
    @Transactional
    public List<CategoryAttributeDto> lockEffectiveAttributesForProduct(long categoryId) {
        try {
            categories.lockActiveForAttributeBinding(categoryId);
        } catch (CategoryBusinessException exception) {
            throw AttributeBusinessException.invalidReference();
        }
        definitions.selectList(new LambdaQueryWrapper<AttributeDefinition>()
                .eq(AttributeDefinition::getStatus, AttributeStatus.ACTIVE)
                .orderByAsc(AttributeDefinition::getId).last("FOR UPDATE"));
        return getCategoryAttributes(categoryId, true);
    }

    @Transactional
    public List<CategoryAttributeDto> replaceCategoryBindings(long categoryId,
                                                                List<CategoryAttributeBindingRequest> requests) {
        try {
            categories.lockActiveForAttributeBinding(categoryId);
        } catch (CategoryBusinessException exception) {
            throw AttributeBusinessException.invalidReference();
        }
        List<CategoryAttributeBindingRequest> requested = requests == null ? List.of() : requests;
        Set<Long> ids = new HashSet<>();
        for (CategoryAttributeBindingRequest request : requested) {
            if (request == null || request.attributeId() == null || !ids.add(request.attributeId())) {
                throw AttributeBusinessException.duplicateBinding();
            }
        }
        List<AttributeDefinition> locked = lockDefinitions(ids);
        if (locked.size() != ids.size() || locked.stream().anyMatch(definition -> definition.getStatus() != AttributeStatus.ACTIVE)) {
            throw AttributeBusinessException.invalidReference();
        }
        bindings.delete(new LambdaQueryWrapper<CategoryAttribute>().eq(CategoryAttribute::getCategoryId, categoryId));
        try {
            for (CategoryAttributeBindingRequest request : requested) {
                CategoryAttribute binding = new CategoryAttribute();
                binding.setCategoryId(categoryId); binding.setAttributeId(request.attributeId());
                binding.setIsFilterable(request.isFilterable()); binding.setIsRequired(request.isRequired());
                binding.setShowInDetail(request.showInDetail()); binding.setSortOrder(request.sortOrder());
                bindings.insert(binding);
            }
        } catch (DuplicateKeyException exception) {
            throw AttributeBusinessException.duplicateBinding();
        }
        return getCategoryAttributes(categoryId);
    }

    public List<CategoryAttributeDto> replaceCategoryAttributes(long categoryId,
                                                                  List<CategoryAttributeBindingRequest> requests) {
        return replaceCategoryBindings(categoryId, requests);
    }

    private List<AttributeDefinition> lockDefinitions(Set<Long> ids) {
        if (ids.isEmpty()) return List.of();
        return definitions.selectList(new LambdaQueryWrapper<AttributeDefinition>()
                .in(AttributeDefinition::getId, ids)
                .orderByAsc(AttributeDefinition::getId).last("FOR UPDATE"));
    }

    private void requireActiveCategory(long id) {
        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM product_category WHERE id = ? AND status = 'ACTIVE' AND deleted_at IS NULL", Integer.class, id);
        if (count == null || count == 0) throw AttributeBusinessException.invalidReference();
    }

    private static CategoryAttributeDto toDto(AttributeDefinition definition, CategoryAttribute binding,
                                               long categoryId, Map<Long, List<AdminAttributeOptionDto>> options) {
        boolean filterable = binding == null ? Boolean.TRUE.equals(definition.getDefaultFilterable()) : Boolean.TRUE.equals(binding.getIsFilterable());
        boolean required = binding == null ? Boolean.TRUE.equals(definition.getDefaultRequired()) : Boolean.TRUE.equals(binding.getIsRequired());
        boolean detail = binding != null && Boolean.TRUE.equals(binding.getShowInDetail());
        int order = binding == null ? definition.getSortOrder() : binding.getSortOrder();
        return new CategoryAttributeDto(binding == null ? null : binding.getId(), categoryId, definition.getId(),
                definition.getNameZh(), definition.getNameEn(), definition.getCode(), definition.getDataType(), definition.getUnit(),
                Boolean.TRUE.equals(definition.getIsGlobal()), filterable, required, detail, order,
                options.getOrDefault(definition.getId(), List.of()));
    }
}
