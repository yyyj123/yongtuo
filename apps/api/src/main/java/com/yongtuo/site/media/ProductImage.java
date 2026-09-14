package com.yongtuo.site.media;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("product_image")
public class ProductImage {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private Long mediaId;
    private String imageUrl;
    private String altZh;
    private String altEn;
    private Integer sortOrder;
    private Boolean isCover;
    private LocalDateTime createdAt;
    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public Long getProductId() { return productId; }
    public void setProductId(Long value) { productId = value; }
    public Long getMediaId() { return mediaId; }
    public void setMediaId(Long value) { mediaId = value; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String value) { imageUrl = value; }
    public String getAltZh() { return altZh; }
    public void setAltZh(String value) { altZh = value; }
    public String getAltEn() { return altEn; }
    public void setAltEn(String value) { altEn = value; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer value) { sortOrder = value; }
    public Boolean getIsCover() { return isCover; }
    public void setIsCover(Boolean value) { isCover = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt = value; }
}
