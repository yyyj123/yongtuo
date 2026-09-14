package com.yongtuo.site.product.query;

/** Public attachment projection intentionally excludes storage and permission metadata. */
public record PublicProductAttachmentDto(String title, String fileType, String downloadUrl) {}
