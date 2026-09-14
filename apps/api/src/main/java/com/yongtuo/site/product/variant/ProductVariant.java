package com.yongtuo.site.product.variant;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.yongtuo.site.product.ProductStatus;
import java.time.LocalDateTime;

@TableName("product_variant")
public class ProductVariant {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private String variantCode;
    private String nameZh;
    private String nameEn;
    private Integer sortOrder;
    private ProductStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long value) { id = value; }
    public Long getProductId() { return productId; }
    public void setProductId(Long value) { productId = value; }
    public String getVariantCode() { return variantCode; }
    public void setVariantCode(String value) { variantCode = value; }
    public String getNameZh() { return nameZh; }
    public void setNameZh(String value) { nameZh = value; }
    public String getNameEn() { return nameEn; }
    public void setNameEn(String value) { nameEn = value; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer value) { sortOrder = value; }
    public ProductStatus getStatus() { return status; }
    public void setStatus(ProductStatus value) { status = value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { createdAt = value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime value) { updatedAt = value; }
}
