package com.yongtuo.site.product;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("product")
public class Product {
    @TableId(type = IdType.AUTO) private Long id;
    private Long categoryId;
    private String productCode;
    private String nameZh;
    private String nameEn;
    private String slug;
    private String summaryZh;
    private String summaryEn;
    private String descriptionZh;
    private String descriptionEn;
    private EnglishStatus englishStatus;
    private String coverImage;
    private Boolean isFeatured;
    private Integer sortOrder;
    private ProductStatus status;
    private String seoTitleZh;
    private String seoTitleEn;
    private String seoDescriptionZh;
    private String seoDescriptionEn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long value) { categoryId = value; }
    public String getProductCode() { return productCode; }
    public void setProductCode(String value) { productCode = value; }
    public String getNameZh() { return nameZh; }
    public void setNameZh(String value) { nameZh = value; }
    public String getNameEn() { return nameEn; }
    public void setNameEn(String value) { nameEn = value; }
    public String getSlug() { return slug; }
    public void setSlug(String value) { slug = value; }
    public String getSummaryZh() { return summaryZh; }
    public void setSummaryZh(String value) { summaryZh = value; }
    public String getSummaryEn() { return summaryEn; }
    public void setSummaryEn(String value) { summaryEn = value; }
    public String getDescriptionZh() { return descriptionZh; }
    public void setDescriptionZh(String value) { descriptionZh = value; }
    public String getDescriptionEn() { return descriptionEn; }
    public void setDescriptionEn(String value) { descriptionEn = value; }
    public EnglishStatus getEnglishStatus() { return englishStatus; }
    public void setEnglishStatus(EnglishStatus value) { englishStatus = value; }
    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String value) { coverImage = value; }
    public Boolean getIsFeatured() { return isFeatured; }
    public void setIsFeatured(Boolean value) { isFeatured = value; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer value) { sortOrder = value; }
    public ProductStatus getStatus() { return status; }
    public void setStatus(ProductStatus value) { status = value; }
    public String getSeoTitleZh() { return seoTitleZh; }
    public void setSeoTitleZh(String value) { seoTitleZh = value; }
    public String getSeoTitleEn() { return seoTitleEn; }
    public void setSeoTitleEn(String value) { seoTitleEn = value; }
    public String getSeoDescriptionZh() { return seoDescriptionZh; }
    public void setSeoDescriptionZh(String value) { seoDescriptionZh = value; }
    public String getSeoDescriptionEn() { return seoDescriptionEn; }
    public void setSeoDescriptionEn(String value) { seoDescriptionEn = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt = value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime value) { updatedAt = value; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime value) { deletedAt = value; }
}
