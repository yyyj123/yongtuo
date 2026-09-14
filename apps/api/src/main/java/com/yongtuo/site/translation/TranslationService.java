package com.yongtuo.site.translation;
import com.yongtuo.site.common.BusinessException;
import com.yongtuo.site.product.EnglishStatus;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class TranslationService {
    private final JdbcTemplate jdbc;private final TranslationProvider provider;
    public TranslationService(JdbcTemplate jdbc,TranslationProvider provider) { this.jdbc=jdbc;this.provider=provider; }
    @Transactional public Result draft(DraftRequest request) {
        var resource=request.resourceType();
        if(request.fields().stream().distinct().count()!=request.fields().size()||!resource.fields.containsAll(request.fields())) throw invalid();
        Map<String,Object> current=lock(resource,request.resourceId());
        if("CONFIRMED".equals(current.get("english_status"))&&!request.replaceConfirmed()) throw conflict("Confirmed English requires explicit replacement");
        // Numeric/site identity configuration is not AI-authored content.
        if(resource==TranslationResource.SITE_CONFIG&&!"HTML".equals(current.get("value_type"))) throw invalid();
        Map<String,String> source=new LinkedHashMap<>();
        for(String field:request.fields()) {
            Object value=current.get(field+"_zh");
            if(value==null||value.toString().isBlank()) throw invalid();
            source.put(field,value.toString());
        }
        TranslationDraft generated=provider.translate(new TranslationRequest(resource.name(),request.resourceId(),source));
        if(generated==null||!generated.fields().keySet().equals(source.keySet())) throw invalidProvider();
        Map<String,String> safeFields=new LinkedHashMap<>();
        for(var field:generated.fields().entrySet()) {
            String value=field.getValue();
            if(resource==TranslationResource.SITE_CONFIG||Set.of("description","content","application_scene","requirement","solution").contains(field.getKey()))
                value=com.yongtuo.site.content.RichTextSanitizer.clean(value);
            if(value==null||value.isBlank()||value.length()>resource.maxLength(field.getKey())) throw invalidProvider();
            safeFields.put(field.getKey(),value);
            jdbc.update("UPDATE "+resource.table+" SET "+field.getKey()+"_en=? WHERE id=?",value,request.resourceId());
        }
        jdbc.update("UPDATE "+resource.table+" SET english_status='AI_DRAFT' WHERE id=?",request.resourceId());
        return new Result(resource,request.resourceId(),safeFields,EnglishStatus.AI_DRAFT);
    }
    @Transactional public Result confirm(ConfirmTranslationRequest request) {
        var resource=request.resourceType();
        if(!resource.fields.containsAll(request.expectedFields().keySet())) throw invalid();
        Map<String,Object> current=lock(resource,request.resourceId());
        if(!"AI_DRAFT".equals(current.get("english_status"))) throw conflict("Resource is not an English draft");
        Map<String,String> actual=new LinkedHashMap<>();
        for(String field:resource.fields) {
            Object value=current.get(field+"_en");
            if(value!=null&&!value.toString().isBlank()) actual.put(field,value.toString());
        }
        if(!actual.equals(request.expectedFields())) throw conflict("English draft changed; review all current fields");
        if(!actual.keySet().containsAll(resource.required)) throw invalid();
        jdbc.update("UPDATE "+resource.table+" SET english_status='CONFIRMED' WHERE id=?",request.resourceId());
        return new Result(resource,request.resourceId(),actual,EnglishStatus.CONFIRMED);
    }
    private Map<String,Object> lock(TranslationResource resource,long id) {
        var rows=jdbc.queryForList("SELECT * FROM "+resource.table+" WHERE id=?"+(resource.softDeleted?" AND deleted_at IS NULL":"")+" FOR UPDATE",id);
        if(rows.isEmpty()) throw new BusinessException(36001,"Translation resource not found",HttpStatus.NOT_FOUND);
        return rows.getFirst();
    }
    private static BusinessException invalid() { return new BusinessException(36003,"Invalid translation fields or source",HttpStatus.BAD_REQUEST); }
    private static BusinessException conflict(String text) { return new BusinessException(36002,text,HttpStatus.CONFLICT); }
    private static BusinessException invalidProvider() { return new BusinessException(36004,"Invalid translation provider result",HttpStatus.BAD_GATEWAY); }
    public record Result(TranslationResource resourceType,long resourceId,Map<String,String> fields,EnglishStatus englishStatus) { }
}
