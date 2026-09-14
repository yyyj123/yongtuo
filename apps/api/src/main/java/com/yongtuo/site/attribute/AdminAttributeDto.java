package com.yongtuo.site.attribute;

import java.util.List;

public record AdminAttributeDto(Long id, String nameZh, String nameEn, String code,
                               AttributeDataType dataType, String unit, boolean isGlobal,
                               boolean defaultFilterable, boolean defaultRequired, int sortOrder,
                               AttributeStatus status, List<AdminAttributeOptionDto> options) {}
