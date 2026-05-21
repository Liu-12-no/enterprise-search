package com.qych.interceptor;

import com.qych.utils.JwtUtils;
import com.qych.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Slf4j
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        //优先从请求头 (Header) 获取 Token
        String token = request.getHeader("Authorization");

        //从url中获取token
        if(!StringUtils.hasText(token)){
            token = request.getParameter("token");
        }

        //兼容处理 "Bearer " 前缀规范
        if (StringUtils.hasText(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        //如果未携带token，返回false并返回401代码
        if(!StringUtils.hasText(token)){
            log.warn("请求未携带 Token，拦截 URI: {}", request.getRequestURI());
            response.setStatus(401);
            return false;
        }

        try {
            //解析token，返回userId
            Long userId = jwtUtils.getUserIdFromToken(token);

            //把userId存入当前线程
            UserContext.setUserId(userId);

            return true;
        } catch (Exception e) {
            log.error("Token解析失败或已过期", e);
            response.setStatus(401);
            return false; // 验证失败，拦截！
        }

    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //请求处理完毕，清空线程
        UserContext.remove();
    }
}
