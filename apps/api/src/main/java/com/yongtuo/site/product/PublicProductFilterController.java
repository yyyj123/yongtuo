package com.yongtuo.site.product;
import com.yongtuo.site.common.ApiResponse;
import java.util.List;
import java.util.Locale;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/v1/public/product-filters")
public class PublicProductFilterController {
    private final PublicProductFilterService service;
    public PublicProductFilterController(PublicProductFilterService service) { this.service = service; }
    @GetMapping
    public ApiResponse<List<PublicProductFilterService.Filter>> filters(@RequestParam(required = false) String category, Locale locale) { return service.filters(category, locale); }
}
