package com.yongtuo.site.catalog;
import com.yongtuo.site.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import org.springframework.web.bind.annotation.*;
@RestController
public class CatalogController {
    private final CatalogService service;
    public CatalogController(CatalogService service) { this.service=service; }
    @GetMapping("/api/v1/admin/catalogs") public ApiResponse<List<Catalog>> list() { return ApiResponse.success(service.list()); }
    @GetMapping("/api/v1/admin/catalogs/{id}") public ApiResponse<Catalog> get(@PathVariable long id) { return ApiResponse.success(service.get(id)); }
    @PostMapping("/api/v1/admin/catalogs") public ApiResponse<Catalog> create(@Valid @RequestBody CatalogInput input) { return ApiResponse.success(service.save(null,input)); }
    @PutMapping("/api/v1/admin/catalogs/{id}") public ApiResponse<Catalog> update(@PathVariable long id,@Valid @RequestBody CatalogInput input) { return ApiResponse.success(service.save(id,input)); }
    @PutMapping("/api/v1/admin/catalogs/{id}/primary") public ApiResponse<Catalog> primary(@PathVariable long id) { return ApiResponse.success(service.primary(id)); }
    @DeleteMapping("/api/v1/admin/catalogs/{id}") public ApiResponse<Void> delete(@PathVariable long id) { service.delete(id);return ApiResponse.success(null); }
    @GetMapping("/api/v1/public/catalogs") public ApiResponse<List<PublicCatalog>> publicList(Locale locale) { return ApiResponse.success(service.publicList("en".equals(locale.getLanguage()))); }
}
