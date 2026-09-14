package com.yongtuo.site.attribute;

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
@RequestMapping("/api/v1/admin/attributes")
public class AdminAttributeController {
    private final AttributeDefinitionService service;

    public AdminAttributeController(AttributeDefinitionService service) { this.service = service; }

    @GetMapping
    public ApiResponse<List<AdminAttributeDto>> list() { return ApiResponse.success(service.list()); }

    @GetMapping("/{id}")
    public ApiResponse<AdminAttributeDto> get(@PathVariable long id) { return ApiResponse.success(service.get(id)); }

    @PostMapping
    public ApiResponse<AdminAttributeDto> create(@Valid @RequestBody AdminAttributeWriteRequest request) {
        return ApiResponse.success(service.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminAttributeDto> update(@PathVariable long id,
                                                  @Valid @RequestBody AdminAttributeWriteRequest request) {
        return ApiResponse.success(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ApiResponse.success(null);
    }
}
