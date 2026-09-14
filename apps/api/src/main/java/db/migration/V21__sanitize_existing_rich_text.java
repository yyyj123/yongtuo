package db.migration;

import com.yongtuo.site.content.RichTextSanitizer;
import java.util.List;
import java.util.Objects;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** One-time cleanup of managed HTML written before the sanitization boundary existed. */
public class V21__sanitize_existing_rich_text extends BaseJavaMigration {
    @Override public Integer getChecksum() { return 20260907; }
    @Override public void migrate(Context context) throws Exception {
        var targets=List.of(
                new Target("product","id",List.of("description_zh","description_en"),"1=1"),
                new Target("product_category","id",List.of("description_zh","description_en"),"1=1"),
                new Target("article","id",List.of("content_zh","content_en"),"1=1"),
                new Target("case_study","id",List.of("content_zh","content_en","application_scene_zh","application_scene_en","requirement_zh","requirement_en","solution_zh","solution_en"),"1=1"),
                new Target("certificate","id",List.of("description_zh","description_en"),"1=1"),
                new Target("home_section","id",List.of("content_zh","content_en"),"1=1"),
                new Target("home_business_entry","code",List.of("content_zh","content_en"),"1=1"),
                new Target("site_config","id",List.of("value_zh","value_en"),"value_type='HTML'"));
        // Preflight size/parse validation before writing any row.
        for(var target:targets) clean(context,target,false);
        for(var target:targets) clean(context,target,true);
    }
    private static void clean(Context context,Target target,boolean write) throws Exception {
        String columns=String.join(",",target.columns());
        try(var select=context.getConnection().prepareStatement("SELECT "+target.key()+","+columns+" FROM "+target.table()+" WHERE "+target.condition());
            var rows=select.executeQuery()) {
            while(rows.next()) {
                for(String column:target.columns()) {
                    String before=rows.getString(column),after=RichTextSanitizer.clean(before);
                    if(write&&!Objects.equals(before,after)) try(var update=context.getConnection().prepareStatement("UPDATE "+target.table()+" SET "+column+"=? WHERE "+target.key()+"=?")) {
                        update.setString(1,after);update.setObject(2,rows.getObject(target.key()));update.executeUpdate();
                    }
                }
            }
        }
    }
    private record Target(String table,String key,List<String> columns,String condition) { }
}
