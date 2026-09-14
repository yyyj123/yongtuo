package com.yongtuo.site.audit;
import com.yongtuo.site.common.ApiResponse;
import com.yongtuo.site.common.BusinessException;
import com.yongtuo.site.content.ContentPage;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
@RestController
public class OperationLogController {
    private final JdbcTemplate jdbc;
    public OperationLogController(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @GetMapping("/api/v1/admin/operation-logs")
    public ApiResponse<?> list(@RequestParam(defaultValue="1") int page,@RequestParam(defaultValue="24") int pageSize) {
        if(page<1||pageSize<1||pageSize>100) throw new BusinessException(35001,"Invalid pagination",HttpStatus.BAD_REQUEST);
        long total=jdbc.queryForObject("SELECT COUNT(*) FROM admin_operation_log",Long.class);
        var items=jdbc.queryForList("SELECT id,admin_user_id,operation,resource_type,resource_id,detail,ip_address,created_at FROM admin_operation_log ORDER BY id DESC LIMIT ? OFFSET ?",pageSize,(long)(page-1)*pageSize);
        return ApiResponse.success(new ContentPage<>(items,page,pageSize,total,(total+pageSize-1)/pageSize));
    }
}
