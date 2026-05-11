package com.piggybank.manager.config;

import com.piggybank.manager.dto.UserPrincipal;
import com.piggybank.manager.service.UserService;
import com.piggybank.manager.util.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final UserService userService;

    public AuthInterceptor(UserService userService) {
        this.userService = userService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        // 所有 /api/** 业务接口统一使用 Bearer token 鉴权，匿名填写接口在 WebConfig 中排除。
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "未登录");
            return false;
        }
        UserPrincipal principal = userService.parseToken(header.substring(7));
        if (principal == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "登录已失效");
            return false;
        }
        AuthContext.set(principal);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }
}
