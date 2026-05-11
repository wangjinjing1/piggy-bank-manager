package com.piggybank.manager.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final IpAccessInterceptor ipAccessInterceptor;
    private final AuthInterceptor authInterceptor;

    public WebConfig(IpAccessInterceptor ipAccessInterceptor, AuthInterceptor authInterceptor) {
        this.ipAccessInterceptor = ipAccessInterceptor;
        this.authInterceptor = authInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(ipAccessInterceptor).addPathPatterns("/**");
        // 登录和匿名借款填写不需要登录，其余 API 都需要通过 AuthInterceptor 校验。
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login", "/api/public/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 前端页面直接放在 static 目录，由同一个 Spring Boot 应用托管。
        registry.addResourceHandler("/**").addResourceLocations("classpath:/static/");
    }
}
