package com.yongtuo.site.attribute;

public record AdminAttributeOptionDto(Long id, Long attributeId, String valueCode,
                                      String labelZh, String labelEn, int sortOrder,
                                      AttributeStatus status) {}
