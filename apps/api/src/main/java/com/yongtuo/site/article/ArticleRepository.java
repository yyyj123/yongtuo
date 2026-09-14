package com.yongtuo.site.article;

import static com.yongtuo.site.content.RichTextSanitizer.clean;
import com.yongtuo.site.content.PublicationPolicy;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class ArticleRepository {
    private final JdbcTemplate jdbc;
    private static final DataClassRowMapper<Article> ROW = new DataClassRowMapper<>(Article.class);
    ArticleRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    void lockWrites() { jdbc.queryForList("SELECT id FROM article_category ORDER BY id FOR UPDATE", Long.class); }
    Optional<Article> find(long id) {
        return jdbc.query("SELECT * FROM article WHERE id=? AND deleted_at IS NULL", ROW, id).stream().findFirst();
    }
    Optional<Article> publicDetail(String slug, boolean en) {
        return jdbc.query("SELECT * FROM article WHERE slug=? AND " + predicate(en), ROW, slug).stream().findFirst();
    }
    List<Article> list(boolean publicOnly, boolean en, int page, int size) {
        return jdbc.query("SELECT * FROM article WHERE " + (publicOnly ? predicate(en) : "deleted_at IS NULL")
                + " ORDER BY sort_order,id DESC LIMIT ? OFFSET ?", ROW, size, (long)(page-1)*size);
    }
    long count(boolean publicOnly, boolean en) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM article WHERE "
                + (publicOnly ? predicate(en) : "deleted_at IS NULL"), Long.class);
    }
    List<Article> publicCategoryList(boolean en, int page, int size, String category) {
        return jdbc.query("SELECT * FROM article WHERE " + predicate(en)
                + " AND category_id IN (SELECT id FROM article_category WHERE slug=?) ORDER BY sort_order,id DESC LIMIT ? OFFSET ?", ROW, category, size, (long)(page-1)*size);
    }
    long publicCategoryCount(boolean en, String category) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM article WHERE " + predicate(en)
                + " AND category_id IN (SELECT id FROM article_category WHERE slug=?)", Long.class, category);
    }
    boolean categoryActive(long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM article_category WHERE id=? AND status='ACTIVE'", Integer.class, id) == 1;
    }
    long featuredCount(long exceptId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM article WHERE deleted_at IS NULL AND is_featured=1 AND id<>?", Long.class, exceptId);
    }
    long save(Long id, ArticleInput a) {
        Object[] fields = {a.categoryId(), a.slug(), a.titleZh(), a.titleEn(), a.coverImage(),
                a.summaryZh(), a.summaryEn(), clean(a.contentZh()), clean(a.contentEn()), a.languageMode().name(),
                a.englishStatus().name(), a.status().name(), a.isFeatured(), a.sortOrder(), a.seoTitleZh(),
                a.seoTitleEn(), a.seoDescriptionZh(), a.seoDescriptionEn()};
        if (id == null) {
            jdbc.update("""
                    INSERT INTO article(category_id,slug,title_zh,title_en,cover_image,summary_zh,summary_en,
                        content_zh,content_en,language_mode,english_status,status,is_featured,sort_order,
                        seo_title_zh,seo_title_en,seo_description_zh,seo_description_en)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """, fields);
            id = jdbc.queryForObject("SELECT id FROM article WHERE slug=?", Long.class, a.slug());
        } else {
            Object[] args = java.util.Arrays.copyOf(fields, fields.length+1);
            args[fields.length] = id;
            jdbc.update("""
                    UPDATE article SET category_id=?,slug=?,title_zh=?,title_en=?,cover_image=?,summary_zh=?,summary_en=?,
                        content_zh=?,content_en=?,language_mode=?,english_status=?,status=?,is_featured=?,sort_order=?,
                        seo_title_zh=?,seo_title_en=?,seo_description_zh=?,seo_description_en=? WHERE id=?
                    """, args);
        }
        jdbc.update("UPDATE article SET published_at=COALESCE(published_at,CURRENT_TIMESTAMP) WHERE id=? AND status='PUBLISHED'", id);
        return id;
    }
    void delete(long id) { jdbc.update("UPDATE article SET deleted_at=CURRENT_TIMESTAMP,is_featured=0 WHERE id=?", id); }
    private static String predicate(boolean en) {
        return PublicationPolicy.publicPredicate(en)
                + " AND category_id IN (SELECT id FROM article_category WHERE status='ACTIVE')";
    }
}
