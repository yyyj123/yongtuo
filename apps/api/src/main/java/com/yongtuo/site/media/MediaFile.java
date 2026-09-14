package com.yongtuo.site.media;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("media_file")
public class MediaFile {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String storageKey;
    private String originalName;
    private String publicUrl;
    private String fileType;
    private String mimeType;
    private Long fileSize;
    private Integer width;
    private Integer height;
    private String checksum;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime deletedAt;
    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public String getStorageKey() { return storageKey; }
    public void setStorageKey(String value) { storageKey = value; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String value) { originalName = value; }
    public String getPublicUrl() { return publicUrl; }
    public void setPublicUrl(String value) { publicUrl = value; }
    public String getFileType() { return fileType; }
    public void setFileType(String value) { fileType = value; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String value) { mimeType = value; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long value) { fileSize = value; }
    public Integer getWidth() { return width; }
    public void setWidth(Integer value) { width = value; }
    public Integer getHeight() { return height; }
    public void setHeight(Integer value) { height = value; }
    public String getChecksum() { return checksum; }
    public void setChecksum(String value) { checksum = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt = value; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime value) { deletedAt = value; }
}
