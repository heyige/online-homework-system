package com.biyesheji.onlinehomework.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * 用户登录请求 DTO
 *
 * 描述：接收前端登录表单提交的数据
 * 用于用户身份认证
 */
@Data
public class LoginRequest {

    /**
     * 用户名
     * @NotBlank: 不能为空字符串
     * message: 校验失败时返回的错误消息
     */
    @NotBlank(message = "用户名不能为空")
    private String username;

    /**
     * 密码
     * @NotBlank: 不能为空字符串
     */
    @NotBlank(message = "密码不能为空")
    private String password;
}
