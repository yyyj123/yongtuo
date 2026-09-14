package com.yongtuo.site.casestudy;

import static com.yongtuo.site.content.RichTextSanitizer.clean;
import com.yongtuo.site.content.PublicationPolicy;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
class CaseStudyRepository {
    private final JdbcTemplate jdbc;
    private static final DataClassRowMapper<CaseStudy> ROW = new DataClassRowMapper<>(CaseStudy.class);
    CaseStudyRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    void lockWrites() { jdbc.queryForList("SELECT id FROM product_category ORDER BY id FOR UPDATE",Long.class); }
    Optional<CaseStudy> find(long id) {
        return jdbc.query("SELECT * FROM case_study WHERE id=? AND deleted_at IS NULL",ROW,id).stream().findFirst();
    }
    Optional<CaseStudy> publicDetail(String slug, boolean en) {
        return jdbc.query("SELECT * FROM case_study WHERE slug=? AND "+PublicationPolicy.publicPredicate(en),ROW,slug).stream().findFirst();
    }
    List<CaseStudy> list(boolean pub,boolean en,int page,int size) {
        return jdbc.query("SELECT * FROM case_study WHERE "+(pub?PublicationPolicy.publicPredicate(en):"deleted_at IS NULL")
                +" ORDER BY sort_order,id DESC LIMIT ? OFFSET ?",ROW,size,(long)(page-1)*size);
    }
    long count(boolean pub,boolean en) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM case_study WHERE "+(pub?PublicationPolicy.publicPredicate(en):"deleted_at IS NULL"),Long.class);
    }
    long featuredCount(long exceptId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM case_study WHERE deleted_at IS NULL AND is_featured=1 AND id<>?",Long.class,exceptId);
    }
    boolean validReferences(List<Long> products,List<Long> categories) {
        for(long id:categories) if(jdbc.queryForList("SELECT id FROM product_category WHERE id=? AND status='ACTIVE' AND deleted_at IS NULL FOR UPDATE",Long.class,id).isEmpty()) return false;
        for(long id:products) if(jdbc.queryForList("SELECT id FROM product WHERE id=? AND deleted_at IS NULL FOR UPDATE",Long.class,id).isEmpty()) return false;
        return true;
    }
    long save(Long id,CaseStudyInput a) {
        Object[] fields={a.slug(),a.titleZh(),a.titleEn(),a.coverImage(),a.summaryZh(),a.summaryEn(),clean(a.contentZh()),clean(a.contentEn()),
                a.languageMode().name(),a.englishStatus().name(),a.status().name(),a.isFeatured(),a.sortOrder(),
                a.seoTitleZh(),a.seoTitleEn(),a.seoDescriptionZh(),a.seoDescriptionEn(),clean(a.applicationSceneZh()),
                clean(a.applicationSceneEn()),clean(a.requirementZh()),clean(a.requirementEn()),clean(a.solutionZh()),clean(a.solutionEn())};
        if(id==null) {
            jdbc.update("""
                    INSERT INTO case_study(slug,title_zh,title_en,cover_image,summary_zh,summary_en,content_zh,content_en,
                        language_mode,english_status,status,is_featured,sort_order,seo_title_zh,seo_title_en,
                        seo_description_zh,seo_description_en,application_scene_zh,application_scene_en,
                        requirement_zh,requirement_en,solution_zh,solution_en)
                    VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                    """,fields);
            id=jdbc.queryForObject("SELECT id FROM case_study WHERE slug=?",Long.class,a.slug());
        } else {
            Object[] args=java.util.Arrays.copyOf(fields,fields.length+1); args[fields.length]=id;
            jdbc.update("""
                    UPDATE case_study SET slug=?,title_zh=?,title_en=?,cover_image=?,summary_zh=?,summary_en=?,
                        content_zh=?,content_en=?,language_mode=?,english_status=?,status=?,is_featured=?,sort_order=?,
                        seo_title_zh=?,seo_title_en=?,seo_description_zh=?,seo_description_en=?,application_scene_zh=?,
                        application_scene_en=?,requirement_zh=?,requirement_en=?,solution_zh=?,solution_en=? WHERE id=?
                    """,args);
        }
        jdbc.update("UPDATE case_study SET published_at=COALESCE(published_at,CURRENT_TIMESTAMP) WHERE id=? AND status='PUBLISHED'",id);
        return id;
    }
    void relations(long id,List<Long> products,List<Long> categories,List<String> images) {
        jdbc.update("DELETE FROM case_product WHERE case_id=?",id);
        jdbc.update("DELETE FROM case_category WHERE case_id=?",id);
        jdbc.update("DELETE FROM case_image WHERE case_id=?",id);
        for(long product:products) jdbc.update("INSERT INTO case_product(case_id,product_id) VALUES (?,?)",id,product);
        for(long category:categories) jdbc.update("INSERT INTO case_category(case_id,category_id) VALUES (?,?)",id,category);
        for(int i=0;i<images.size();i++) jdbc.update("INSERT INTO case_image(case_id,sort_order,image_url) VALUES (?,?,?)",id,i,images.get(i));
    }
    CaseAdminDetail admin(CaseStudy a) {
        return new CaseAdminDetail(a,jdbc.queryForList("SELECT product_id FROM case_product WHERE case_id=? ORDER BY product_id",Long.class,a.id()),
                jdbc.queryForList("SELECT category_id FROM case_category WHERE case_id=? ORDER BY category_id",Long.class,a.id()),images(a.id()));
    }
    PublicCaseStudy publicView(CaseStudy a,boolean en) {
        var linkRow=new DataClassRowMapper<>(PublicCaseStudy.Link.class);
        var products=jdbc.query("SELECT id,slug,"+(en?"name_en":"name_zh")+" AS name FROM product WHERE id IN (SELECT product_id FROM case_product WHERE case_id=?)"
                +" AND status='PUBLISHED' AND deleted_at IS NULL"+(en?" AND english_status='CONFIRMED'":"")
                +" AND category_id IN (SELECT id FROM product_category WHERE status='ACTIVE' AND deleted_at IS NULL) ORDER BY sort_order,id",linkRow,a.id());
        var categories=jdbc.query("SELECT id,slug,"+(en?"name_en":"name_zh")+" AS name FROM product_category WHERE id IN (SELECT category_id FROM case_category WHERE case_id=?)"
                +" AND status='ACTIVE' AND deleted_at IS NULL ORDER BY sort_order,id",linkRow,a.id());
        return new PublicCaseStudy(a.id(),a.slug(),en?a.titleEn():a.titleZh(),a.coverImage(),en?a.summaryEn():a.summaryZh(),
                en?clean(a.contentEn()):clean(a.contentZh()),en?clean(a.applicationSceneEn()):clean(a.applicationSceneZh()),en?clean(a.requirementEn()):clean(a.requirementZh()),
                en?clean(a.solutionEn()):clean(a.solutionZh()),en?a.seoTitleEn():a.seoTitleZh(),en?a.seoDescriptionEn():a.seoDescriptionZh(),products,categories,images(a.id()));
    }
    List<CaseSummary> related(long productId,long categoryId,boolean en) {
        return jdbc.query("SELECT id,slug,"+(en?"title_en":"title_zh")+" AS title,cover_image,"
                +(en?"summary_en":"summary_zh")+" AS summary FROM case_study WHERE "+PublicationPolicy.publicPredicate(en)
                +" AND (id IN (SELECT case_id FROM case_product WHERE product_id=?) OR id IN (SELECT case_id FROM case_category WHERE category_id=?))"
                +" ORDER BY sort_order,id DESC",new DataClassRowMapper<>(CaseSummary.class),productId,categoryId);
    }
    private List<String> images(long id) { return jdbc.queryForList("SELECT image_url FROM case_image WHERE case_id=? ORDER BY sort_order",String.class,id); }
    void delete(long id) { jdbc.update("UPDATE case_study SET deleted_at=CURRENT_TIMESTAMP,is_featured=0 WHERE id=?",id); }
}
