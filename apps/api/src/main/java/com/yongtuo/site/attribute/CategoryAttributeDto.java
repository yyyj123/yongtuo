package com.yongtuo.site.attribute;

import java.util.List;

public record CategoryAttributeDto(Long id, Long categoryId, Long attributeId, String nameZh,
                                   String nameEn, String code, AttributeDataType dataType,
                                   String unit, boolean isGlobal, boolean isFilterable,
                                   boolean isRequired, boolean showInDetail, int sortOrder,
                                   List<AdminAttributeOptionDto> options) {}
