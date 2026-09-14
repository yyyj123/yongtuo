package com.yongtuo.site.product.importer;

import com.yongtuo.site.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/products/import")
public class ProductImportController {
    private final ProductImportService service;

    public ProductImportController(ProductImportService service) {
        this.service = service;
    }

    @org.springframework.web.bind.annotation.GetMapping("/template")
    public org.springframework.http.ResponseEntity<byte[]> template() {
        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=yongtuo-products-template.xlsx")
                .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(ProductImportTemplate.create());
    }

    @PostMapping("/preview")
    public ApiResponse<ProductImportPreview> preview(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success(service.preview(file));
    }

    @PostMapping("/confirm")
    public ApiResponse<ProductImportConfirmation> confirm(
            @Valid @RequestBody ProductImportConfirmRequest request) {
        return ApiResponse.success(service.confirm(request.importToken()));
    }
}
