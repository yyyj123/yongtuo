package com.yongtuo.site.search;

import com.yongtuo.site.common.ApiResponse;
import com.yongtuo.site.product.query.PublicProductPage;
import java.util.List;
import java.util.Locale;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/search")
public class PublicSearchController {
    private final SearchService service;

    public PublicSearchController(SearchService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<PublicProductPage> search(@RequestParam(required = false) String keyword, Locale locale) {
        return ApiResponse.success(service.search(keyword, locale));
    }

    @GetMapping("/suggestions")
    public ApiResponse<List<SearchSuggestion>> suggestions(@RequestParam(required = false) String q, Locale locale) {
        return ApiResponse.success(service.suggestions(q, locale));
    }
}
