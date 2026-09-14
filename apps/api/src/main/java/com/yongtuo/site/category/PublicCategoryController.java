package com.yongtuo.site.category;

import com.yongtuo.site.common.ApiResponse;
import java.util.List;
import java.util.Locale;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/categories")
public class PublicCategoryController {
    private final CategoryService service;

    public PublicCategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<PublicCategoryDto>> tree(Locale locale) {
        return ApiResponse.success(service.getPublicTree(locale));
    }

    @GetMapping("/{slug}")
    public ApiResponse<PublicCategoryDto> detail(@PathVariable String slug, Locale locale) {
        return ApiResponse.success(service.getPublicBySlug(slug, locale));
    }
}
