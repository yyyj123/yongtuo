package com.yongtuo.site.translation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
@Configuration
public class TranslationConfiguration {
    @Bean @ConditionalOnMissingBean(TranslationProvider.class)
    TranslationProvider translationProvider() { return new DisabledTranslationProvider(); }
}
