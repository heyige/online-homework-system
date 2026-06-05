package com.biyesheji.onlinehomework.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * 用户注册请求 DTO
 *
 * 描述：接收前端注册表单提交的数据
 * 用于创建新用户账号
 */
@Data
public class RegisterRequest {

    /**
     * 用户名（登录账号）
     * @NotBlank: 不能为空
     * @Size: 最大长度限制
     */
    @NotBlank(message = "用户名不能为空")
    @Size(max = 50, message = "用户名长度不能超过 50")
    private String username;

    /**
     * 密码
     * @NotBlank: 不能为空
     * @Size: 最大长度限制
     */
    @NotBlank(message = "密码不能为空")
    @Size(max = 100, message = "密码长度不能超过 100")
    private String password;

    /**
     * 真实姓名
     * @NotBlank: 不能为空
     */
    @NotBlank(message = "姓名不能为空")
    private String name;

    /**
     * 用户角色
     * @NotBlank: 不能为空
     * 取值范围: ADMIN / TEACHER / STUDENT
     */
    @NotBlank(message = "角色不能为空")
    private String role;

    /**
     * 电子邮箱
     * @Email: 格式校验，必须是有效的邮箱格式
     */
    @Email(message = "邮箱格式不正确")
    private String email;

    /**
     * 手机号码（可选）
     */
    private String phone;

    /**
     * 院系/部门（可选）
     */
    private String department;

    /**
     * 专业（可选）
     */
    private String major;
}
