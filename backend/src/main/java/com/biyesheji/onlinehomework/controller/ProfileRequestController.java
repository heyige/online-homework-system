package com.biyesheji.onlinehomework.controller;

import com.biyesheji.onlinehomework.dto.ApiResponse;
import com.biyesheji.onlinehomework.exception.ResourceNotFoundException;
import com.biyesheji.onlinehomework.model.ProfileRequest;
import com.biyesheji.onlinehomework.service.ProfileRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 个人资料修改申请控制器
 *
 * 描述：处理个人资料修改申请相关的所有接口，包括创建申请、查询申请、审批申请等
 * 用户可以提交个人信息修改申请，管理员可以审批或拒绝申请
 *
 * @Tag: Swagger API 文档注解
 * @RestController: 标记为 REST 控制器
 * @RequestMapping: 类级基础路径，所有接口以 /profile-requests 开头
 * @SecurityRequirement: 需要 JWT 认证
 */
@Tag(name = "个人资料申请", description = "个人信息修改申请相关接口")
@RestController
@RequestMapping("/profile-requests")
@SecurityRequirement(name = "bearerAuth")
public class ProfileRequestController {

    /**
     * 个人资料申请服务
     * 处理个人资料修改申请的业务逻辑
     * @Autowired: 自动注入 Spring 容器中的 ProfileRequestService Bean
     */
    @Autowired
    private ProfileRequestService profileRequestService;

    /**
     * 创建个人资料修改申请接口
     * 用户提交个人信息修改申请，如修改姓名、邮箱、手机号等
     *
     * 功能描述：
     * 1. 从安全上下文中获取当前登录用户的 userId 和 username
     * 2. 将用户信息设置到申请对象中
     * 3. 设置申请状态为"待审批"(pending)
     * 4. 调用服务保存申请记录
     *
     * @param request 包含修改内容的申请信息（姓名、邮箱、手机号等）
     * @return 创建成功的申请记录
     */
    @Operation(summary = "创建个人资料修改申请", description = "用户提交个人信息修改申请")
    @PostMapping
    public ResponseEntity<ApiResponse<ProfileRequest>> createProfileRequest(@RequestBody ProfileRequest request) {
        // 获取当前登录用户信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> principal = (Map<String, Object>) authentication.getPrincipal();
        Long userId = (Long) principal.get("userId");
        String username = (String) principal.get("username");

        // 设置申请的用户信息
        request.setUserId(userId);
        request.setUsername(username);
        // 设置初始状态为待审批
        request.setStatus("pending");

        // 创建申请并返回结果
        ProfileRequest createdRequest = profileRequestService.createProfileRequest(request);
        return ResponseEntity.ok(ApiResponse.success("申请提交成功", createdRequest));
    }

    /**
     * 获取当前用户的申请记录接口
     * 用户查看自己提交的所有个人信息修改申请
     *
     * @return 当前用户的所有申请记录列表
     */
    @Operation(summary = "获取用户的申请记录", description = "获取当前用户的所有申请记录")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ProfileRequest>>> getMyProfileRequests() {
        // 获取当前登录用户信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> principal = (Map<String, Object>) authentication.getPrincipal();
        Long userId = (Long) principal.get("userId");

        // 根据用户ID查询申请记录
        List<ProfileRequest> requests = profileRequestService.getProfileRequestsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    /**
     * 获取所有申请记录接口
     * 仅管理员可调用，查看系统中所有用户的申请
     *
     * @return 所有申请记录列表
     */
    @Operation(summary = "获取所有申请", description = "获取所有用户的申请记录（管理员）")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ProfileRequest>>> getAllProfileRequests() {
        List<ProfileRequest> requests = profileRequestService.getAllProfileRequests();
        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    /**
     * 获取待审批申请接口
     * 仅管理员可调用，查看所有待审批的申请
     *
     * @return 所有状态为"待审批"的申请列表
     */
    @Operation(summary = "获取待审批申请", description = "获取所有待审批的申请")
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<ProfileRequest>>> getPendingProfileRequests() {
        List<ProfileRequest> requests = profileRequestService.getPendingProfileRequests();
        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    /**
     * 获取申请详情接口
     * 根据申请ID查看申请的详细信息
     *
     * @param id 申请ID
     * @return 申请详细信息
     * @throws ResourceNotFoundException 如果申请不存在
     */
    @Operation(summary = "获取申请详情", description = "根据 ID 获取申请详情")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProfileRequest>> getProfileRequestById(@PathVariable Long id) {
        ProfileRequest request = profileRequestService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("申请", "id", id));
        return ResponseEntity.ok(ApiResponse.success(request));
    }

    /**
     * 批准申请接口
     * 仅管理员可调用，批准用户的个人信息修改申请
     *
     * 功能描述：
     * 1. 根据申请ID查找申请记录
     * 2. 调用服务执行批准操作（更新用户表中的个人信息）
     * 3. 将申请状态更新为"已批准"
     *
     * @param id 要批准的申请ID
     * @return 批准后的申请记录
     * @throws ResourceNotFoundException 如果申请不存在
     */
    @Operation(summary = "批准申请", description = "批准个人资料修改申请")
    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProfileRequest>> approveProfileRequest(@PathVariable Long id) {
        ProfileRequest request = profileRequestService.approveProfileRequest(id);
        if (request == null) {
            throw new ResourceNotFoundException("申请", "id", id);
        }
        return ResponseEntity.ok(ApiResponse.success("批准成功", request));
    }

    /**
     * 拒绝申请接口
     * 仅管理员可调用，拒绝用户的个人信息修改申请
     *
     * 功能描述：
     * 1. 根据申请ID查找申请记录
     * 2. 调用服务执行拒绝操作
     * 3. 将申请状态更新为"已拒绝"，并记录拒绝原因
     *
     * @param id 要拒绝的申请ID
     * @param reason 拒绝原因（管理员填写）
     * @return 拒绝后的申请记录
     * @throws ResourceNotFoundException 如果申请不存在
     */
    @Operation(summary = "拒绝申请", description = "拒绝个人资料修改申请")
    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ProfileRequest>> rejectProfileRequest(@PathVariable Long id,
                                                                             @RequestParam String reason) {
        ProfileRequest request = profileRequestService.rejectProfileRequest(id, reason);
        if (request == null) {
            throw new ResourceNotFoundException("申请", "id", id);
        }
        return ResponseEntity.ok(ApiResponse.success("已拒绝申请", request));
    }
}