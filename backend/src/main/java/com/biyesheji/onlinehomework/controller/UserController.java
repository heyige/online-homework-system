package com.biyesheji.onlinehomework.controller;

import com.biyesheji.onlinehomework.dto.ApiResponse;
import com.biyesheji.onlinehomework.dto.ChangePasswordRequest;
import com.biyesheji.onlinehomework.dto.PageDTO;
import com.biyesheji.onlinehomework.exception.BusinessException;
import com.biyesheji.onlinehomework.exception.ResourceNotFoundException;
import com.biyesheji.onlinehomework.model.User;
import com.biyesheji.onlinehomework.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户管理控制器
 *
 * 描述：处理用户信息管理相关的所有接口，包括用户CRUD、角色管理、密码修改等
 * 大部分接口需要管理员或教师权限
 *
 * @Tag: Swagger API 文档注解
 * @RestController: 标记为 REST 控制器
 * @RequestMapping: 类级基础路径，所有接口以 /users 开头
 * @SecurityRequirement: 需要 JWT 认证
 */
@Tag(name = "用户管理", description = "用户信息管理相关接口")
@RestController
@RequestMapping("/users")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    /**
     * 用户服务
     * 处理用户相关的业务逻辑
     * @Autowired: 自动注入 Spring 容器中的 UserService Bean
     */
    @Autowired
    private UserService userService;

    /**
     * 创建用户接口
     * 仅管理员可调用
     *
     * @param user 用户信息
     * @return 创建成功的用户信息（密码置空）
     */
    @Operation(summary = "创建用户", description = "管理员创建新用户")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<User>> createUser(@Valid @RequestBody User user) {
        User createdUser = userService.createUser(user);
        createdUser.setPassword(null);
        return ResponseEntity.ok(ApiResponse.success("用户创建成功", createdUser));
    }

    /**
     * 获取当前登录用户信息
     *
     * @return 当前用户的详细信息
     */
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<User>> getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> principal = (Map<String, Object>) authentication.getPrincipal();
        String username = (String) principal.get("username");

        User user = userService.findByUsername(username);
        user.setPassword(null);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    /**
     * 根据ID获取用户信息
     * 管理员和教师可调用
     *
     * @param id 用户ID
     * @return 用户信息
     */
    @Operation(summary = "根据 ID 获取用户", description = "根据用户 ID 获取用户信息")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable Long id) {
        User user = userService.findById(id);
        user.setPassword(null);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    /**
     * 获取所有用户列表（分页）
     * 仅管理员可调用
     *
     * @param page 页码（默认1）
     * @param limit 每页数量（默认10）
     * @param username 用户名（可选，模糊查询）
     * @param name 姓名（可选，模糊查询）
     * @param role 角色（可选，精确查询）
     * @return 分页后的用户列表
     */
    @Operation(summary = "获取所有用户", description = "获取所有用户列表（仅管理员）")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageDTO<User>>> getAllUsers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String role) {
        PageDTO<User> pageDTO = userService.getUsersByPage(page, limit, username, name, role);
        // 返回前将所有用户的密码置空
        pageDTO.getContent().forEach(user -> user.setPassword(null));
        return ResponseEntity.ok(ApiResponse.success(pageDTO));
    }

    /**
     * 根据角色获取用户列表
     * 管理员和教师可调用
     *
     * @param role 用户角色
     * @return 符合角色的用户列表
     */
    @Operation(summary = "按角色获取用户", description = "根据角色获取用户列表")
    @GetMapping("/role/{role}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<List<User>>> getUsersByRole(@PathVariable String role) {
        List<User> users = userService.findByRole(role);
        users.forEach(user -> user.setPassword(null));
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    /**
     * 更新用户信息
     * 管理员和教师可调用
     *
     * @param id 要更新的用户ID
     * @param user 包含更新信息的用户对象
     * @return 更新后的用户信息
     */
    @Operation(summary = "更新用户信息", description = "更新用户基本信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<ApiResponse<User>> updateUser(@PathVariable Long id, @RequestBody User user) {
        User existingUser = userService.findById(id);

        // 只更新非空字段
        if (user.getName() != null) {
            existingUser.setName(user.getName());
        }
        if (user.getRole() != null) {
            existingUser.setRole(user.getRole());
        }
        if (user.getDepartment() != null) {
            existingUser.setDepartment(user.getDepartment());
        }
        if (user.getMajor() != null) {
            existingUser.setMajor(user.getMajor());
        }
        if (user.getEmail() != null) {
            existingUser.setEmail(user.getEmail());
        }
        if (user.getPhone() != null) {
            existingUser.setPhone(user.getPhone());
        }

        User updatedUser = userService.updateUser(existingUser);
        updatedUser.setPassword(null);

        return ResponseEntity.ok(ApiResponse.success("更新成功", updatedUser));
    }

    /**
     * 删除用户
     * 仅管理员可调用
     *
     * @param id 要删除的用户ID
     * @return 删除成功响应
     */
    @Operation(summary = "删除用户", description = "删除指定用户（仅管理员）")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }

    /**
     * 修改当前用户密码
     *
     * @param request 包含旧密码和新密码的请求
     * @return 修改成功或失败响应
     */
    @Operation(summary = "修改密码", description = "修改当前用户的密码")
    @PutMapping("/change-password")
    public ResponseEntity<ApiResponse<String>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> principal = (Map<String, Object>) authentication.getPrincipal();
        String username = (String) principal.get("username");
        User user = userService.findByUsername(username);

        String result = userService.changePassword(user.getId(), request.getOldPassword(), request.getNewPassword());

        if ("success".equals(result)) {
            return ResponseEntity.ok(ApiResponse.success("密码修改成功", null));
        } else {
            throw new BusinessException(result);
        }
    }

    /**
     * 生成验证码
     * 用于忘记密码功能
     *
     * @param identifier 邮箱或手机号
     * @return 生成的验证码
     */
    @Operation(summary = "生成验证码", description = "生成邮箱或手机验证码")
    @PostMapping("/verification-code")
    public ResponseEntity<ApiResponse<String>> generateVerificationCode(@RequestParam String identifier) {
        String code = userService.generateVerificationCode(identifier);
        Map<String, String> response = new HashMap<>();
        response.put("code", code);
        return ResponseEntity.ok(ApiResponse.success("验证码已发送", code));
    }

    /**
     * 验证验证码
     *
     * @param identifier 邮箱或手机号
     * @param code 用户输入的验证码
     * @return 验证结果
     */
    @Operation(summary = "验证验证码", description = "验证邮箱或手机验证码")
    @PostMapping("/verify-code")
    public ResponseEntity<ApiResponse<Boolean>> verifyCode(@RequestParam String identifier, @RequestParam String code) {
        boolean result = userService.verifyCode(identifier, code);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 重置密码
     * 通过验证码验证后重置
     *
     * @param identifier 邮箱或手机号
     * @param code 验证码
     * @param newPassword 新密码
     * @return 重置结果
     */
    @Operation(summary = "重置密码", description = "通过验证码重置密码")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<String>> resetPassword(@RequestParam String identifier,
                                                              @RequestParam String code,
                                                              @RequestParam String newPassword) {
        if (!userService.verifyCode(identifier, code)) {
            throw new BusinessException("验证码错误");
        }

        boolean result = userService.resetPassword(identifier, newPassword);

        if (result) {
            return ResponseEntity.ok(ApiResponse.success("密码重置成功", null));
        } else {
            throw new BusinessException("密码重置失败");
        }
    }
}
