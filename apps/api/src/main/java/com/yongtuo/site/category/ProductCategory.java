package com.yongtuo.site.category;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("product_category")
public class ProductCategory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentId;
    private String nameZh;
    private String nameEn;
    private String slug;
    private String coverImage;
    private String descriptionZh;
    private String descriptionEn;
    private CategoryMode categoryMode;
    private Integer sortOrder;
    private CategoryStatus status;
    private Boolean showOnHome;
    private String seoTitleZh;
    private String seoTitleEn;
    private String seoDescriptionZh;
    private String seoDescriptionEn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
    public String getNameZh() { return nameZh; }
    public void setNameZh(String nameZh) { this.nameZh = nameZh; }
    public String getNameEn() { return nameEn; }
    public void setNameEn(String nameEn) { this.nameEn = nameEn; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }
    public String getDescriptionZh() { return descriptionZh; }
    public void setDescriptionZh(String descriptionZh) { this.descriptionZh = descriptionZh; }
    public String getDescriptionEn() { return descriptionEn; }
    public void setDescriptionEn(String descriptionEn) { this.descriptionEn = descriptionEn; }
    public CategoryMode getCategoryMode() { return categoryMode; }
    public void setCategoryMode(CategoryMode categoryMode) { this.categoryMode = categoryMode; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public CategoryStatus getStatus() { return status; }
    public void setStatus(CategoryStatus status) { this.status = status; }
    public Boolean getShowOnHome() { return showOnHome; }
    public void setShowOnHome(Boolean showOnHome) { this.showOnHome = showOnHome; }
    public String getSeoTitleZh() { return seoTitleZh; }
    public void setSeoTitleZh(String seoTitleZh) { this.seoTitleZh = seoTitleZh; }
    public String getSeoTitleEn() { return seoTitleEn; }
    public void setSeoTitleEn(String seoTitleEn) { this.seoTitleEn = seoTitleEn; }
    public String getSeoDescriptionZh() { return seoDescriptionZh; }
    public void setSeoDescriptionZh(String seoDescriptionZh) { this.seoDescriptionZh = seoDescriptionZh; }
    public String getSeoDescriptionEn() { return seoDescriptionEn; }
    public void setSeoDescriptionEn(String seoDescriptionEn) { this.seoDescriptionEn = seoDescriptionEn; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
