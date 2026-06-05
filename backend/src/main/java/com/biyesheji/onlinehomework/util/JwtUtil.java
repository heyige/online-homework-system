package com.biyesheji.onlinehomework.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT（JSON Web Token）工具类
 *
 * 描述：提供JWT令牌的生成、解析、验证等功能
 * 用于用户身份认证和授权
 *
 * @Component: 标记为 Spring 组件，可被自动注入
 */
@Component
public class JwtUtil {

    /**
     * JWT签名密钥
     * 从配置文件 application.yml 读取
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * 访问令牌过期时间（毫秒）
     * 默认为 86400000 毫秒 = 24 小时
     */
    @Value("${jwt.expiration}")
    private Long expiration;

    /**
     * 刷新令牌过期时间（毫秒）
     * 默认为 604800000 毫秒 = 7 天
     */
    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    /**
     * 根据密钥字符串生成签名密钥
     * 使用 HMAC-SHA 算法
     *
     * @return 签名密钥对象
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 JWT 访问令牌
     *
     * @param username 用户名
     * @param userId 用户ID
     * @param role 用户角色
     * @return 生成的 JWT 访问令牌
     */
    public String generateToken(String username, Long userId, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);
        return createToken(claims, username, expiration);
    }

    /**
     * 生成 JWT 刷新令牌
     *
     * @param username 用户名
     * @return 生成的 JWT 刷新令牌
     */
    public String generateRefreshToken(String username) {
        return createToken(new HashMap<>(), username, refreshExpiration);
    }

    /**
     * 创建 JWT 令牌的内部方法
     *
     * @param claims 自定义声明（包含用户信息）
     * @param subject 主题（用户名）
     * @param expirationTime 过期时间（毫秒）
     * @return 生成的 JWT 令牌
     */
    private String createToken(Map<String, Object> claims, String subject, Long expirationTime) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 从 JWT 令牌中提取用户名
     *
     * @param token JWT 令牌
     * @return 用户名
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 从 JWT 令牌中提取用户ID
     *
     * @param token JWT 令牌
     * @return 用户ID
     */
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    /**
     * 从 JWT 令牌中提取用户角色
     *
     * @param token JWT 令牌
     * @return 用户角色
     */
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }

    /**
     * 从 JWT 令牌中提取指定的声明
     *
     * @param token JWT 令牌
     * @param claimsResolver 声明解析函数
     * @param <T> 返回值类型
     * @return 提取的声明值
     */
    public <T> T extractClaim(String token, java.util.function.Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 从 JWT 令牌中提取所有声明
     *
     * @param token JWT 令牌
     * @return 声明对象
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 检查 JWT 令牌是否已过期
     *
     * @param token JWT 令牌
     * @return true 表示已过期，false 表示未过期
     */
    public boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 从 JWT 令牌中提取过期时间
     *
     * @param token JWT 令牌
     * @return 过期时间
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * 验证 JWT 令牌是否有效（验证用户名和过期时间）
     *
     * @param token JWT 令牌
     * @param username 用户名
     * @return true 表示有效，false 表示无效
     */
    public boolean validateToken(String token, String username) {
        try {
            final String extractedUsername = extractUsername(token);
            return (extractedUsername.equals(username) && !isTokenExpired(token));
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 验证 JWT 令牌是否有效（仅验证签名和格式）
     *
     * @param token JWT 令牌
     * @return true 表示有效，false 表示无效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
