package com.yongtuo.site.product;

import com.yongtuo.site.common.ApiResponse;
import com.yongtuo.site.product.query.ProductFilter;
import com.yongtuo.site.product.query.ProductQueryService;
import com.yongtuo.site.product.query.PublicProductDto;
import com.yongtuo.site.product.query.PublicProductPage;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/products")
public class PublicProductController {
    private final ProductQueryService service;

    public PublicProductController(ProductQueryService service) { this.service = service; }

    @GetMapping
    public ApiResponse<PublicProductPage> list(@RequestParam MultiValueMap<String, String> parameters, Locale locale) {
        Map<String, List<String>> attributes = new LinkedHashMap<>();
        parameters.forEach((key, values) -> {
            if (key.startsWith("attr.")) attributes.put(key.substring(5), values);
        });
        ProductFilter filter = new ProductFilter(parameters.getFirst("category"), attributes,
                integer(parameters.getFirst("page")), integer(parameters.getFirst("pageSize")), parameters.getFirst("sort"));
        return ApiResponse.success(service.list(filter, locale, parameters.getFirst("keyword")));
    }

    @GetMapping("/{slug}")
    public ApiResponse<PublicProductDto> detail(@PathVariable String slug, Locale locale) {
        return ApiResponse.success(service.detail(slug, locale));
    }

    private static Integer integer(String value) {
        if (value == null || value.isBlank()) return null;
        try { return Integer.valueOf(value); }
        catch (NumberFormatException exception) { throw com.yongtuo.site.product.query.ProductQueryException.invalidFilter(); }
    }
}
