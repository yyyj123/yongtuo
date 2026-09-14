package com.yongtuo.site.translation;
import java.util.Map;
public record TranslationRequest(String resourceType,long resourceId,Map<String,String> sourceFields) {
    public TranslationRequest { sourceFields=Map.copyOf(sourceFields); }
}
