package com.yongtuo.site.translation;
import com.yongtuo.site.common.BusinessException;
import org.springframework.http.HttpStatus;
public class DisabledTranslationProvider implements TranslationProvider {
    public TranslationDraft translate(TranslationRequest request) { throw new BusinessException(50001,"FEATURE_NOT_CONFIGURED",HttpStatus.SERVICE_UNAVAILABLE); }
}
