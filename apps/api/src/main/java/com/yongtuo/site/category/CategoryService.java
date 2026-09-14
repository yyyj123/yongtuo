package com.yongtuo.site.category;

import static com.yongtuo.site.content.RichTextSanitizer.clean;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.yongtuo.site.site.UrlRedirectService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {
    private static final Comparator<ProductCategory> ORDER = Comparator
            .comparing(ProductCategory::getSortOrder)
            .thenComparing(ProductCategory::getId);

    private final CategoryMapper mapper;
    private final JdbcTemplate jdbc;
    private final UrlRedirectService redirects;

    public CategoryService(CategoryMapper mapper, JdbcTemplate jdbc, UrlRedirectService redirects) {
        this.mapper = mapper;
        this.jdbc = jdbc;
        this.redirects = redirects;
    }

    @Transactional(readOnly = true)
    public List<PublicCategoryDto> getPublicTree(Locale locale) {
        List<ProductCategory> categories = mapper.selectList(new LambdaQueryWrapper<ProductCategory>()
                .eq(ProductCategory::getStatus, CategoryStatus.ACTIVE)
                .isNull(ProductCategory::getDeletedAt)
                .orderByAsc(ProductCategory::getSortOrder, ProductCategory::getId));
        return buildTree(categories, category -> publicDto(category, locale, List.of()));
    }

    @Transactional(readOnly = true)
    public PublicCategoryDto getPublicBySlug(String slug, Locale locale) {
        ProductCategory category = mapper.selectOne(new LambdaQueryWrapper<ProductCategory>()
                .eq(ProductCategory::getSlug, slug)
                .eq(ProductCategory::getStatus, CategoryStatus.ACTIVE)
                .isNull(ProductCategory::getDeletedAt));
        if (category == null) throw CategoryBusinessException.notFound();
        return find(getPublicTree(locale), category.getId());
    }

    @Transactional(readOnly = true)
    public List<AdminCategoryDto> getAdminTree() {
        List<ProductCategory> categories = allActiveRecords();
        return buildTree(categories, category -> adminDto(category, List.of()));
    }

    @Transactional
    public AdminCategoryDto create(AdminCategoryWriteRequest request) {
        List<ProductCategory> lockedHierarchy = lockHierarchy();
        validateParent(request.parentId(), null, lockedHierarchy);
        ProductCategory category = new ProductCategory();
        apply(category, request);
        try {
            mapper.insert(category);
        } catch (DuplicateKeyException exception) {
            throw CategoryBusinessException.duplicateSlug();
        }
        redirects.prepareCategoryCanonical(request.slug(), request.status() == CategoryStatus.ACTIVE);
        return adminDto(category, List.of());
    }

    @Transactional
    public AdminCategoryDto update(long id, AdminCategoryWriteRequest request) {
        List<ProductCategory> lockedHierarchy = lockHierarchy();
        ProductCategory category = requireFromHierarchy(id, lockedHierarchy);
        validateParent(request.parentId(), id, lockedHierarchy);
        ensureNoCycle(id, request.parentId(), parentMap(lockedHierarchy));
        String previousSlug = category.getSlug();
        CategoryStatus previousStatus = category.getStatus();
        apply(category, request);
        try {
            mapper.update(null, new LambdaUpdateWrapper<ProductCategory>()
                    .eq(ProductCategory::getId, id)
                    .isNull(ProductCategory::getDeletedAt)
                    .set(ProductCategory::getParentId, request.parentId())
                    .set(ProductCategory::getNameZh, request.nameZh())
                    .set(ProductCategory::getNameEn, request.nameEn())
                    .set(ProductCategory::getSlug, request.slug())
                    .set(ProductCategory::getCoverImage, request.coverImage())
                    .set(ProductCategory::getDescriptionZh, clean(request.descriptionZh()))
                    .set(ProductCategory::getDescriptionEn, clean(request.descriptionEn()))
                    .set(ProductCategory::getCategoryMode, request.categoryMode())
                    .set(ProductCategory::getSortOrder, request.sortOrder())
                    .set(ProductCategory::getStatus, request.status())
                    .set(ProductCategory::getShowOnHome, request.showOnHome())
                    .set(ProductCategory::getSeoTitleZh, request.seoTitleZh())
                    .set(ProductCategory::getSeoTitleEn, request.seoTitleEn())
                    .set(ProductCategory::getSeoDescriptionZh, request.seoDescriptionZh())
                    .set(ProductCategory::getSeoDescriptionEn, request.seoDescriptionEn()));
        } catch (DuplicateKeyException exception) {
            throw CategoryBusinessException.duplicateSlug();
        }
        boolean publicCategory = request.status() == CategoryStatus.ACTIVE;
        redirects.prepareCategoryCanonical(request.slug(), publicCategory);
        redirects.recordCategorySlugChange(previousSlug, request.slug(),
                previousStatus == CategoryStatus.ACTIVE && publicCategory);
        return adminDto(category, List.of());
    }

    @Transactional
    public void sort(List<CategorySortItem> items) {
        if (items == null || items.isEmpty()) return;
        List<ProductCategory> categories = lockHierarchy();
        Map<Long, Long> parents = parentMap(categories);
        Set<Long> knownIds = parents.keySet();
        Set<Long> itemIds = new HashSet<>();
        for (CategorySortItem item : items) {
            if (item == null || item.id() == null || item.sortOrder() == null || item.sortOrder() < 0
                    || !knownIds.contains(item.id())) {
                throw CategoryBusinessException.notFound();
            }
            if (!itemIds.add(item.id())) throw CategoryBusinessException.invalidParent();
            if (item.parentId() != null && !knownIds.contains(item.parentId())) {
                throw CategoryBusinessException.invalidParent();
            }
            parents.put(item.id(), item.parentId());
        }
        for (Long id : knownIds) ensureNoCycle(id, parents.get(id), parents);
        for (CategorySortItem item : items) {
            mapper.update(null, new LambdaUpdateWrapper<ProductCategory>()
                    .eq(ProductCategory::getId, item.id())
                    .isNull(ProductCategory::getDeletedAt)
                    .set(ProductCategory::getParentId, item.parentId())
                    .set(ProductCategory::getSortOrder, item.sortOrder()));
        }
    }

    @Transactional
    public void softDelete(long id) {
        List<ProductCategory> lockedHierarchy = lockHierarchy();
        requireFromHierarchy(id, lockedHierarchy);
        boolean hasChildren = lockedHierarchy.stream()
                .anyMatch(category -> Objects.equals(category.getParentId(), id));
        if (hasChildren) throw CategoryBusinessException.notEmpty();
        if (jdbc.queryForObject("SELECT COUNT(*) FROM category_attribute WHERE category_id = ?", Integer.class, id) > 0) {
            throw CategoryBusinessException.notEmpty();
        }
        if (jdbc.queryForObject("SELECT COUNT(*) FROM product WHERE category_id = ?", Integer.class, id) > 0) {
            throw CategoryBusinessException.notEmpty();
        }
        mapper.update(null, new LambdaUpdateWrapper<ProductCategory>()
                .eq(ProductCategory::getId, id)
                .isNull(ProductCategory::getDeletedAt)
                .set(ProductCategory::getDeletedAt, LocalDateTime.now()));
    }

    /** Locks the live category so product creation and category deletion share one ordering. */
    @Transactional
    public void requireUsableForProduct(long id) {
        if (mapper.selectLiveByIdForUpdate(id) == null) {
            throw CategoryBusinessException.invalidParent();
        }
    }

    /** Locks one category before an attribute binding transaction acquires attribute locks. */
    @Transactional
    public void lockActiveForAttributeBinding(long id) {
        List<Long> rows = jdbc.query("""
                SELECT id FROM product_category
                WHERE id = ? AND status = 'ACTIVE' AND deleted_at IS NULL
                FOR UPDATE
                """, (result, rowNum) -> result.getLong(1), id);
        if (rows.isEmpty()) throw CategoryBusinessException.invalidParent();
    }

    private static void validateParent(Long parentId, Long categoryId,
                                       List<ProductCategory> lockedHierarchy) {
        if (parentId == null) return;
        if (Objects.equals(parentId, categoryId)) throw CategoryBusinessException.cycle();
        if (lockedHierarchy.stream().noneMatch(category -> Objects.equals(category.getId(), parentId))) {
            throw CategoryBusinessException.invalidParent();
        }
    }

    private static ProductCategory requireFromHierarchy(
            long id, List<ProductCategory> lockedHierarchy) {
        return lockedHierarchy.stream()
                .filter(category -> Objects.equals(category.getId(), id))
                .findFirst()
                .orElseThrow(CategoryBusinessException::notFound);
    }

    private List<ProductCategory> allActiveRecords() {
        return mapper.selectList(new LambdaQueryWrapper<ProductCategory>()
                .isNull(ProductCategory::getDeletedAt)
                .orderByAsc(ProductCategory::getSortOrder, ProductCategory::getId));
    }

    private List<ProductCategory> lockHierarchy() {
        return mapper.selectLiveHierarchyForUpdate();
    }

    private static Map<Long, Long> parentMap(List<ProductCategory> categories) {
        Map<Long, Long> parents = new HashMap<>();
        for (ProductCategory category : categories) parents.put(category.getId(), category.getParentId());
        return parents;
    }

    private static void ensureNoCycle(long id, Long proposedParent, Map<Long, Long> parents) {
        Set<Long> visited = new HashSet<>();
        visited.add(id);
        Long cursor = proposedParent;
        while (cursor != null) {
            if (!visited.add(cursor)) throw CategoryBusinessException.cycle();
            cursor = parents.get(cursor);
        }
    }

    private static void apply(ProductCategory category, AdminCategoryWriteRequest request) {
        category.setParentId(request.parentId());
        category.setNameZh(request.nameZh());
        category.setNameEn(request.nameEn());
        category.setSlug(request.slug());
        category.setCoverImage(request.coverImage());
        category.setDescriptionZh(clean(request.descriptionZh()));
        category.setDescriptionEn(clean(request.descriptionEn()));
        category.setCategoryMode(request.categoryMode());
        category.setSortOrder(request.sortOrder());
        category.setStatus(request.status());
        category.setShowOnHome(request.showOnHome());
        category.setSeoTitleZh(request.seoTitleZh());
        category.setSeoTitleEn(request.seoTitleEn());
        category.setSeoDescriptionZh(request.seoDescriptionZh());
        category.setSeoDescriptionEn(request.seoDescriptionEn());
    }

    private static PublicCategoryDto publicDto(ProductCategory category, Locale locale,
                                                List<PublicCategoryDto> children) {
        boolean english = locale != null && Locale.ENGLISH.getLanguage().equals(locale.getLanguage());
        return new PublicCategoryDto(category.getId(), category.getSlug(),
                english ? category.getNameEn() : category.getNameZh(), category.getCoverImage(),
                english ? category.getDescriptionEn() : category.getDescriptionZh(),
                category.getCategoryMode(), Boolean.TRUE.equals(category.getShowOnHome()),
                english ? category.getSeoTitleEn() : category.getSeoTitleZh(),
                english ? category.getSeoDescriptionEn() : category.getSeoDescriptionZh(), children);
    }

    private static AdminCategoryDto adminDto(ProductCategory category, List<AdminCategoryDto> children) {
        return new AdminCategoryDto(category.getId(), category.getParentId(), category.getNameZh(),
                category.getNameEn(), category.getSlug(), category.getCoverImage(),
                category.getDescriptionZh(), category.getDescriptionEn(), category.getCategoryMode(),
                category.getSortOrder(), category.getStatus(), Boolean.TRUE.equals(category.getShowOnHome()),
                category.getSeoTitleZh(), category.getSeoTitleEn(), category.getSeoDescriptionZh(),
                category.getSeoDescriptionEn(), children);
    }

    private static <T> List<T> buildTree(List<ProductCategory> categories,
                                          Function<ProductCategory, T> flatMapper) {
        Map<Long, List<ProductCategory>> children = new HashMap<>();
        for (ProductCategory category : categories) {
            children.computeIfAbsent(category.getParentId(), ignored -> new ArrayList<>()).add(category);
        }
        children.values().forEach(list -> list.sort(ORDER));
        return buildChildren(null, children, flatMapper);
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> buildChildren(Long parentId, Map<Long, List<ProductCategory>> grouped,
                                              Function<ProductCategory, T> flatMapper) {
        List<T> result = new ArrayList<>();
        for (ProductCategory category : grouped.getOrDefault(parentId, List.of())) {
            List<T> nested = buildChildren(category.getId(), grouped, flatMapper);
            T flat = flatMapper.apply(category);
            if (flat instanceof PublicCategoryDto dto) {
                flat = (T) new PublicCategoryDto(dto.id(), dto.slug(), dto.name(), dto.coverImage(),
                        dto.description(), dto.mode(), dto.showOnHome(), dto.seoTitle(), dto.seoDescription(),
                        (List<PublicCategoryDto>) nested);
            } else if (flat instanceof AdminCategoryDto dto) {
                flat = (T) new AdminCategoryDto(dto.id(), dto.parentId(), dto.nameZh(), dto.nameEn(),
                        dto.slug(), dto.coverImage(), dto.descriptionZh(), dto.descriptionEn(), dto.categoryMode(),
                        dto.sortOrder(), dto.status(), dto.showOnHome(), dto.seoTitleZh(), dto.seoTitleEn(),
                        dto.seoDescriptionZh(), dto.seoDescriptionEn(), (List<AdminCategoryDto>) nested);
            }
            result.add(flat);
        }
        return List.copyOf(result);
    }

    private static PublicCategoryDto find(List<PublicCategoryDto> categories, long id) {
        for (PublicCategoryDto category : categories) {
            if (category.id() == id) return category;
            try {
                return find(category.children(), id);
            } catch (CategoryBusinessException ignored) {
                // Continue with the next branch.
            }
        }
        throw CategoryBusinessException.notFound();
    }
}
