package com.yongtuo.site.article;

import com.yongtuo.site.common.ApiResponse;
import com.yongtuo.site.content.ContentPage;
import jakarta.validation.Valid;
import java.util.Locale;
import org.springframework.web.bind.annotation.*;

@RestController
public class ArticleController {
    private final ArticleService service;
    public ArticleController(ArticleService service) { this.service = service; }
    @GetMapping("/api/v1/admin/articles")
    public ApiResponse<ContentPage<?>> adminList(@RequestParam(defaultValue="1") int page,
            @RequestParam(defaultValue="24") int pageSize) { return ApiResponse.success(service.list(false,false,page,pageSize)); }
    @GetMapping("/api/v1/admin/articles/{id}")
    public ApiResponse<Article> get(@PathVariable long id) { return ApiResponse.success(service.get(id)); }
    @PostMapping("/api/v1/admin/articles")
    public ApiResponse<Article> create(@Valid @RequestBody ArticleInput input) { return ApiResponse.success(service.save(null,input)); }
    @PutMapping("/api/v1/admin/articles/{id}")
    public ApiResponse<Article> update(@PathVariable long id,@Valid @RequestBody ArticleInput input) { return ApiResponse.success(service.save(id,input)); }
    @DeleteMapping("/api/v1/admin/articles/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) { service.delete(id); return ApiResponse.success(null); }
    @GetMapping("/api/v1/public/articles")
    public ApiResponse<ContentPage<?>> list(@RequestParam(defaultValue="1") int page,
            @RequestParam(defaultValue="24") int pageSize, @RequestParam(required=false) String category, Locale locale) {
        return ApiResponse.success(service.publicList("en".equals(locale.getLanguage()),page,pageSize,category));
    }
    @GetMapping("/api/v1/public/articles/{slug}")
    public ApiResponse<PublicArticle> detail(@PathVariable String slug,Locale locale) {
        return ApiResponse.success(service.publicDetail(slug,"en".equals(locale.getLanguage())));
    }
}
