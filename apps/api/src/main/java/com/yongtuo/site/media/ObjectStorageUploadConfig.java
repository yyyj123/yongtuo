package com.yongtuo.site.media;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ObjectStorageUploadConfig {

    @Bean
    @ConditionalOnMissingBean(ObjectStorageUploadService.class)
    ObjectStorageUploadService unavailableObjectStorageUploadService() {
        return new ObjectStorageUploadService() {
            @Override
            public StoredObject upload(String storageKey, String originalName,
                                       String mimeType, byte[] content) {
                throw new com.yongtuo.site.common.BusinessException(50001,"FEATURE_NOT_CONFIGURED",org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE);
            }

            @Override
            public void delete(String storageKey) {}
        };
    }
}
