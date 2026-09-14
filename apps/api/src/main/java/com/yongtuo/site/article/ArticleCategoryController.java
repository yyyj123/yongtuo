package com.yongtuo.site.article;

import com.yongtuo.site.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import org.springframework.web.bind.annotation.*;

@RestController
public class ArticleCategoryController {
    private final ArticleCategoryService service;
    public ArticleCategoryController(ArticleCategoryService service) { this.service = service; }
    @GetMapping("/api/v1/admin/article-categories")
    public ApiResponse<List<ArticleCategory>> list() { return ApiResponse.success(service.list()); }
    @PostMapping("/api/v1/admin/article-categories")
    public ApiResponse<ArticleCategory> create(@Valid @RequestBody ArticleCategoryInput input) { return ApiResponse.success(service.save(null,input)); }
    @PutMapping("/api/v1/admin/article-categories/{id}")
    public ApiResponse<ArticleCategory> update(@PathVariable long id,@Valid @RequestBody ArticleCategoryInput input) { return ApiResponse.success(service.save(id,input)); }
    @DeleteMapping("/api/v1/admin/article-categories/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) { service.delete(id); return ApiResponse.success(null); }
    @GetMapping("/api/v1/public/article-categories")
    public ApiResponse<List<Map<String,Object>>> publicList(Locale locale) { return ApiResponse.success(service.publicList("en".equals(locale.getLanguage()))); }
}
