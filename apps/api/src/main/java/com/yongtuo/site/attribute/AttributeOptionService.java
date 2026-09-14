package com.yongtuo.site.attribute;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AttributeOptionService {
    private final AttributeOptionMapper mapper;
    private final JdbcTemplate jdbc;

    public AttributeOptionService(AttributeOptionMapper mapper, JdbcTemplate jdbc) {
        this.mapper = mapper;
        this.jdbc = jdbc;
    }

    public Map<Long, List<AdminAttributeOptionDto>> byAttributeIds(Collection<Long> ids, boolean activeOnly) {
        if (ids == null || ids.isEmpty()) return Map.of();
        LambdaQueryWrapper<AttributeOption> query = new LambdaQueryWrapper<AttributeOption>()
                .in(AttributeOption::getAttributeId, ids)
                .orderByAsc(AttributeOption::getSortOrder, AttributeOption::getId);
        if (activeOnly) query.eq(AttributeOption::getStatus, AttributeStatus.ACTIVE);
        Map<Long, List<AdminAttributeOptionDto>> grouped = new HashMap<>();
        for (AttributeOption option : mapper.selectList(query)) {
            grouped.computeIfAbsent(option.getAttributeId(), ignored -> new java.util.ArrayList<>())
                    .add(toDto(option));
        }
        return grouped;
    }

    public void validate(AttributeDataType type, List<AdminAttributeOptionRequest> requests) {
        List<AdminAttributeOptionRequest> incoming = requests == null ? List.of() : requests;
        if ((type == AttributeDataType.TEXT || type == AttributeDataType.NUMBER) && !incoming.isEmpty()) {
            throw AttributeBusinessException.optionsNotAllowed();
        }
        if ((type == AttributeDataType.SELECT || type == AttributeDataType.MULTI_SELECT) && incoming.isEmpty()) {
            throw AttributeBusinessException.invalidOption();
        }
        Set<String> codes = new HashSet<>();
        for (AdminAttributeOptionRequest option : incoming) {
            if (option == null || option.valueCode() == null || !codes.add(option.valueCode())) {
                throw option == null || option.valueCode() == null
                        ? AttributeBusinessException.invalidOption() : AttributeBusinessException.duplicateOption();
            }
        }
    }

    public void insert(long attributeId, List<AdminAttributeOptionRequest> requests) {
        if (requests == null) return;
        for (AdminAttributeOptionRequest request : requests) {
            AttributeOption option = new AttributeOption();
            apply(option, attributeId, request);
            mapper.insert(option);
        }
    }

    public void reconcile(long attributeId, List<AdminAttributeOptionRequest> requests) {
        List<AdminAttributeOptionRequest> incoming = requests == null ? List.of() : requests;
        List<AttributeOption> current = mapper.selectList(new LambdaQueryWrapper<AttributeOption>()
                .eq(AttributeOption::getAttributeId, attributeId)
                .orderByAsc(AttributeOption::getId).last("FOR UPDATE"));
        Set<Long> retained = new HashSet<>();
        for (AdminAttributeOptionRequest request : incoming) {
            AttributeOption option;
            if (request.id() == null) {
                option = new AttributeOption();
                apply(option, attributeId, request);
                mapper.insert(option);
                continue;
            }
            option = current.stream().filter(candidate -> request.id().equals(candidate.getId())).findFirst().orElse(null);
            if (option == null || !retained.add(option.getId())) throw AttributeBusinessException.invalidOption();
            mapper.update(null, new LambdaUpdateWrapper<AttributeOption>().eq(AttributeOption::getId, option.getId())
                    .set(AttributeOption::getValueCode, request.valueCode())
                    .set(AttributeOption::getLabelZh, request.labelZh())
                    .set(AttributeOption::getLabelEn, request.labelEn())
                    .set(AttributeOption::getSortOrder, request.sortOrder())
                    .set(AttributeOption::getStatus, request.status()));
        }
        for (AttributeOption option : current) {
            if (retained.contains(option.getId())) continue;
            if (referenceCount(option.getId()) > 0) {
                mapper.update(null, new LambdaUpdateWrapper<AttributeOption>().eq(AttributeOption::getId, option.getId())
                        .set(AttributeOption::getStatus, AttributeStatus.INACTIVE));
            } else {
                try {
                    mapper.deleteById(option.getId());
                } catch (DataIntegrityViolationException exception) {
                    throw AttributeBusinessException.inUse();
                }
            }
        }
    }

    public long referenceCount(long optionId) {
        return jdbc.queryForObject("""
                SELECT (SELECT COUNT(*) FROM product_attribute_value WHERE option_id = ?)
                     + (SELECT COUNT(*) FROM product_variant_value WHERE option_id = ?)
                """, Long.class, optionId, optionId);
    }

    public List<AttributeOption> lockForAttribute(long attributeId) {
        return mapper.selectList(new LambdaQueryWrapper<AttributeOption>()
                .eq(AttributeOption::getAttributeId, attributeId)
                .orderByAsc(AttributeOption::getId).last("FOR UPDATE"));
    }

    public void deleteForAttribute(long attributeId) {
        mapper.delete(new LambdaQueryWrapper<AttributeOption>().eq(AttributeOption::getAttributeId, attributeId));
    }

    public static AdminAttributeOptionDto toDto(AttributeOption option) {
        return new AdminAttributeOptionDto(option.getId(), option.getAttributeId(), option.getValueCode(), option.getLabelZh(),
                option.getLabelEn(), option.getSortOrder() == null ? 0 : option.getSortOrder(), option.getStatus());
    }

    private static void apply(AttributeOption option, long attributeId, AdminAttributeOptionRequest request) {
        option.setAttributeId(attributeId); option.setValueCode(request.valueCode());
        option.setLabelZh(request.labelZh()); option.setLabelEn(request.labelEn());
        option.setSortOrder(request.sortOrder()); option.setStatus(request.status());
    }
}
