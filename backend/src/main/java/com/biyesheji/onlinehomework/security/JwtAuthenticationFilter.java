package com.biyesheji.onlinehomework.security;

import com.biyesheji.onlinehomework.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 认证过滤器
 * 每当收到请求时都会执行此过滤器，用于验证 JWT token 并设置用户认证信息
 * 该过滤器是 Spring Security 链的一部分，在请求到达 Controller 之前拦截并处理认证
 *
 * @author毕业设计
 * @version1.0.0
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    /**
     * JWT 工具类，用于 token 的生成、验证和解析
     */
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 用户详情服务，用于根据用户名加载用户信息
     */
    @Autowired
    private UserDetailsService userDetailsService;

    /**
     * 核心过滤方法，处理每个 HTTP 请求的 JWT 认证
     * 从请求头中提取 JWT token，验证其有效性，并设置 Spring Security 上下文
     *
     * @param request HTTP 请求对象，包含请求头等信息
     * @param response HTTP 响应对象
     * @param filterChain 过滤器链，用于将请求传递给下一个过滤器
     * @throws ServletException 如果 servlet 处理请求时发生错误
     * @throws IOException 如果发生输入输出错误
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {
        try {
            // 从请求中获取 JWT token
            String jwt = getJwtFromRequest(request);

            // 如果 token 存在且有效
            if (StringUtils.hasText(jwt) && jwtUtil.validateToken(jwt)) {
                // 从 token 中提取用户名
                String username = jwtUtil.extractUsername(jwt);

                // 使用用户详情服务加载用户完整信息
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                // 从 JWT 中提取 userId（自定义 claims）
                Long userId = jwtUtil.extractUserId(jwt);

                // 创建一个包含 username、userId 和 role 的 Map，作为认证主体的附加信息
                // 这样在 Controller 中可以通过 SecurityContextHolder 获取用户信息
                Map<String, Object> principal = new HashMap<>();
                principal.put("username", username);
                principal.put("userId", userId);
                principal.put("role", jwtUtil.extractRole(jwt));

                // 创建认证令牌，包含用户信息、密码（为 null）和用户权限
                UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, null, userDetails.getAuthorities());

                // 设置认证的详细信息，包括请求的 IP 地址、session ID 等
                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                // 将认证信息设置到 Spring Security 上下文中
                // 这样后续的代码可以通过 SecurityContextHolder.getContext().getAuthentication() 获取当前用户信息
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (Exception ex) {
            // 如果认证过程中发生任何异常，记录错误但不影响请求继续处理
            logger.error("无法在安全上下文中设置用户认证信息", ex);
        }

        // 将请求和响应传递给过滤器链中的下一个过滤器
        // 如果没有认证信息，后续的 Security 配置可能会拒绝访问
        filterChain.doFilter(request, response);
    }

    /**
     * 从 HTTP 请求头中提取 JWT token
     * JWT token 通常在 Authorization 头中，格式为 "Bearer <token>"
     *
     * @param request HTTP 请求对象
     * @return 提取的 JWT token字符串，如果不存在则返回 null
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        // 获取 Authorization 请求头
        String bearerToken = request.getHeader("Authorization");

        // 检查 token 是否存在且以 "Bearer " 开头
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            // 去掉 "Bearer " 前缀，返回实际的 token 值
            return bearerToken.substring(7);
        }
        return null;
    }
}