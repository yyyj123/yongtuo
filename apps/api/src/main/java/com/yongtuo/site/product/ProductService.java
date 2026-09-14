package com.yongtuo.site.product;

import static com.yongtuo.site.content.RichTextSanitizer.clean;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yongtuo.site.category.CategoryService;
import com.yongtuo.site.category.CategoryBusinessException;
import com.yongtuo.site.site.UrlRedirectService;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private final ProductMapper mapper;
    private final CategoryService categoryService;
    private final ProductCompositionService composition;
    private final UrlRedirectService redirects;

    public ProductService(ProductMapper mapper, CategoryService categoryService,
                          ProductCompositionService composition, UrlRedirectService redirects) {
        this.mapper = mapper;
        this.categoryService = categoryService;
        this.composition = composition;
        this.redirects = redirects;
    }

    @Transactional
    public ProductDto create(AdminProductCreateRequest request) {
        requireCategory(request.categoryId());
        Product product = new Product();
        apply(product, request);
        try {
            mapper.insert(product);
        } catch (DuplicateKeyException exception) {
            throw mapDuplicate(exception);
        }
        composition.replace(product.getId(), request.categoryId(), request.attributes(), request.variants());
        boolean chinesePublic = request.status() == ProductStatus.PUBLISHED;
        redirects.prepareProductCanonical(request.slug(), chinesePublic,
                chinesePublic && request.englishStatus() == EnglishStatus.CONFIRMED);
        return toDto(product, composition.assemble(product.getId(), request.categoryId()));
    }

    @Transactional
    public ProductDto update(long id, AdminProductUpdateRequest request) {
        requireCategory(request.categoryId());
        Product previous = requireLiveForUpdate(id);
        try {
            mapper.update(null, new LambdaUpdateWrapper<Product>()
                    .eq(Product::getId, id).isNull(Product::getDeletedAt)
                    .set(Product::getCategoryId, request.categoryId())
                    .set(Product::getProductCode, request.productCode())
                    .set(Product::getSlug, request.slug())
                    .set(Product::getNameZh, request.nameZh()).set(Product::getNameEn, request.nameEn())
                    .set(Product::getSummaryZh, request.summaryZh()).set(Product::getSummaryEn, request.summaryEn())
                    .set(Product::getDescriptionZh, clean(request.descriptionZh())).set(Product::getDescriptionEn, clean(request.descriptionEn()))
                    .set(Product::getEnglishStatus, request.englishStatus())
                    .set(Product::getCoverImage, request.coverImage()).set(Product::getIsFeatured, request.isFeatured())
                    .set(Product::getSortOrder, request.sortOrder()).set(Product::getStatus, request.status())
                    .set(Product::getSeoTitleZh, request.seoTitleZh()).set(Product::getSeoTitleEn, request.seoTitleEn())
                    .set(Product::getSeoDescriptionZh, request.seoDescriptionZh())
                    .set(Product::getSeoDescriptionEn, request.seoDescriptionEn()));
        } catch (DuplicateKeyException exception) {
            throw mapDuplicate(exception);
        }
        Product updated = requireLive(id);
        composition.replace(updated.getId(), request.categoryId(), request.attributes(), request.variants());
        boolean chinesePublic = request.status() == ProductStatus.PUBLISHED;
        boolean englishPublic = chinesePublic && request.englishStatus() == EnglishStatus.CONFIRMED;
        redirects.prepareProductCanonical(request.slug(), chinesePublic, englishPublic);
        boolean publicBeforeAndAfter = previous.getStatus() == ProductStatus.PUBLISHED
                && chinesePublic;
        boolean englishBeforeAndAfter = publicBeforeAndAfter
                && previous.getEnglishStatus() == EnglishStatus.CONFIRMED
                && englishPublic;
        redirects.recordProductSlugChange(previous.getSlug(), request.slug(),
                publicBeforeAndAfter, englishBeforeAndAfter);
        return toDto(updated, composition.assemble(updated.getId(), request.categoryId()));
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getAdminList() {
        return mapper.selectList(new LambdaQueryWrapper<Product>()
                .isNull(Product::getDeletedAt)
                .orderByAsc(Product::getSortOrder, Product::getId))
                .stream().map(ProductService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Optional<ProductDto> getAdminById(long id) {
        Product product = mapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getId, id).isNull(Product::getDeletedAt));
        return Optional.ofNullable(product).map(value -> toDto(value,
                composition.assemble(value.getId(), value.getCategoryId())));
    }

    @Transactional(readOnly = true)
    public Optional<ProductDto> getPublicBySlug(String slug, Locale locale) {
        return Optional.ofNullable(mapper.selectOne(publicVisibilityQuery(locale)
                .eq(Product::getSlug, slug))).map(ProductService::toDto);
    }

    @Transactional(readOnly = true)
    public List<ProductDto> getPublicList(Locale locale) {
        return mapper.selectList(publicVisibilityQuery(locale)
                .orderByAsc(Product::getSortOrder, Product::getId))
                .stream().map(ProductService::toDto).toList();
    }

    @Transactional
    public void softDelete(long id) {
        if (mapper.update(null, new LambdaUpdateWrapper<Product>()
                .eq(Product::getId, id).isNull(Product::getDeletedAt)
                .setSql("deleted_at = CURRENT_TIMESTAMP")) == 0) {
            throw ProductBusinessException.notFound();
        }
    }

    @Transactional
    public ProductDto duplicate(long id) {
        Product original = requireLive(id);
        requireCategory(original.getCategoryId());
        Product copy = new Product();
        copy.setCategoryId(original.getCategoryId());
        copy.setProductCode(uniqueCode(original.getProductCode()));
        copy.setSlug(uniqueSlug(original.getSlug()));
        copy.setNameZh(original.getNameZh());
        copy.setNameEn(original.getNameEn());
        copy.setSummaryZh(original.getSummaryZh());
        copy.setSummaryEn(original.getSummaryEn());
        copy.setDescriptionZh(original.getDescriptionZh());
        copy.setDescriptionEn(original.getDescriptionEn());
        copy.setEnglishStatus(original.getEnglishStatus());
        copy.setCoverImage(original.getCoverImage());
        copy.setIsFeatured(original.getIsFeatured());
        copy.setSortOrder(original.getSortOrder());
        copy.setStatus(ProductStatus.DRAFT);
        copy.setSeoTitleZh(original.getSeoTitleZh());
        copy.setSeoTitleEn(original.getSeoTitleEn());
        copy.setSeoDescriptionZh(original.getSeoDescriptionZh());
        copy.setSeoDescriptionEn(original.getSeoDescriptionEn());
        try {
            mapper.insert(copy);
        } catch (DuplicateKeyException exception) {
            throw mapDuplicate(exception);
        }
        return toDto(copy);
    }

    private Product requireLive(long id) {
        Product product = mapper.selectOne(new LambdaQueryWrapper<Product>()
                .eq(Product::getId, id).isNull(Product::getDeletedAt));
        if (product == null) throw ProductBusinessException.notFound();
        return product;
    }

    private Product requireLiveForUpdate(long id) {
        Product product = mapper.selectLiveByIdForUpdate(id);
        if (product == null) throw ProductBusinessException.notFound();
        return product;
    }

    private void requireCategory(long categoryId) {
        try {
            categoryService.requireUsableForProduct(categoryId);
        } catch (CategoryBusinessException exception) {
            throw ProductBusinessException.invalidCategory();
        }
    }

    private static LambdaQueryWrapper<Product> publicVisibilityQuery(Locale locale) {
        Objects.requireNonNull(locale, "locale");
        LambdaQueryWrapper<Product> query = new LambdaQueryWrapper<Product>()
                .eq(Product::getStatus, ProductStatus.PUBLISHED)
                .isNull(Product::getDeletedAt);
        if ("en".equalsIgnoreCase(locale.getLanguage())) {
            query.eq(Product::getEnglishStatus, EnglishStatus.CONFIRMED);
        }
        return query;
    }

    private static ProductBusinessException mapDuplicate(DuplicateKeyException exception) {
        Throwable cause = exception;
        while (cause != null) {
            String message = cause.getMessage();
            if (message != null) {
                if (message.contains("uk_product_active_code")) {
                    return ProductBusinessException.duplicateCode();
                }
                if (message.contains("uk_product_active_slug")) {
                    return ProductBusinessException.duplicateSlug();
                }
            }
            Throwable next = cause.getCause();
            if (next == cause) {
                break;
            }
            cause = next;
        }
        return ProductBusinessException.constraintViolation();
    }

    private String uniqueCode(String base) {
        return unique(base, true);
    }

    private String uniqueSlug(String base) {
        return unique(base, false);
    }

    private String unique(String base, boolean code) {
        String suffix = "-copy";
        String candidate = trim(base, code ? 100 : 191, suffix);
        int n = 2;
        while (mapper.selectCount(new LambdaQueryWrapper<Product>()
                .eq(code ? Product::getProductCode : Product::getSlug, candidate)
                .isNull(Product::getDeletedAt)) > 0) {
            String numbered = suffix + "-" + n++;
            candidate = trim(base, code ? 100 : 191, numbered);
        }
        return candidate;
    }

    private static String trim(String base, int max, String suffix) {
        int end = Math.max(1, max - suffix.length());
        return base.substring(0, Math.min(base.length(), end)) + suffix;
    }

    private static void apply(Product product, AdminProductCreateRequest r) {
        product.setCategoryId(r.categoryId()); product.setProductCode(r.productCode()); product.setSlug(r.slug());
        product.setNameZh(r.nameZh()); product.setNameEn(r.nameEn()); product.setSummaryZh(r.summaryZh());
        product.setSummaryEn(r.summaryEn()); product.setDescriptionZh(clean(r.descriptionZh())); product.setDescriptionEn(clean(r.descriptionEn()));
        product.setEnglishStatus(r.englishStatus());
        product.setCoverImage(r.coverImage()); product.setIsFeatured(r.isFeatured()); product.setSortOrder(r.sortOrder());
        product.setStatus(r.status()); product.setSeoTitleZh(r.seoTitleZh()); product.setSeoTitleEn(r.seoTitleEn());
        product.setSeoDescriptionZh(r.seoDescriptionZh()); product.setSeoDescriptionEn(r.seoDescriptionEn());
    }

    private static ProductDto toDto(Product p) {
        return toDto(p, new ProductCompositionService.CompositionDto(List.of(), List.of()));
    }

    private static ProductDto toDto(Product p, ProductCompositionService.CompositionDto composition) {
        return new ProductDto(p.getId(), p.getCategoryId(), p.getProductCode(), p.getSlug(), p.getNameZh(), p.getNameEn(),
                p.getSummaryZh(), p.getSummaryEn(), p.getDescriptionZh(), p.getDescriptionEn(), p.getEnglishStatus(), p.getCoverImage(),
                Boolean.TRUE.equals(p.getIsFeatured()), p.getSortOrder() == null ? 0 : p.getSortOrder(), p.getStatus(),
                p.getSeoTitleZh(), p.getSeoTitleEn(), p.getSeoDescriptionZh(), p.getSeoDescriptionEn(),
                composition.attributes(), composition.variants());
    }
}
