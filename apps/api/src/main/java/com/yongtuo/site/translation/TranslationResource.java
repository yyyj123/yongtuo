package com.yongtuo.site.translation;
import java.util.Set;
public enum TranslationResource {
    PRODUCT("product",true,Set.of("name","summary","description"),Set.of("name")),
    ARTICLE("article",true,Set.of("title","summary","content"),Set.of("title","content")),
    CASE("case_study",true,Set.of("title","summary","content","application_scene","requirement","solution"),Set.of("title","content")),
    CERTIFICATE("certificate",true,Set.of("name","description"),Set.of("name")),
    CATALOG("catalog",true,Set.of("title"),Set.of("title")),
    HOME_SECTION("home_section",false,Set.of("title","subtitle","content"),Set.of("title")),
    SITE_CONFIG("site_config",false,Set.of("value"),Set.of("value"));
    final String table;final boolean softDeleted;final Set<String> fields,required;
    TranslationResource(String table,boolean softDeleted,Set<String> fields,Set<String> required) { this.table=table;this.softDeleted=softDeleted;this.fields=fields;this.required=required; }
    int maxLength(String field) { return switch(field) {case "name","title"->200;case "subtitle"->500;case "summary","application_scene","requirement","solution"->10000;default->100000;}; }
}
