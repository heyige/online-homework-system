package com.biyesheji.onlinehomework.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 用户登录响应 DTO
 *
 * 描述：登录成功后返回给前端的数据
 * 包含 JWT Token 和用户基本信息
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {

    /**
     * JWT 访问令牌
     * 用于后续请求的身份认证
     * 有效期 24 小时
     */
    private String token;

    /**
     * JWT 刷新令牌
     * 用于获取新的访问令牌
     * 有效期 7 天
     */
    private String refreshToken;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名（登录账号）
     */
    private String username;

    /**
     * 用户角色
     * ADMIN（管理员）/ TEACHER（教师）/ STUDENT（学生）
     */
    private String role;

    /**
     * 用户真实姓名
     */
    private String name;

    /**
     * 用户头像 URL
     */
    private String avatar;
}
