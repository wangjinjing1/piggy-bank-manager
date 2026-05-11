package com.piggybank.manager.config;

import com.piggybank.manager.service.IpSecurityService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class IpAccessInterceptor implements HandlerInterceptor {
    private final IpSecurityService ipSecurityService;

    public IpAccessInterceptor(IpSecurityService ipSecurityService) {
        this.ipSecurityService = ipSecurityService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ip = clientIp(request);
        if (ipSecurityService.isBlacklisted(ip)) {
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"当前 IP 已被加入黑名单\"}");
            return false;
        }
        ipSecurityService.recordVisit(ip);
        return true;
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(realIp)) {
            return realIp;
        }
        return request.getRemoteAddr();
    }
}
