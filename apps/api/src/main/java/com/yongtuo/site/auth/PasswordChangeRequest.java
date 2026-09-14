package com.yongtuo.site.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordChangeRequest(
        @NotBlank @Size(max = 200) String currentPassword,
        @NotBlank @Size(min = 12, max = 72) String newPassword) {
}
