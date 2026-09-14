package com.yongtuo.site.category;

import com.yongtuo.site.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/categories")
public class AdminCategoryController {
    private final CategoryService service;

    public AdminCategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping("/tree")
    public ApiResponse<List<AdminCategoryDto>> tree() {
        return ApiResponse.success(service.getAdminTree());
    }

    @PostMapping
    public ApiResponse<AdminCategoryDto> create(@Valid @RequestBody AdminCategoryWriteRequest request) {
        return ApiResponse.success(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminCategoryDto> update(@PathVariable long id,
                                                 @Valid @RequestBody AdminCategoryWriteRequest request) {
        return ApiResponse.success(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        service.softDelete(id);
        return ApiResponse.success(null);
    }

    @PutMapping("/sort")
    public ApiResponse<Void> sort(@RequestBody List<@Valid CategorySortItem> items) {
        service.sort(items);
        return ApiResponse.success(null);
    }
}
