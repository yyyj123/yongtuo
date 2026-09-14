package com.yongtuo.site.auth;

import com.yongtuo.site.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/auth")
public class AuthController {
    private final AuthService service;

    public AuthController(AuthService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public ApiResponse<AuthTokens> login(@Valid @RequestBody LoginRequest request,
                                         jakarta.servlet.http.HttpServletRequest servletRequest) {
        return ApiResponse.success(service.login(request, servletRequest.getRemoteAddr(),
                servletRequest.getHeader("User-Agent")));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthTokens> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(service.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request,
                                    @AuthenticationPrincipal Jwt jwt) {
        service.logout(request, jwt);
        return ApiResponse.success(null);
    }

    @GetMapping("/me")
    public ApiResponse<AuthMe> me(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.success(service.me(jwt));
    }

    @PutMapping("/password")
    public ApiResponse<Void> password(@Valid @RequestBody PasswordChangeRequest request,
                                      @AuthenticationPrincipal Jwt jwt) {
        service.changePassword(request, jwt);
        return ApiResponse.success(null);
    }
}
