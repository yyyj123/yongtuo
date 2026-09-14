package com.yongtuo.site.catalog;
import com.yongtuo.site.common.BusinessException;
import com.yongtuo.site.content.LanguageMode;
import com.yongtuo.site.content.PublicationPolicy;
import com.yongtuo.site.product.EnglishStatus;
import com.yongtuo.site.product.ProductStatus;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class CatalogService {
    private final JdbcTemplate jdbc;
    private static final DataClassRowMapper<Catalog> ROW=new DataClassRowMapper<>(Catalog.class);
    public CatalogService(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public List<Catalog> list() { return jdbc.query("SELECT * FROM catalog WHERE deleted_at IS NULL ORDER BY id DESC",ROW); }
    public Catalog get(long id) { return jdbc.query("SELECT * FROM catalog WHERE id=? AND deleted_at IS NULL",ROW,id).stream().findFirst().orElseThrow(()->new BusinessException(33001,"Catalog not found",HttpStatus.NOT_FOUND)); }
    public List<PublicCatalog> publicList(boolean en) { return jdbc.query("SELECT * FROM catalog WHERE "+PublicationPolicy.publicPredicate(en)+" ORDER BY is_primary DESC,id DESC",ROW).stream().map(a->new PublicCatalog(a.id(),en?a.titleEn():a.titleZh(),a.version(),a.coverImage(),a.fileUrl(),a.isPrimary())).toList(); }
    @Transactional public Catalog save(Long id,CatalogInput a) {
        lock(); Catalog old=id==null?null:get(id);
        if(a.englishStatus()==EnglishStatus.CONFIRMED&&blank(a.titleEn())) throw invalid("Confirmed English title is required");
        if(a.status()==ProductStatus.PUBLISHED&&((a.languageMode()!=LanguageMode.EN_ONLY&&blank(a.titleZh()))
                ||(a.languageMode()==LanguageMode.EN_ONLY&&a.englishStatus()!=EnglishStatus.CONFIRMED))) throw invalid("Published catalog requires complete confirmed content");
        Object[] fields={a.titleZh(),a.titleEn(),a.languageMode().name(),a.englishStatus().name(),a.version(),a.coverImage(),a.mediaId(),a.fileUrl(),a.status().name()};
        try {
            if(id==null) {
                var key=new org.springframework.jdbc.support.GeneratedKeyHolder();
                jdbc.update(connection->{var statement=connection.prepareStatement("INSERT INTO catalog(title_zh,title_en,language_mode,english_status,version,cover_image,media_id,file_url,status) VALUES (?,?,?,?,?,?,?,?,?)",java.sql.Statement.RETURN_GENERATED_KEYS);
                    for(int i=0;i<fields.length;i++) statement.setObject(i+1,fields[i]);return statement;},key);
                id=key.getKey().longValue();
            }else {
                jdbc.update("UPDATE catalog SET is_primary=0 WHERE id=?",id);
                Object[] args=java.util.Arrays.copyOf(fields,fields.length+1);args[fields.length]=id;
                jdbc.update("UPDATE catalog SET title_zh=?,title_en=?,language_mode=?,english_status=?,version=?,cover_image=?,media_id=?,file_url=?,status=? WHERE id=?",args);
            }
            jdbc.update("UPDATE catalog SET published_at=COALESCE(published_at,CURRENT_TIMESTAMP) WHERE id=? AND status='PUBLISHED'",id);
            if(old!=null&&old.isPrimary()&&a.status()==ProductStatus.PUBLISHED) selectPrimary(id);
            return get(id);
        }catch(DataIntegrityViolationException exception) { throw invalid("Invalid catalog reference or data"); }
    }
    @Transactional public Catalog primary(long id) { lock();selectPrimary(id);return get(id); }
    private void selectPrimary(long id) {
        Catalog a=get(id);
        if(a.status()!=ProductStatus.PUBLISHED) throw invalid("Primary catalog must be published");
        // Bilingual primary occupies both language slots; other modes occupy just their own slot.
        String overlap=switch(a.languageMode()) { case BILINGUAL->"1=1";case ZH_ONLY->"language_mode IN ('ZH_ONLY','BILINGUAL')";case EN_ONLY->"language_mode IN ('EN_ONLY','BILINGUAL')"; };
        jdbc.update("UPDATE catalog SET is_primary=0 WHERE is_primary=1 AND ("+overlap+")");
        jdbc.update("UPDATE catalog SET is_primary=1 WHERE id=?",id);
    }
    @Transactional public void delete(long id) { lock();get(id);jdbc.update("UPDATE catalog SET deleted_at=CURRENT_TIMESTAMP,is_primary=0 WHERE id=?",id); }
    private void lock() { jdbc.queryForList("SELECT id FROM catalog_primary_guard FOR UPDATE",Integer.class); }
    private static boolean blank(String s) { return s==null||s.isBlank(); }
    private static BusinessException invalid(String message) { return new BusinessException(33002,message,HttpStatus.BAD_REQUEST); }
}
