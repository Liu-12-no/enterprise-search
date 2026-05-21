package com.qych.config;

import com.qych.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor; // 把你刚才写的安检员请过来

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                // 1. 规定他管辖的范围：拦截所有 /api 开头的请求
                .addPathPatterns("/api/**")              
                
                // 2. 给他一份“免检白名单”
                .excludePathPatterns(
                        "/api/user/login",               // 登录接口绝对不能拦

                        "/api/enterprise/**",           //放行企业查询相关接口

                        "/api/ai/summary/company",

                        "/v2/api-docs",                  // 下面全是放行 Knife4j (Swagger) 接口文档
                        "/swagger-resources/**",
                        "/swagger-ui/**",
                        "/doc.html",
                        "/webjars/**"
                );
    }
}