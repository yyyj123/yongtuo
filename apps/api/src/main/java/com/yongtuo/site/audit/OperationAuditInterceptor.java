package com.yongtuo.site.audit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import tools.jackson.databind.ObjectMapper;

/** Logs route metadata only. Never reads request bodies, query strings, headers or files. */
public class OperationAuditInterceptor implements HandlerInterceptor {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(OperationAuditInterceptor.class);
    private final JdbcTemplate jdbc;private final ObjectMapper mapper;
    public OperationAuditInterceptor(JdbcTemplate jdbc,ObjectMapper mapper) { this.jdbc=jdbc;this.mapper=mapper; }
    @Override public void afterCompletion(HttpServletRequest request,HttpServletResponse response,Object handler,Exception failure) {
        if(!(handler instanceof HandlerMethod)||failure!=null||response.getStatus()<200||response.getStatus()>=300
                ||!Set.of("POST","PUT","PATCH","DELETE").contains(request.getMethod())) return;
        Object routeValue=request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if(routeValue==null) return;
        String route=routeValue.toString();
        if(!route.startsWith("/api/v1/admin/")||route.startsWith("/api/v1/admin/auth/")) return;
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if(auth==null||!auth.isAuthenticated()) return;
        Long adminId=auth.getPrincipal() instanceof Jwt jwt?number(jwt.getSubject()):null;
        Long resourceId=null;
        if(request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) instanceof Map<?,?> variables)
            resourceId=number(variables.get("id"));
        String type=route.substring("/api/v1/admin/".length()).split("/")[0];
        try {
            jdbc.update("INSERT INTO admin_operation_log(admin_user_id,operation,resource_type,resource_id,detail,ip_address) VALUES (?,?,?,?,?,?)",
                    adminId,truncate(request.getMethod()+" "+route,100),truncate(type,100),resourceId,
                    mapper.writeValueAsString(Map.of("method",request.getMethod(),"route",route)),truncate(request.getRemoteAddr(),45));
        }catch(RuntimeException exception) {
            // Business work has already committed. Report audit failure without leaking request data.
            LOG.warn("Unable to persist admin operation audit metadata");
        }
    }
    private static Long number(Object value) { try{return value==null?null:Long.valueOf(value.toString());}catch(NumberFormatException ex){return null;} }
    private static String truncate(String text,int max) { return text==null?null:text.substring(0,Math.min(text.length(),max)); }
}
