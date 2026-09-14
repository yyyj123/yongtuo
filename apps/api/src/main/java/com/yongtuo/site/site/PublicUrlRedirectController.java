package com.yongtuo.site.site;

import com.yongtuo.site.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/redirects")
public class PublicUrlRedirectController {
    private final UrlRedirectService service;

    public PublicUrlRedirectController(UrlRedirectService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<UrlRedirect>> resolve(@RequestParam String path) {
        return service.resolve(path)
                .map(redirect -> ResponseEntity.ok(ApiResponse.success(redirect)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Not Found", null)));
    }
}
