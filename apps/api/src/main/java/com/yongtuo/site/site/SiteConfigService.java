package com.yongtuo.site.site;
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
public class SiteConfigService {
    public static final Set<String> KEYS=Set.of("brand","company_name","company_legal_name","founded_year","company_profile","default_seo_title","default_seo_description","icp_number","copyright","logo","favicon","cnc_intro","capabilities_intro","privacy_policy");
    private final JdbcTemplate jdbc;
    public SiteConfigService(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public Map<String,ConfigValueInput> admin() {
        Map<String,ConfigValueInput> result=new LinkedHashMap<>();
        jdbc.query("SELECT * FROM site_config ORDER BY config_key",(org.springframework.jdbc.core.RowCallbackHandler)row->result.put(row.getString("config_key"),new ConfigValueInput(row.getString("value_zh"),row.getString("value_en"),EnglishStatus.valueOf(row.getString("english_status")))));
        return result;
    }
    public Map<String,String> publicConfig(boolean en) {
        Map<String,String> result=new LinkedHashMap<>();
        admin().forEach((key,value)->{String text=en?value.valueEn():value.valueZh();if(text!=null&&(!en||value.englishStatus()==EnglishStatus.CONFIRMED)) result.put(key,text);});
        return result;
    }
    @Transactional public Map<String,ConfigValueInput> update(Map<String,ConfigValueInput> values) {
        if(values==null||!KEYS.containsAll(values.keySet())||values.values().stream().anyMatch(java.util.Objects::isNull)) throw invalid();
        for(var entry:values.entrySet()) {
            String key=entry.getKey();var a=entry.getValue();
            String type=Set.of("company_profile","privacy_policy","cnc_intro","capabilities_intro").contains(key)?"HTML":key.equals("founded_year")?"NUMBER":Set.of("logo","favicon").contains(key)?"URL":"TEXT";
            if(a.englishStatus()==EnglishStatus.CONFIRMED&&(a.valueEn()==null||a.valueEn().isBlank())) throw invalid();
            if(type.equals("NUMBER")&&a.valueZh()!=null&&!a.valueZh().matches("[0-9]{4}")) throw invalid();
            if(type.equals("URL")) for(String url:new String[]{a.valueZh(),a.valueEn()}) if(url!=null&&!url.isBlank()&&!url.matches("https?://[^\\s]+")) throw invalid();
            jdbc.update("INSERT INTO site_config(config_key,value_zh,value_en,value_type,english_status) VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE value_zh=VALUES(value_zh),value_en=VALUES(value_en),value_type=VALUES(value_type),english_status=VALUES(english_status)",key,type.equals("HTML")?com.yongtuo.site.content.RichTextSanitizer.clean(a.valueZh()):a.valueZh(),type.equals("HTML")?com.yongtuo.site.content.RichTextSanitizer.clean(a.valueEn()):a.valueEn(),type,a.englishStatus().name());
        }
        return admin();
    }
    private static BusinessException invalid() { return new BusinessException(34002,"Invalid site configuration",HttpStatus.BAD_REQUEST); }
}
