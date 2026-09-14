package com.yongtuo.site.content.home;

import static com.yongtuo.site.content.RichTextSanitizer.clean;
import com.yongtuo.site.common.BusinessException;
import com.yongtuo.site.product.EnglishStatus;
import com.yongtuo.site.site.ContactService;
import com.yongtuo.site.site.SiteConfigService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HomeService {
    private static final Set<String> SECTIONS=Set.of("HERO","BUSINESS","CNC","CAPABILITIES","ABOUT");
    private final JdbcTemplate jdbc;private final ContactService contact;private final SiteConfigService site;
    public HomeService(JdbcTemplate jdbc,ContactService contact,SiteConfigService site) { this.jdbc=jdbc;this.contact=contact;this.site=site; }
    public Map<String,Object> admin() {
        Map<String,Object> result=new LinkedHashMap<>();
        Map<String,HomeSectionInput> sections=new LinkedHashMap<>();
        for(String code:SECTIONS) sections.put(code,sectionInput(code));
        result.put("sections",sections);
        result.put("business",businessAdmin());
        for(var kind:FeaturedKind.values()) result.put(kind.name(),jdbc.queryForList("SELECT resource_id FROM "+kind.relation+" ORDER BY sort_order",Long.class));
        return result;
    }
    @Transactional(readOnly=true)
    public Map<String,Object> publicHome(boolean en) {
        Map<String,Object> result=new LinkedHashMap<>();
        result.put("hero",section("HERO",en));result.put("business",business(en));
        result.put("categories",jdbc.query("SELECT id,slug,"+(en?"name_en":"name_zh")+" AS title,cover_image FROM product_category WHERE show_on_home=1 AND status='ACTIVE' AND deleted_at IS NULL ORDER BY sort_order,id",(row,n)->new CategoryCard(row.getLong("id"),row.getString("slug"),row.getString("title"),row.getString("cover_image"))));
        result.put("featuredProducts",featured(FeaturedKind.products,en));
        result.put("cnc",section("CNC",en));result.put("capabilities",section("CAPABILITIES",en));
        result.put("featuredCases",featured(FeaturedKind.cases,en));result.put("featuredCertificates",featured(FeaturedKind.certificates,en));
        result.put("featuredArticles",featured(FeaturedKind.articles,en));result.put("about",section("ABOUT",en));
        result.put("contact",contact.publicList(en));result.put("site",site.publicConfig(en));return result;
    }
    public PublicSection section(String code,boolean en) {
        HomeSectionInput a=sectionInput(code);
        if(!a.enabled()||(en&&a.englishStatus()!=EnglishStatus.CONFIRMED)) return null;
        return new PublicSection(en?a.titleEn():a.titleZh(),en?a.subtitleEn():a.subtitleZh(),en?clean(a.contentEn()):clean(a.contentZh()),a.imageUrl());
    }
    private HomeSectionInput sectionInput(String code) {
        if(!SECTIONS.contains(code)) throw invalid("Unknown fixed section");
        return jdbc.queryForObject("SELECT * FROM home_section WHERE section_code=?",new DataClassRowMapper<>(HomeSectionInput.class),code);
    }
    @Transactional public HomeSectionInput updateSection(String code,HomeSectionInput a) {
        code=code.toUpperCase(java.util.Locale.ROOT);if(!SECTIONS.contains(code)) throw invalid("Unknown fixed section");
        if(a.englishStatus()==EnglishStatus.CONFIRMED&&(a.titleEn()==null||a.titleEn().isBlank())) throw invalid("Confirmed English title is required");
        jdbc.update("UPDATE home_section SET title_zh=?,title_en=?,subtitle_zh=?,subtitle_en=?,content_zh=?,content_en=?,image_url=?,english_status=?,enabled=? WHERE section_code=?",a.titleZh(),a.titleEn(),a.subtitleZh(),a.subtitleEn(),clean(a.contentZh()),clean(a.contentEn()),a.imageUrl(),a.englishStatus().name(),a.enabled(),code);
        return sectionInput(code);
    }
    private List<BusinessEntryInput> businessAdmin() { return jdbc.query("SELECT * FROM home_business_entry ORDER BY sort_order",new DataClassRowMapper<>(BusinessEntryInput.class)); }
    private List<PublicBusiness> business(boolean en) {
        return businessAdmin().stream().filter(a->a.enabled()&&(!en||a.englishStatus()==EnglishStatus.CONFIRMED))
                .map(a->new PublicBusiness(a.code(),en?a.titleEn():a.titleZh(),en?clean(a.contentEn()):clean(a.contentZh()),a.imageUrl(),a.linkPath())).toList();
    }
    @Transactional public List<BusinessEntryInput> updateBusiness(List<BusinessEntryInput> values) {
        if(values==null||values.size()!=3||!values.stream().map(BusinessEntryInput::code).collect(java.util.stream.Collectors.toSet()).equals(Set.of("HARDWARE","MACHINED","CNC"))) throw invalid("Exactly three fixed business entries are required");
        lock();
        for(var a:values) {
            if(a.englishStatus()==EnglishStatus.CONFIRMED&&(a.titleEn()==null||a.titleEn().isBlank())) throw invalid("Confirmed English title is required");
            jdbc.update("UPDATE home_business_entry SET title_zh=?,title_en=?,content_zh=?,content_en=?,image_url=?,link_path=?,english_status=?,enabled=? WHERE code=?",a.titleZh(),a.titleEn(),clean(a.contentZh()),clean(a.contentEn()),a.imageUrl(),a.linkPath(),a.englishStatus().name(),a.enabled(),a.code());
        }
        return businessAdmin();
    }
    @Transactional public List<Long> replaceFeatured(String kindName,List<Long> ids) {
        FeaturedKind kind;
        try { kind=FeaturedKind.valueOf(kindName); }catch(IllegalArgumentException ex) { throw invalid("Unknown featured resource"); }
        if(ids==null||ids.size()>kind.max||(kind==FeaturedKind.articles&&ids.size()!=3)
                ||ids.stream().anyMatch(id->id==null||id<=0)||ids.stream().distinct().count()!=ids.size()) throw invalid("Invalid featured count or duplicate selection");
        lock();
        switch(kind) {
            case products,cases->jdbc.queryForList("SELECT id FROM product_category ORDER BY id FOR UPDATE",Long.class);
            case articles->jdbc.queryForList("SELECT id FROM article_category ORDER BY id FOR UPDATE",Long.class);
            case certificates->jdbc.queryForList("SELECT id FROM catalog_primary_guard FOR UPDATE",Integer.class);
        }
        for(long id:ids) if(jdbc.queryForList("SELECT id FROM "+kind.table+" WHERE id=? AND status='PUBLISHED' AND deleted_at IS NULL"
                +(kind==FeaturedKind.certificates?" AND is_public=1":"")+" FOR UPDATE",Long.class,id).isEmpty()) throw invalid("Featured resource must be published and public");
        jdbc.update("DELETE FROM "+kind.relation);
        jdbc.update("UPDATE "+kind.table+" SET is_featured=0 WHERE is_featured=1");
        for(int i=0;i<ids.size();i++) {
            jdbc.update("INSERT INTO "+kind.relation+"(resource_id,sort_order) VALUES (?,?)",ids.get(i),i);
            jdbc.update("UPDATE "+kind.table+" SET is_featured=1 WHERE id=?",ids.get(i));
        }
        return List.copyOf(ids);
    }
    public List<HomeCard> featured(FeaturedKind kind,boolean en) {
        String suffix=en?"_en":"_zh";
        String title=(kind==FeaturedKind.products||kind==FeaturedKind.certificates?"name":"title")+suffix;
        String summary=(kind==FeaturedKind.certificates?"description":"summary")+suffix;
        String predicate="a.status='PUBLISHED' AND a.deleted_at IS NULL AND a.is_featured=1";
        if(en) predicate+=" AND a.english_status='CONFIRMED'";
        if(kind==FeaturedKind.articles||kind==FeaturedKind.cases) predicate+=en?" AND a.language_mode<>'ZH_ONLY'":" AND a.language_mode<>'EN_ONLY'";
        if(kind==FeaturedKind.certificates) predicate+=" AND a.is_public=1";
        if(kind==FeaturedKind.products) predicate+=" AND a.category_id IN (SELECT id FROM product_category WHERE status='ACTIVE' AND deleted_at IS NULL)";
        if(kind==FeaturedKind.articles) predicate+=" AND a.category_id IN (SELECT id FROM article_category WHERE status='ACTIVE')";
        return jdbc.query("SELECT a.id,"+(kind==FeaturedKind.certificates?"NULL":"a.slug")+" AS slug,a."+title+" AS title,a.cover_image,a."+summary+" AS summary,"
                +(kind==FeaturedKind.certificates?"CASE WHEN a.allow_download=1 THEN a.file_url ELSE NULL END":"NULL")+" AS download_url FROM "+kind.table+" a LEFT JOIN "+kind.relation+" f ON f.resource_id=a.id WHERE "+predicate
                +" ORDER BY COALESCE(f.sort_order,a.sort_order),a.id LIMIT ?",new DataClassRowMapper<>(HomeCard.class),kind.max);
    }
    private void lock() { jdbc.queryForList("SELECT id FROM home_section ORDER BY id FOR UPDATE",Long.class); }
    private static BusinessException invalid(String message) { return new BusinessException(34003,message,HttpStatus.BAD_REQUEST); }
    public record PublicSection(String title,String subtitle,String content,String imageUrl) { }
    public record PublicBusiness(String code,String title,String content,String imageUrl,String linkPath) { }
    public record HomeCard(long id,String slug,String title,String coverImage,String summary,String downloadUrl) { }
    public record CategoryCard(long id,String slug,String title,String coverImage) { }
}
