package com.yongtuo.site.product.query;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.yongtuo.site.media.MediaFile;
import com.yongtuo.site.media.MediaFileMapper;
import com.yongtuo.site.media.ObjectStorageService;
import com.yongtuo.site.media.ProductAttachment;
import com.yongtuo.site.media.ProductAttachmentMapper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class PublicProductAttachmentAssembler {
    private final ProductAttachmentMapper attachments;
    private final MediaFileMapper mediaFiles;
    private final ObjectStorageService storage;

    public PublicProductAttachmentAssembler(ProductAttachmentMapper attachments, MediaFileMapper mediaFiles,
                                            ObjectStorageService storage) {
        this.attachments = attachments;
        this.mediaFiles = mediaFiles;
        this.storage = storage;
    }

    public List<PublicProductAttachmentDto> assemble(long productId, boolean english) {
        List<ProductAttachment> publicAttachments = attachments.selectList(
                new LambdaQueryWrapper<ProductAttachment>()
                        .eq(ProductAttachment::getProductId, productId)
                        .eq(ProductAttachment::getIsPublic, true)
                        .orderByAsc(ProductAttachment::getSortOrder, ProductAttachment::getId));
        if (publicAttachments.isEmpty()) return List.of();

        List<Long> mediaIds = publicAttachments.stream().map(ProductAttachment::getMediaId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, MediaFile> activeMedia = mediaIds.isEmpty() ? Map.of()
                : mediaFiles.selectList(new LambdaQueryWrapper<MediaFile>()
                                .in(MediaFile::getId, mediaIds)
                                .eq(MediaFile::getStatus, "ACTIVE")
                                .isNull(MediaFile::getDeletedAt))
                        .stream().collect(Collectors.toMap(MediaFile::getId, Function.identity()));

        return publicAttachments.stream().map(attachment -> new PublicProductAttachmentDto(
                english ? attachment.getDisplayNameEn() : attachment.getDisplayNameZh(),
                attachment.getFileType(), downloadUrl(attachment, activeMedia))).toList();
    }

    private String downloadUrl(ProductAttachment attachment, Map<Long, MediaFile> activeMedia) {
        if (!Boolean.TRUE.equals(attachment.getAllowDownload())) return null;
        if (attachment.getMediaId() == null) return null;
        MediaFile media = activeMedia.get(attachment.getMediaId());
        if (media == null) return null;
        return storage.resolveDownloadUrl(media.getStorageKey(), media.getPublicUrl()).orElse(null);
    }
}
