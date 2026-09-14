package com.yongtuo.site.attribute;

import com.yongtuo.site.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/categories/{categoryId}/attributes")
public class AdminCategoryAttributeController {
    private final CategoryAttributeService service;

    public AdminCategoryAttributeController(CategoryAttributeService service) { this.service = service; }

    @GetMapping
    public ApiResponse<List<CategoryAttributeDto>> list(@PathVariable long categoryId) {
        return ApiResponse.success(service.getCategoryAttributes(categoryId));
    }

    @PutMapping
    public ApiResponse<List<CategoryAttributeDto>> replace(@PathVariable long categoryId,
            @RequestBody List<@Valid CategoryAttributeBindingRequest> requests) {
        return ApiResponse.success(service.replaceCategoryBindings(categoryId, requests));
    }
}
