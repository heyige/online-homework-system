package com.biyesheji.onlinehomework.service;

import com.biyesheji.onlinehomework.model.Message;
import com.biyesheji.onlinehomework.model.ProfileRequest;
import com.biyesheji.onlinehomework.repository.ProfileRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 个人信息修改申请业务逻辑服务类
 *
 * 描述：处理用户提交的个人信息修改申请相关的所有业务逻辑，包括提交申请、管理员审核、查询申请、发送通知等
 *
 * @Service: 标记为 Spring 业务逻辑组件
 * @Transactional: 开启事务管理，所有方法均在事务中执行
 */
@Service
@Transactional
public class ProfileRequestService {

    /**
     * 个人信息申请数据访问接口
     * @Autowired: 自动注入 Spring 容器中的 ProfileRequestRepository Bean
     */
    @Autowired
    private ProfileRequestRepository profileRequestRepository;

    /**
     * 用户服务
     */
    @Autowired
    private UserService userService;

    /**
     * 消息服务
     */
    @Autowired
    private MessageService messageService;

    /**
     * 创建个人信息修改申请
     * 同一用户同时只能有一个待处理的申请
     *
     * @param request 申请实体（包含用户ID、新的姓名、邮箱、电话等信息）
     * @return 创建成功的申请实体
     * @throws IllegalArgumentException 如果用户已有待处理的申请
     */
    public ProfileRequest createProfileRequest(ProfileRequest request) {
        // 检查用户是否已有待处理的申请
        List<ProfileRequest> pendingRequests = profileRequestRepository.findByUserIdAndStatus(request.getUserId(), "pending");
        if (!pendingRequests.isEmpty()) {
            throw new IllegalArgumentException("用户已有未处理的修改申请");
        }
        return profileRequestRepository.save(request);
    }

    /**
     * 获取所有个人信息修改申请
     * 按优先级排序：待处理优先，相同状态按创建时间倒序
     *
     * @return 所有申请列表
     */
    public List<ProfileRequest> getAllProfileRequests() {
        List<ProfileRequest> requests = profileRequestRepository.findAll();
        // 自定义排序：待处理申请在前，其他在后；同状态按创建时间倒序
        requests.sort((r1, r2) -> {
            if ("pending".equals(r1.getStatus()) && !"pending".equals(r2.getStatus())) {
                return -1;
            } else if (!"pending".equals(r1.getStatus()) && "pending".equals(r2.getStatus())) {
                return 1;
            } else {
                return r2.getCreatedAt().compareTo(r1.getCreatedAt());
            }
        });
        return requests;
    }

    /**
     * 获取所有待处理的申请
     *
     * @return 待处理的申请列表
     */
    public List<ProfileRequest> getPendingProfileRequests() {
        return profileRequestRepository.findByStatus("pending");
    }

    /**
     * 根据申请ID查询申请
     *
     * @param id 申请ID
     * @return 包含申请的 Optional 对象
     */
    public Optional<ProfileRequest> findById(Long id) {
        return profileRequestRepository.findById(id);
    }

    /**
     * 批准个人信息修改申请
     * 批准后，系统自动更新用户的个人信息
     *
     * @param id 申请ID
     * @return 更新后的申请实体
     */
    public ProfileRequest approveProfileRequest(Long id) {
        Optional<ProfileRequest> requestOptional = profileRequestRepository.findById(id);
        if (requestOptional.isPresent()) {
            ProfileRequest request = requestOptional.get();
            // 更新申请状态为已批准
            request.setStatus("approved");

            // 更新用户的个人信息（同时清除用户缓存）
            userService.applyProfileApproval(request);

            // 发送批准通知给用户
            sendProfileRequestNotification(request, "approved", null);

            return profileRequestRepository.save(request);
        }
        return null;
    }

    /**
     * 拒绝个人信息修改申请
     *
     * @param id 申请ID
     * @param reason 拒绝理由
     * @return 更新后的申请实体
     */
    public ProfileRequest rejectProfileRequest(Long id, String reason) {
        Optional<ProfileRequest> requestOptional = profileRequestRepository.findById(id);
        if (requestOptional.isPresent()) {
            ProfileRequest request = requestOptional.get();
            // 更新申请状态为已拒绝
            request.setStatus("rejected");

            // 发送拒绝通知给用户
            sendProfileRequestNotification(request, "rejected", reason);

            return profileRequestRepository.save(request);
        }
        return null;
    }

    /**
     * 发送个人信息修改申请处理结果通知
     *
     * @param request 申请实体
     * @param status 处理状态（approved/rejected）
     * @param reason 拒绝理由（批准时为 null）
     */
    private void sendProfileRequestNotification(ProfileRequest request, String status, String reason) {
        Message message = new Message();
        message.setTitle("个人信息修改申请处理结果");
        message.setContent(
            "approved".equals(status) ? 
            "您的个人信息修改申请已被批准，您的个人信息已更新。" : 
            "您的个人信息修改申请已被拒绝，理由：" + (reason != null ? reason : "请检查申请内容") + "。"
        );
        // 发送者设置为系统用户（ID为1）
        message.setSenderId(1L);
        message.setReceiverId(request.getUserId());
        message.setType("permanent");
        message.setStatus("active");
        message.setNotificationId("profile_request_" + request.getId() + "_" + status);

        try {
            messageService.createMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 查询用户提交的所有个人信息修改申请
     *
     * @param userId 用户ID
     * @return 该用户的申请列表
     */
    public List<ProfileRequest> getProfileRequestsByUserId(Long userId) {
        return profileRequestRepository.findByUserId(userId);
    }
}
