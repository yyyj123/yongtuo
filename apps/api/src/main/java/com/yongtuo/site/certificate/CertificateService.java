package com.yongtuo.site.certificate;
import static com.yongtuo.site.content.RichTextSanitizer.clean;
import com.yongtuo.site.common.BusinessException;
import com.yongtuo.site.product.EnglishStatus;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class CertificateService {
    private final JdbcTemplate jdbc;
    private static final DataClassRowMapper<Certificate> ROW=new DataClassRowMapper<>(Certificate.class);
    public CertificateService(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public List<Certificate> list() { return jdbc.query("SELECT * FROM certificate WHERE deleted_at IS NULL ORDER BY sort_order,id",ROW); }
    public Certificate get(long id) { return jdbc.query("SELECT * FROM certificate WHERE id=? AND deleted_at IS NULL",ROW,id).stream().findFirst().orElseThrow(()->new BusinessException(32001,"Certificate not found",HttpStatus.NOT_FOUND)); }
    public List<PublicCertificate> publicList(boolean en) {
        return jdbc.query("SELECT * FROM certificate WHERE deleted_at IS NULL AND status='PUBLISHED' AND is_public=1"
                +(en?" AND english_status='CONFIRMED'":"")+" ORDER BY sort_order,id",ROW).stream()
                .map(a->new PublicCertificate(a.id(),a.type(),en?a.nameEn():a.nameZh(),en?clean(a.descriptionEn()):clean(a.descriptionZh()),
                        a.coverImage(),a.allowDownload()?a.fileUrl():null,a.allowDownload())).toList();
    }
    @Transactional public Certificate save(Long id,CertificateInput a) {
        jdbc.queryForList("SELECT id FROM catalog_primary_guard FOR UPDATE",Integer.class);
        if(id!=null) get(id);
        if(a.englishStatus()==EnglishStatus.CONFIRMED&&(a.nameEn()==null||a.nameEn().isBlank())) throw invalid("Confirmed English name is required");
        if(a.allowDownload()&&(a.fileUrl()==null||!a.fileUrl().matches("https?://[^\\s]+"))) throw invalid("Download file URL is required");
        if(a.isFeatured()&&jdbc.queryForObject("SELECT COUNT(*) FROM certificate WHERE is_featured=1 AND deleted_at IS NULL AND id<>?",Long.class,id==null?-1:id)>=4) throw invalid("At most four featured certificates");
        Object[] fields={a.type(),a.nameZh(),a.nameEn(),clean(a.descriptionZh()),clean(a.descriptionEn()),a.englishStatus().name(),a.coverImage(),a.mediaId(),a.fileUrl(),a.isPublic(),a.allowDownload(),a.isFeatured(),a.sortOrder(),a.status().name()};
        try {
            if(id==null) {
                var key=new org.springframework.jdbc.support.GeneratedKeyHolder();
                jdbc.update(connection->{var statement=connection.prepareStatement("INSERT INTO certificate(type,name_zh,name_en,description_zh,description_en,english_status,cover_image,media_id,file_url,is_public,allow_download,is_featured,sort_order,status) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",java.sql.Statement.RETURN_GENERATED_KEYS);
                    for(int i=0;i<fields.length;i++) statement.setObject(i+1,fields[i]);return statement;},key);
                id=key.getKey().longValue();
            }else {
                Object[] args=java.util.Arrays.copyOf(fields,fields.length+1);args[fields.length]=id;
                jdbc.update("UPDATE certificate SET type=?,name_zh=?,name_en=?,description_zh=?,description_en=?,english_status=?,cover_image=?,media_id=?,file_url=?,is_public=?,allow_download=?,is_featured=?,sort_order=?,status=? WHERE id=?",args);
            }
            return get(id);
        }catch(DataIntegrityViolationException ex) { throw invalid("Invalid certificate reference or data"); }
    }
    @Transactional public void delete(long id) { get(id);jdbc.update("UPDATE certificate SET deleted_at=CURRENT_TIMESTAMP,is_featured=0 WHERE id=?",id); }
    private static BusinessException invalid(String message) { return new BusinessException(32002,message,HttpStatus.BAD_REQUEST); }
}
