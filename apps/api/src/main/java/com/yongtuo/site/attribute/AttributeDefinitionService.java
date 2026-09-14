package com.yongtuo.site.attribute;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttributeDefinitionService {
    private final AttributeDefinitionMapper mapper;
    private final AttributeOptionService optionService;
    private final JdbcTemplate jdbc;

    public AttributeDefinitionService(AttributeDefinitionMapper mapper, AttributeOptionService optionService,
                                      JdbcTemplate jdbc) {
        this.mapper = mapper;
        this.optionService = optionService;
        this.jdbc = jdbc;
    }

    @Transactional(readOnly = true)
    public List<AdminAttributeDto> list() {
        List<AttributeDefinition> definitions = mapper.selectList(new LambdaQueryWrapper<AttributeDefinition>()
                .orderByAsc(AttributeDefinition::getSortOrder, AttributeDefinition::getId));
        Map<Long, List<AdminAttributeOptionDto>> options = optionService.byAttributeIds(
                definitions.stream().map(AttributeDefinition::getId).toList(), false);
        return definitions.stream().map(definition -> toDto(definition, options)).toList();
    }

    @Transactional(readOnly = true)
    public AdminAttributeDto get(long id) {
        AttributeDefinition definition = require(id);
        return toDto(definition, optionService.byAttributeIds(List.of(id), false));
    }

    @Transactional
    public AdminAttributeDto create(AdminAttributeWriteRequest request) {
        optionService.validate(request.dataType(), request.options());
        AttributeDefinition definition = new AttributeDefinition();
        apply(definition, request);
        try {
            mapper.insert(definition);
            optionService.insert(definition.getId(), request.options());
        } catch (DuplicateKeyException exception) {
            throw mapDuplicate(exception);
        }
        return get(definition.getId());
    }

    @Transactional
    public AdminAttributeDto update(long id, AdminAttributeWriteRequest request) {
        AttributeDefinition current = requireForUpdate(id);
        optionService.validate(request.dataType(), request.options());
        if (current.getDataType() != request.dataType()
                && jdbc.queryForObject("SELECT COUNT(*) FROM attribute_option WHERE attribute_id = ?", Integer.class, id) > 0) {
            throw AttributeBusinessException.typeChangeNotAllowed();
        }
        try {
            mapper.update(null, new LambdaUpdateWrapper<AttributeDefinition>().eq(AttributeDefinition::getId, id)
                    .set(AttributeDefinition::getNameZh, request.nameZh())
                    .set(AttributeDefinition::getNameEn, request.nameEn())
                    .set(AttributeDefinition::getCode, request.code())
                    .set(AttributeDefinition::getDataType, request.dataType())
                    .set(AttributeDefinition::getUnit, request.unit())
                    .set(AttributeDefinition::getIsGlobal, request.isGlobal())
                    .set(AttributeDefinition::getDefaultFilterable, request.defaultFilterable())
                    .set(AttributeDefinition::getDefaultRequired, request.defaultRequired())
                    .set(AttributeDefinition::getSortOrder, request.sortOrder())
                    .set(AttributeDefinition::getStatus, request.status()));
            optionService.reconcile(id, request.options());
        } catch (DuplicateKeyException exception) {
            throw mapDuplicate(exception);
        } catch (DataIntegrityViolationException exception) {
            throw AttributeBusinessException.inUse();
        }
        return get(id);
    }

    @Transactional
    public void delete(long id) {
        requireForUpdate(id);
        optionService.lockForAttribute(id);
        long references = jdbc.queryForObject("""
                SELECT (SELECT COUNT(*) FROM category_attribute WHERE attribute_id = ?)
                     + (SELECT COUNT(*) FROM product_attribute_value WHERE attribute_id = ?)
                     + (SELECT COUNT(*) FROM product_variant_value WHERE attribute_id = ?)
                     + (SELECT COUNT(*) FROM product_attribute_value pav JOIN attribute_option ao ON ao.id = pav.option_id WHERE ao.attribute_id = ?)
                     + (SELECT COUNT(*) FROM product_variant_value pvv JOIN attribute_option ao ON ao.id = pvv.option_id WHERE ao.attribute_id = ?)
                """, Long.class, id, id, id, id, id);
        if (references > 0) throw AttributeBusinessException.inUse();
        try {
            optionService.deleteForAttribute(id);
            mapper.deleteById(id);
        } catch (DataIntegrityViolationException exception) {
            throw AttributeBusinessException.inUse();
        }
    }

    private AttributeDefinition require(long id) {
        AttributeDefinition definition = mapper.selectById(id);
        if (definition == null) throw AttributeBusinessException.notFound();
        return definition;
    }

    private AttributeDefinition requireForUpdate(long id) {
        AttributeDefinition definition = mapper.selectOne(new LambdaQueryWrapper<AttributeDefinition>()
                .eq(AttributeDefinition::getId, id).last("FOR UPDATE"));
        if (definition == null) throw AttributeBusinessException.notFound();
        return definition;
    }

    private static void apply(AttributeDefinition definition, AdminAttributeWriteRequest request) {
        definition.setNameZh(request.nameZh()); definition.setNameEn(request.nameEn());
        definition.setCode(request.code()); definition.setDataType(request.dataType());
        definition.setUnit(request.unit()); definition.setIsGlobal(request.isGlobal());
        definition.setDefaultFilterable(request.defaultFilterable()); definition.setDefaultRequired(request.defaultRequired());
        definition.setSortOrder(request.sortOrder()); definition.setStatus(request.status());
    }

    private static AdminAttributeDto toDto(AttributeDefinition definition,
                                            Map<Long, List<AdminAttributeOptionDto>> options) {
        return new AdminAttributeDto(definition.getId(), definition.getNameZh(), definition.getNameEn(), definition.getCode(),
                definition.getDataType(), definition.getUnit(), Boolean.TRUE.equals(definition.getIsGlobal()),
                Boolean.TRUE.equals(definition.getDefaultFilterable()), Boolean.TRUE.equals(definition.getDefaultRequired()),
                definition.getSortOrder() == null ? 0 : definition.getSortOrder(), definition.getStatus(),
                options.getOrDefault(definition.getId(), List.of()));
    }

    private static AttributeBusinessException mapDuplicate(DuplicateKeyException exception) {
        Throwable cause = exception;
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null) {
                if (message.contains("uk_attribute_definition_code")) return AttributeBusinessException.duplicateCode();
                if (message.contains("uk_attribute_option_attribute_value")) return AttributeBusinessException.duplicateOption();
            }
            Throwable next = cause.getCause();
            if (next == cause) break;
            cause = next;
        }
        return AttributeBusinessException.duplicateOption();
    }
}
