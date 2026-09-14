package com.yongtuo.site.auth;

public record AuthTokens(String accessToken, String refreshToken,
                         String tokenType, long expiresInSeconds) {
}
