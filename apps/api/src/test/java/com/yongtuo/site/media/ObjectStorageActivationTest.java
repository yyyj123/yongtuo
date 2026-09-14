package com.yongtuo.site.media;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {DisabledObjectStorageService.class, LocalTestObjectStorageService.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ObjectStorageActivationTest {
    @Autowired ObjectStorageService storage;

    @Test
    void noExplicitProfileStartsWithSafeDisabledStorage() {
        assertThat(storage).isInstanceOf(DisabledObjectStorageService.class);
        assertThat(storage.resolveDownloadUrl("private/key.pdf", "https://cdn.example.test/file.pdf")).isEmpty();
    }
}

@SpringBootTest(classes = {DisabledObjectStorageService.class, LocalTestObjectStorageService.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("production")
class ProductionObjectStorageActivationTest {
    @Autowired ObjectStorageService storage;

    @Test
    void productionProfileStartsWithSafeDisabledStorage() {
        assertThat(storage).isInstanceOf(DisabledObjectStorageService.class);
        assertThat(storage.resolveDownloadUrl("private/key.pdf", "https://cdn.example.test/file.pdf")).isEmpty();
    }
}

@SpringBootTest(classes = {DisabledObjectStorageService.class, LocalTestObjectStorageService.class,
        ExternalStorageTestConfiguration.class}, webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "yongtuo.storage.provider=external-test")
@ActiveProfiles("production")
class ExternalObjectStorageActivationTest {
    @Autowired Map<String, ObjectStorageService> adapters;

    @Test
    void explicitProviderReplacesDisabledAdapterWithoutCreatingMultipleBeans() {
        assertThat(adapters).containsOnlyKeys("externalTestObjectStorageService");
    }
}

@TestConfiguration(proxyBeanMethods = false)
class ExternalStorageTestConfiguration {
    @Bean
    @ConditionalOnProperty(prefix = "yongtuo.storage", name = "provider", havingValue = "external-test")
    ObjectStorageService externalTestObjectStorageService() {
        return (storageKey, storedPublicUrl) -> Optional.of("https://signed.example.test/file.pdf");
    }
}
