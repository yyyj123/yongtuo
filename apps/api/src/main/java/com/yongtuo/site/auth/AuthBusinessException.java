package com.yongtuo.site.auth;

import com.yongtuo.site.common.BusinessException;
import org.springframework.http.HttpStatus;

final class AuthBusinessException extends BusinessException {
    private AuthBusinessException(int code, String message, HttpStatus status) {
        super(code, message, status);
    }

    static AuthBusinessException loginFailed() {
        return new AuthBusinessException(10001, "Login failed", HttpStatus.UNAUTHORIZED);
    }

    static AuthBusinessException throttled() {
        return new AuthBusinessException(10005, "Login temporarily limited", HttpStatus.TOO_MANY_REQUESTS);
    }

    static AuthBusinessException invalidToken() {
        return new AuthBusinessException(10002, "Token expired or invalid", HttpStatus.UNAUTHORIZED);
    }

    static AuthBusinessException unauthorized() {
        return new AuthBusinessException(10003, "Unauthorized", HttpStatus.UNAUTHORIZED);
    }

    static AuthBusinessException invalidCurrentPassword() {
        return new AuthBusinessException(10004, "Current password is invalid", HttpStatus.BAD_REQUEST);
    }
}
