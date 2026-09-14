package com.yongtuo.site.media;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("product_attachment")
public class ProductAttachment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private Long mediaId;
    private String fileName;
    private String displayNameZh;
    private String displayNameEn;
    private String fileUrl;
    private String fileType;
    private Boolean isPublic;
    private Boolean allowDownload;
    private Integer sortOrder;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public Long getProductId() { return productId; }
    public void setProductId(Long value) { productId = value; }
    public Long getMediaId() { return mediaId; }
    public void setMediaId(Long value) { mediaId = value; }
    public String getFileName() { return fileName; }
    public void setFileName(String value) { fileName = value; }
    public String getDisplayNameZh() { return displayNameZh; }
    public void setDisplayNameZh(String value) { displayNameZh = value; }
    public String getDisplayNameEn() { return displayNameEn; }
    public void setDisplayNameEn(String value) { displayNameEn = value; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String value) { fileUrl = value; }
    public String getFileType() { return fileType; }
    public void setFileType(String value) { fileType = value; }
    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean value) { isPublic = value; }
    public Boolean getAllowDownload() { return allowDownload; }
    public void setAllowDownload(Boolean value) { allowDownload = value; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer value) { sortOrder = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt = value; }
}
