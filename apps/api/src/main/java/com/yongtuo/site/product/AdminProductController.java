package com.yongtuo.site.product;

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
@RequestMapping("/api/v1/admin/products")
public class AdminProductController {
    private final ProductService service;

    public AdminProductController(ProductService service) { this.service = service; }

    @GetMapping
    public ApiResponse<List<ProductDto>> list() { return ApiResponse.success(service.getAdminList()); }

    @GetMapping("/{id}")
    public ApiResponse<ProductDto> detail(@PathVariable long id) {
        return ApiResponse.success(service.getAdminById(id).orElseThrow(ProductBusinessException::notFound));
    }

    @PostMapping
    public ApiResponse<ProductDto> create(@Valid @RequestBody AdminProductCreateRequest request) {
        return ApiResponse.success(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ProductDto> update(@PathVariable long id,
                                           @Valid @RequestBody AdminProductUpdateRequest request) {
        return ApiResponse.success(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        service.softDelete(id); return ApiResponse.success(null);
    }

    @PostMapping("/{id}/duplicate")
    public ApiResponse<ProductDto> duplicate(@PathVariable long id) {
        return ApiResponse.success(service.duplicate(id));
    }
}
