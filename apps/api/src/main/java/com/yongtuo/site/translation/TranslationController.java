package com.yongtuo.site.translation;
import com.yongtuo.site.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/admin/translation")
public class TranslationController {
    private final TranslationService service;
    public TranslationController(TranslationService service) { this.service=service; }
    @PostMapping("/confirm") public ApiResponse<?> confirm(@Valid @RequestBody ConfirmTranslationRequest request) { return ApiResponse.success(service.confirm(request)); }
}
