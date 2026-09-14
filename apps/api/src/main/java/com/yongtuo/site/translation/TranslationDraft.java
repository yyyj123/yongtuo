package com.yongtuo.site.translation;
import java.util.Map;
public record TranslationDraft(Map<String,String> fields) { public TranslationDraft { fields=Map.copyOf(fields); } }
