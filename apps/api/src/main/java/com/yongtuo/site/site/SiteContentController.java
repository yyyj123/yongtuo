package com.yongtuo.site.site;
import com.yongtuo.site.common.ApiResponse;
import com.yongtuo.site.content.home.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.web.bind.annotation.*;
@RestController
public class SiteContentController {
    private final ContactService contact;private final SiteConfigService site;private final HomeService home;
    public SiteContentController(ContactService contact,SiteConfigService site,HomeService home) { this.contact=contact;this.site=site;this.home=home; }
    @GetMapping("/api/v1/public/contact") public ApiResponse<?> contact(Locale locale) { return ApiResponse.success(contact.publicList(en(locale))); }
    @GetMapping("/api/v1/public/site") public ApiResponse<?> site(Locale locale) { return ApiResponse.success(Map.of("config",site.publicConfig(en(locale)),"contact",contact.publicList(en(locale)))); }
    @GetMapping("/api/v1/public/home") public ApiResponse<?> home(Locale locale) { return ApiResponse.success(home.publicHome(en(locale))); }
    @GetMapping("/api/v1/public/cnc-machining") public ApiResponse<?> cnc(Locale locale) { return ApiResponse.success(home.section("CNC",en(locale))); }
    @GetMapping("/api/v1/public/capabilities") public ApiResponse<?> capabilities(Locale locale) { return ApiResponse.success(home.section("CAPABILITIES",en(locale))); }
    @GetMapping("/api/v1/admin/contact") public ApiResponse<?> contactAdmin() { return ApiResponse.success(contact.adminList()); }
    @PutMapping("/api/v1/admin/contact") public ApiResponse<?> updateContact(@Valid @RequestBody List<@NotNull @Valid ContactInput> input) { return ApiResponse.success(contact.replace(input)); }
    @GetMapping("/api/v1/admin/site") public ApiResponse<?> siteAdmin() { return ApiResponse.success(site.admin()); }
    @PutMapping("/api/v1/admin/site") public ApiResponse<?> updateSite(@Valid @RequestBody Map<String,@NotNull @Valid ConfigValueInput> input) { return ApiResponse.success(site.update(input)); }
    @GetMapping("/api/v1/admin/home") public ApiResponse<?> homeAdmin() { return ApiResponse.success(home.admin()); }
    @PutMapping("/api/v1/admin/home/business") public ApiResponse<?> business(@Valid @RequestBody List<@NotNull @Valid BusinessEntryInput> input) { return ApiResponse.success(home.updateBusiness(input)); }
    @PutMapping("/api/v1/admin/home/featured-{kind}") public ApiResponse<?> featured(@PathVariable String kind,@RequestBody List<Long> ids) { return ApiResponse.success(home.replaceFeatured(kind,ids)); }
    @PutMapping("/api/v1/admin/home/{section}") public ApiResponse<?> section(@PathVariable String section,@Valid @RequestBody HomeSectionInput input) { return ApiResponse.success(home.updateSection(section,input)); }
    private static boolean en(Locale locale) { return "en".equals(locale.getLanguage()); }
}
