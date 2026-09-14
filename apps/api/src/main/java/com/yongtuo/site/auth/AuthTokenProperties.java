package com.yongtuo.site.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("security.jwt")
public record AuthTokenProperties(
        @NotBlank @Size(min = 32) String accessSecret,
        @NotBlank @Size(min = 32) String refreshSecret,
        @NotNull Duration accessTtl,
        @NotNull Duration refreshTtl) {

    public AuthTokenProperties {
        requirePositive(accessTtl, "accessTtl");
        requirePositive(refreshTtl, "refreshTtl");
    }

    private static void requirePositive(Duration value, String name) {
        if (value != null && (value.isZero() || value.isNegative())) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }
}
