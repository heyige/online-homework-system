package com.biyesheji.onlinehomework.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 修改密码请求 DTO
 *
 * 描述：用户修改密码时提交的数据
 */
@Data
public class ChangePasswordRequest {

    /**
     * 旧密码（当前密码）
     * @NotBlank: 不能为空
     */
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    /**
     * 新密码
     * @NotBlank: 不能为空
     * @Size: 最大长度限制
     */
    @NotBlank(message = "新密码不能为空")
    @Size(max = 100, message = "密码长度不能超过 100")
    private String newPassword;
}
