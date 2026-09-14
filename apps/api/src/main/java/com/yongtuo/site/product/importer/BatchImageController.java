package com.yongtuo.site.product.importer;

import com.yongtuo.site.common.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/products/images")
public class BatchImageController {
    private final BatchImageService service;

    public BatchImageController(BatchImageService service) {
        this.service = service;
    }

    @PostMapping("/batch")
    public ApiResponse<BatchImageReport> upload(@RequestParam("files") List<MultipartFile> files) {
        return ApiResponse.success(service.upload(files));
    }
}
