package com.yongtuo.site.certificate;
import com.yongtuo.site.common.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Locale;
import org.springframework.web.bind.annotation.*;
@RestController
public class CertificateController {
    private final CertificateService service;
    public CertificateController(CertificateService service) { this.service=service; }
    @GetMapping("/api/v1/admin/certificates") public ApiResponse<List<Certificate>> list() { return ApiResponse.success(service.list()); }
    @GetMapping("/api/v1/admin/certificates/{id}") public ApiResponse<Certificate> get(@PathVariable long id) { return ApiResponse.success(service.get(id)); }
    @PostMapping("/api/v1/admin/certificates") public ApiResponse<Certificate> create(@Valid @RequestBody CertificateInput input) { return ApiResponse.success(service.save(null,input)); }
    @PutMapping("/api/v1/admin/certificates/{id}") public ApiResponse<Certificate> update(@PathVariable long id,@Valid @RequestBody CertificateInput input) { return ApiResponse.success(service.save(id,input)); }
    @DeleteMapping("/api/v1/admin/certificates/{id}") public ApiResponse<Void> delete(@PathVariable long id) { service.delete(id);return ApiResponse.success(null); }
    @GetMapping("/api/v1/public/certificates") public ApiResponse<List<PublicCertificate>> publicList(Locale locale) { return ApiResponse.success(service.publicList("en".equals(locale.getLanguage()))); }
}
