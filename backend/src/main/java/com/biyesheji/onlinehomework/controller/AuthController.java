package com.biyesheji.onlinehomework.controller;

import com.biyesheji.onlinehomework.dto.*;
import com.biyesheji.onlinehomework.model.User;
import com.biyesheji.onlinehomework.service.UserService;
import com.biyesheji.onlinehomework.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证管理控制器
 *
 * 描述：处理用户认证相关的所有接口，包括登录、注册、退出、忘记密码等功能
 *
 * @Tag: Swagger API 文档注解，用于分组
 * @RestController: 标记为 REST 控制器
 * @RequestMapping: 类级基础路径，所有接口以 /auth 开头
 */
@Tag(name = "认证管理", description = "用户登录、注册、认证相关接口")
@RestController
@RequestMapping("/auth")
public class AuthController {

    /**
     * 认证管理器
     * 用于执行用户认证操作
     * @Autowired: 自动注入 Spring 容器中的 AuthenticationManager Bean
     */
    @Autowired
    private AuthenticationManager authenticationManager;

    /**
     * JWT 工具类
     * 用于生成和验证 JWT 令牌
     */
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 用户服务
     * 处理用户相关的业务逻辑
     */
    @Autowired
    private UserService userService;

    /**
     * 用户登录接口
     *
     * 功能描述：
     * 1. 接收用户名和密码
     * 2. 调用 AuthenticationManager 进行认证
     * 3. 生成 JWT 访问令牌和刷新令牌
     * 4. 返回用户信息和令牌
     *
     * @param loginRequest 登录请求（包含用户名和密码）
     * @return 包含 JWT 令牌和用户信息的响应
     */
    @Operation(summary = "用户登录", description = "使用用户名和密码进行登录")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest loginRequest) {
        // 调用认证管理器进行用户认证
        // 如果认证失败会抛出异常
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginRequest.getUsername(),
                loginRequest.getPassword()
            )
        );

        // 将认证信息存入安全上下文
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // 获取用户完整信息
        User user = userService.findByUsername(loginRequest.getUsername());

        // 将角色转换为大写，以符合 Spring Security 的角色命名规范
        String role = user.getRole() != null ? user.getRole().toUpperCase() : "";
        // 生成 JWT 访问令牌（24小时有效期）
        String jwt = jwtUtil.generateToken(user.getUsername(), user.getId(), role);
        // 生成刷新令牌（7天有效期）
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

        // 构建登录响应
        LoginResponse response = new LoginResponse(
            jwt,
            refreshToken,
            user.getId(),
            user.getUsername(),
            user.getRole(),
            user.getName(),
            user.getAvatar()
        );

        return ResponseEntity.ok(ApiResponse.success("登录成功", response));
    }

    /**
     * 用户注册接口
     *
     * 功能描述：
     * 1. 接收注册信息（用户名、密码、姓名、角色等）
     * 2. 调用 UserService 创建新用户
     * 3. 返回创建成功的用户信息（密码置空）
     *
     * @param registerRequest 注册请求（包含用户信息）
     * @return 创建成功的用户信息
     */
    @Operation(summary = "用户注册", description = "注册新用户")
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<User>> register(@Valid @RequestBody RegisterRequest registerRequest) {
        // 构建用户对象
        User user = new User();
        user.setUsername(registerRequest.getUsername());
        user.setPassword(registerRequest.getPassword());
        user.setName(registerRequest.getName());
        user.setRole(registerRequest.getRole());
        user.setEmail(registerRequest.getEmail());
        user.setPhone(registerRequest.getPhone());
        user.setDepartment(registerRequest.getDepartment());
        user.setMajor(registerRequest.getMajor());

        // 创建用户
        User createdUser = userService.createUser(user);
        // 返回前将密码置空，不暴露敏感信息
        createdUser.setPassword(null);

        return ResponseEntity.ok(ApiResponse.success("注册成功", createdUser));
    }

    /**
     * 刷新令牌接口
     *
     * 功能描述：
     * 1. 接收刷新令牌
     * 2. 验证刷新令牌是否有效
     * 3. 生成新的访问令牌
     * 4. 返回新的访问令牌
     *
     * @param refreshTokenRequest 包含刷新令牌的请求
     * @return 新的访问令牌
     */
    @Operation(summary = "刷新令牌", description = "使用刷新令牌获取新的访问令牌")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, String>>> refreshToken(@RequestBody Map<String, String> refreshTokenRequest) {
        String refreshToken = refreshTokenRequest.get("refreshToken");

        // 验证刷新令牌是否有效
        if (!jwtUtil.validateToken(refreshToken)) {
            return ResponseEntity.badRequest().body(ApiResponse.error(401, "刷新令牌无效或已过期"));
        }

        // 从刷新令牌中提取用户名
        String username = jwtUtil.extractUsername(refreshToken);
        User user = userService.findByUsername(username);

        // 将角色转换为大写
        String role = user.getRole() != null ? user.getRole().toUpperCase() : "";
        // 生成新的访问令牌
        String newToken = jwtUtil.generateToken(username, user.getId(), role);

        // 构建响应
        Map<String, String> response = new HashMap<>();
        response.put("token", newToken);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 退出登录接口
     *
     * 功能描述：清除安全上下文中的认证信息
     *
     * @return 退出成功响应
     */
    @Operation(summary = "退出登录", description = "退出当前登录")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout() {
        // 清除安全上下文
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(ApiResponse.success("退出成功", null));
    }

    /**
     * 发送验证码接口
     *
     * 功能描述：
     * 1. 接收邮箱或手机号
     * 2. 验证用户是否存在
     * 3. 生成6位数字验证码
     * 4. 返回发送成功响应
     *
     * @param request 包含邮箱或手机号的请求
     * @return 发送成功响应
     */
    @Operation(summary = "发送验证码", description = "发送验证码到邮箱或手机用于忘记密码")
    @PostMapping("/forgot-password/send-code")
    public ResponseEntity<ApiResponse<String>> sendVerificationCode(@RequestBody Map<String, String> request) {
        String identifier = request.get("identifier");
        if (identifier == null || identifier.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "请输入邮箱或手机号"));
        }

        // 检查用户是否存在
        boolean exists;
        if (identifier.contains("@")) {
            exists = userService.findByEmail(identifier).isPresent();
        } else {
            exists = userService.findByPhone(identifier).isPresent();
        }

        if (!exists) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "该邮箱或手机号未注册"));
        }

        // 生成并发送验证码
        userService.generateVerificationCode(identifier);

        return ResponseEntity.ok(ApiResponse.success("验证码已发送", null));
    }

    /**
     * 验证验证码接口
     *
     * 功能描述：
     * 1. 接收邮箱/手机号和验证码
     * 2. 验证验证码是否正确
     * 3. 返回验证结果
     *
     * @param request 包含邮箱/手机号和验证码的请求
     * @return 验证成功或失败响应
     */
    @Operation(summary = "验证验证码", description = "验证忘记密码时输入的验证码")
    @PostMapping("/forgot-password/verify-code")
    public ResponseEntity<ApiResponse<String>> verifyCode(@RequestBody Map<String, String> request) {
        String identifier = request.get("identifier");
        String code = request.get("code");

        if (identifier == null || code == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "请输入邮箱或手机号和验证码"));
        }

        // 验证验证码
        boolean isValid = userService.verifyCode(identifier, code);
        if (!isValid) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "验证码错误"));
        }

        return ResponseEntity.ok(ApiResponse.success("验证码验证成功", null));
    }

    /**
     * 重置密码接口
     *
     * 功能描述：
     * 1. 接收邮箱/手机号和新密码
     * 2. 重置用户密码
     * 3. 返回重置结果
     *
     * @param request 包含邮箱/手机号和新密码的请求
     * @return 重置成功或失败响应
     */
    @Operation(summary = "重置密码", description = "通过验证码验证后重置密码")
    @PostMapping("/forgot-password/reset")
    public ResponseEntity<ApiResponse<String>> resetPassword(@RequestBody Map<String, String> request) {
        String identifier = request.get("identifier");
        String newPassword = request.get("newPassword");

        if (identifier == null || newPassword == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "请输入邮箱或手机号和新密码"));
        }

        boolean success = userService.resetPassword(identifier, newPassword);
        if (!success) {
            return ResponseEntity.badRequest().body(ApiResponse.error(400, "重置密码失败"));
        }

        return ResponseEntity.ok(ApiResponse.success("密码重置成功", null));
    }

    /**
     * 检查用户名是否存在接口
     *
     * @param request 包含用户名的请求
     * @return 用户是否存在的结果
     */
    @Operation(summary = "检查用户名是否存在", description = "检查用户名是否已被使用")
    @PostMapping("/check-username")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> checkUsername(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        if (username == null || username.isEmpty()) {
            Map<String, Boolean> response = new HashMap<>();
            response.put("exists", false);
            return ResponseEntity.ok(ApiResponse.success(response));
        }

        boolean exists = userService.findByUsername(username) != null;
        Map<String, Boolean> response = new HashMap<>();
        response.put("exists", exists);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 获取用户联系方式接口
     * 返回脱敏后的邮箱和手机号
     *
     * @param request 包含用户名的请求
     * @return 脱敏后的联系方式
     */
    @Operation(summary = "获取用户联系方式", description = "获取用户的邮箱和手机号（脱敏）")
    @PostMapping("/get-user-contacts")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserContacts(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        if (username == null || username.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("hasPhone", false);
            response.put("hasEmail", false);
            return ResponseEntity.ok(ApiResponse.success(response));
        }

        User user = userService.findByUsername(username);
        Map<String, Object> response = new HashMap<>();
        if (user != null) {
            response.put("hasPhone", user.getPhone() != null && !user.getPhone().isEmpty());
            response.put("hasEmail", user.getEmail() != null && !user.getEmail().isEmpty());

            // 隐藏手机号中间4位：138****1234
            if (user.getPhone() != null && !user.getPhone().isEmpty()) {
                String phone = user.getPhone();
                if (phone.length() >= 8) {
                    response.put("phone", phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4));
                } else {
                    response.put("phone", phone);
                }
            }

            // 隐藏邮箱用户名部分：t***@example.com
            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                String email = user.getEmail();
                int atIndex = email.indexOf('@');
                if (atIndex > 0) {
                    String usernamePart = email.substring(0, atIndex);
                    String domainPart = email.substring(atIndex);
                    if (usernamePart.length() > 1) {
                        response.put("email", usernamePart.substring(0, 1) + "****" + domainPart);
                    } else {
                        response.put("email", "*" + "****" + domainPart);
                    }
                } else {
                    response.put("email", email);
                }
            }
        } else {
            response.put("hasPhone", false);
            response.put("hasEmail", false);
        }

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
