package com.biyesheji.onlinehomework.config;

import com.biyesheji.onlinehomework.security.JwtAuthenticationFilter;
import com.biyesheji.onlinehomework.security.JwtAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Spring Security 安全配置类
 *
 * 描述：配置 Spring Security 的安全规则，包括：
 * - JWT 认证过滤器
 * - CORS 跨域配置
 * - 权限规则（哪些路径需要什么角色）
 * - 密码加密方式
 * - 会话管理策略
 *
 * @Configuration: 标记为 Spring 配置类
 * @EnableWebSecurity: 启用 Spring Security 安全配置
 * @EnableMethodSecurity: 启用方法级别的权限注解
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * JWT 认证过滤器
     * 用于在请求到达控制器前验证 JWT 令牌
     */
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * JWT 认证异常处理入口
     * 用于处理认证失败的情况
     */
    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    /**
     * 用户详情服务
     * 用于加载用户信息
     */
    @Autowired
    private UserDetailsService userDetailsService;

    /**
     * 配置安全过滤器链
     *
     * @param http HttpSecurity 配置对象
     * @return 安全过滤器链
     * @throws Exception 配置异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF（跨站请求伪造）防护，因为我们使用 JWT
            .csrf(AbstractHttpConfigurer::disable)
            // 配置 CORS（跨域）
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            // 配置异常处理
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(jwtAuthenticationEntryPoint))
            // 配置授权规则
            .authorizeHttpRequests(auth -> auth
                // 认证相关接口允许匿名访问
                .requestMatchers("/auth/**", "/public/**").permitAll()
                .requestMatchers("/users/verification-code", "/users/verify-code", "/users/reset-password").permitAll()
                // 管理员接口需要 ADMIN 角色
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // 教师接口需要 TEACHER 或 ADMIN 角色
                .requestMatchers("/teacher/**").hasAnyRole("TEACHER", "ADMIN")
                // 学生接口需要 STUDENT、TEACHER 或 ADMIN 角色
                .requestMatchers("/student/**").hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                // 学生作业接口需要学生或教师角色
                .requestMatchers("/homework/student/**").hasAnyRole("STUDENT", "TEACHER", "ADMIN")
                // 教师作业接口需要教师或管理员角色
                .requestMatchers("/homework/teacher/**").hasAnyRole("TEACHER", "ADMIN")
                // Swagger 文档接口允许匿名访问
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // 其他所有请求都需要认证
                .anyRequest().authenticated()
            )
            // 会话策略：无状态（不创建 Session）
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 配置认证提供者
            .authenticationProvider(authenticationProvider())
            // 在用户名密码认证过滤器前添加 JWT 过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 配置认证提供者
     * 使用 DaoAuthenticationProvider 从数据库加载用户信息
     *
     * @return 认证提供者 Bean
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * 配置认证管理器
     *
     * @param config 认证配置对象
     * @return 认证管理器 Bean
     * @throws Exception 配置异常
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * 配置密码编码器
     * 使用 BCrypt 算法加密密码
     *
     * @return 密码编码器 Bean
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 配置 CORS 跨域规则
     *
     * @return CORS 配置源 Bean
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // 允许的来源（前端地址）
        configuration.setAllowedOrigins(List.of("http://localhost:3000", "http://localhost:3003", "http://localhost:8080", "http://127.0.0.1:3000"));
        // 允许的 HTTP 方法
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        // 允许所有请求头
        configuration.setAllowedHeaders(Arrays.asList("*"));
        // 暴露 Authorization 响应头（用于返回 JWT）
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        // 允许携带凭证（如 Cookie、Authorization 头）
        configuration.setAllowCredentials(true);
        // 预检请求缓存时间（秒）
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 对所有路径应用此配置
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
