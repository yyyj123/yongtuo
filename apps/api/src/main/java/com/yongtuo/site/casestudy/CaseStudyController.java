package com.yongtuo.site.casestudy;
import com.yongtuo.site.common.ApiResponse;
import com.yongtuo.site.content.ContentPage;
import jakarta.validation.Valid;
import java.util.Locale;
import org.springframework.web.bind.annotation.*;
@RestController
public class CaseStudyController {
    private final CaseStudyService service;
    public CaseStudyController(CaseStudyService service) { this.service=service; }
    @GetMapping("/api/v1/admin/cases") public ApiResponse<ContentPage<?>> adminList(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="24") int pageSize) { return ApiResponse.success(service.list(false,false,page,pageSize)); }
    @GetMapping("/api/v1/admin/cases/{id}") public ApiResponse<CaseAdminDetail> get(@PathVariable long id) { return ApiResponse.success(service.get(id)); }
    @PostMapping("/api/v1/admin/cases") public ApiResponse<CaseAdminDetail> create(@Valid @RequestBody CaseStudyInput input) { return ApiResponse.success(service.save(null,input)); }
    @PutMapping("/api/v1/admin/cases/{id}") public ApiResponse<CaseAdminDetail> update(@PathVariable long id,@Valid @RequestBody CaseStudyInput input) { return ApiResponse.success(service.save(id,input)); }
    @DeleteMapping("/api/v1/admin/cases/{id}") public ApiResponse<Void> delete(@PathVariable long id) { service.delete(id);return ApiResponse.success(null); }
    @GetMapping("/api/v1/public/cases") public ApiResponse<ContentPage<?>> list(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="24") int pageSize,Locale locale) { return ApiResponse.success(service.list(true,"en".equals(locale.getLanguage()),page,pageSize)); }
    @GetMapping("/api/v1/public/cases/{slug}") public ApiResponse<PublicCaseStudy> detail(@PathVariable String slug,Locale locale) { return ApiResponse.success(service.publicDetail(slug,"en".equals(locale.getLanguage()))); }
}
