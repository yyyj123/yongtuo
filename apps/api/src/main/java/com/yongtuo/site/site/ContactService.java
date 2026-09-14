package com.yongtuo.site.site;
import com.yongtuo.site.common.BusinessException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class ContactService {
    private final JdbcTemplate jdbc;
    public ContactService(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public List<ContactInput> adminList() { return jdbc.query("SELECT * FROM contact_info ORDER BY sort_order_zh,id",new DataClassRowMapper<>(ContactInput.class)); }
    public List<PublicContact> publicList(boolean en) {
        return jdbc.query("SELECT * FROM contact_info WHERE enabled=1 ORDER BY "+(en?"sort_order_en":"sort_order_zh")+",id",new DataClassRowMapper<>(ContactInput.class))
                .stream().filter(a->!en||a.type()!=ContactInput.Type.ADDRESS||a.valueEn()!=null&&!a.valueEn().isBlank())
                .map(a->new PublicContact(a.type(),en?a.labelEn():a.labelZh(),en&&a.valueEn()!=null&&!a.valueEn().isBlank()?a.valueEn():a.value(),a.linkUrl())).toList();
    }
    @Transactional public List<ContactInput> replace(List<ContactInput> input) {
        if(input==null||input.size()>50) throw invalid();
        for(var a:input) {
            if(a.type()==ContactInput.Type.EMAIL&&!a.value().matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw invalid();
            if(a.linkUrl()!=null&&!a.linkUrl().isBlank()&&!a.linkUrl().matches("(?:https?://|mailto:|tel:)[^\\s]+")) throw invalid();
        }
        jdbc.queryForList("SELECT id FROM home_section ORDER BY id FOR UPDATE",Long.class);
        jdbc.update("DELETE FROM contact_info");
        for(var a:input) jdbc.update("INSERT INTO contact_info(type,label_zh,label_en,value,value_en,link_url,sort_order_zh,sort_order_en,enabled) VALUES (?,?,?,?,?,?,?,?,?)",a.type().name(),a.labelZh(),a.labelEn(),a.value(),a.valueEn(),a.linkUrl(),a.sortOrderZh(),a.sortOrderEn(),a.enabled());
        return adminList();
    }
    private static BusinessException invalid() { return new BusinessException(34001,"Invalid contact configuration",HttpStatus.BAD_REQUEST); }
    public record PublicContact(ContactInput.Type type,String label,String value,String linkUrl) { }
}
