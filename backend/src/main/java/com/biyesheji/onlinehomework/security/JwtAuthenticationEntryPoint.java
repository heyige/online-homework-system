package com.biyesheji.onlinehomework.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 认证入口点
 * 当用户尝试访问受保护的资源但未通过认证时，此组件会处理请求并返回适当的错误响应
 * 实现了 Spring Security 的 AuthenticationEntryPoint 接口，用于处理"未认证"情况
 *
 * @author毕业设计
 * @version1.0.0
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    /**
     * 处理未认证请求的核心方法
     * 当用户没有提供有效的 JWT token 或认证失败时，Spring Security 会调用此方法
     *
     * @param request HTTP 请求对象
     * @param response HTTP 响应对象
     * @param authException 认证异常对象，包含具体的认证失败原因
     * @throws IOException 如果发生输入输出错误
     * @throws ServletException 如果 servlet 处理请求时发生错误
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                        AuthenticationException authException) throws IOException, ServletException {
        // 设置响应内容类型为 JSON
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // 设置 HTTP 状态码为 401 Unauthorized（未授权）
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // 创建响应体的 Map 对象
        Map<String, Object> body = new HashMap<>();

        // 设置错误码：401 表示未授权
        body.put("code", 401);

        // 设置错误消息，格式为"未授权访问："加上具体的异常信息
        body.put("message", "未授权访问：" + authException.getMessage());

        // 设置请求路径，帮助前端定位是哪个接口出了问题
        body.put("path", request.getServletPath());

        // 使用 Jackson ObjectMapper 将响应体对象转换为 JSON 格式并写入响应输出流
        final ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(response.getOutputStream(), body);
    }
}