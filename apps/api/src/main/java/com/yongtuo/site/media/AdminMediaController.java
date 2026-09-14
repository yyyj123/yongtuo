package com.yongtuo.site.media;
import com.yongtuo.site.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import com.yongtuo.site.media.AdminMediaService.ImageInput;
import com.yongtuo.site.media.AdminMediaService.AttachmentInput;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
@RestController
public class AdminMediaController {
 private final AdminMediaService service;public AdminMediaController(AdminMediaService service){this.service=service;}
 @PostMapping("/api/v1/admin/media") public ApiResponse<?> upload(@RequestParam("file") MultipartFile file){return ApiResponse.success(service.upload(file));}
 @GetMapping("/api/v1/admin/products/{id}/images") public ApiResponse<?> images(@PathVariable long id){return ApiResponse.success(service.images(id));}
 @PutMapping("/api/v1/admin/products/{id}/images") public ApiResponse<?> images(@PathVariable long id,@Valid @RequestBody List<@NotNull @Valid ImageInput> input){return ApiResponse.success(service.images(id,input));}
 @GetMapping("/api/v1/admin/products/{id}/attachments") public ApiResponse<?> attachments(@PathVariable long id){return ApiResponse.success(service.attachments(id));}
 @PutMapping("/api/v1/admin/products/{id}/attachments") public ApiResponse<?> attachments(@PathVariable long id,@Valid @RequestBody List<@NotNull @Valid AttachmentInput> input){return ApiResponse.success(service.attachments(id,input));}
}
