package com.biyesheji.onlinehomework.controller;

import com.biyesheji.onlinehomework.dto.ApiResponse;
import com.biyesheji.onlinehomework.exception.ResourceNotFoundException;
import com.biyesheji.onlinehomework.model.Message;
import com.biyesheji.onlinehomework.model.User;
import com.biyesheji.onlinehomework.repository.UserRepository;
import com.biyesheji.onlinehomework.service.HomeworkService;
import com.biyesheji.onlinehomework.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Tag(name = "消息管理", description = "系统消息、通知相关接口")
@RestController
@RequestMapping("/messages")
@SecurityRequirement(name = "bearerAuth")
public class MessageController {
    
    @Autowired
    private MessageService messageService;
    
    @Autowired
    private HomeworkService homeworkService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Operation(summary = "获取当前用户的消息", description = "获取当前登录用户的所有消息")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Message>>> getCurrentUserMessages() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> principal = (Map<String, Object>) authentication.getPrincipal();
        Long userId = (Long) principal.get("userId");
        String role = (String) principal.get("role");

        List<Message> messages;
        if ("ADMIN".equalsIgnoreCase(role)) {
            messages = messageService.findAllMessagesForAdmin(userId);
        } else {
            messages = messageService.findByReceiverId(userId);
        }
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @Operation(summary = "按用户获取消息", description = "管理员查看指定用户收到的最近消息")
    @GetMapping("/receiver/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Message>>> getMessagesByReceiver(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        if (userRepository.findById(userId).isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("用户不存在"));
        }
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        List<Message> messages = messageService.findRecentMessagesByReceiverId(userId, safeLimit);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }
    
    @Operation(summary = "获取未读消息", description = "获取当前用户的未读消息")
    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<List<Message>>> getUnreadMessages() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> principal = (Map<String, Object>) authentication.getPrincipal();
        Long userId = (Long) principal.get("userId");
        
        List<Message> messages = messageService.findUnreadMessages(userId);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }
    
    @Operation(summary = "获取消息详情", description = "根据 ID 获取消息详情")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Message>> getMessageById(@PathVariable Long id) {
        Message message = messageService.findById(id);
        if (message == null) {
            throw new ResourceNotFoundException("消息", "id", id);
        }
        return ResponseEntity.ok(ApiResponse.success(message));
    }
    
    @Operation(summary = "标记消息为已读", description = "将指定消息标记为已读")
    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Message>> markMessageAsRead(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> principal = (Map<String, Object>) authentication.getPrincipal();
        Long userId = (Long) principal.get("userId");
        
        Message message = messageService.markAsRead(id, userId);
        if (message == null) {
            throw new ResourceNotFoundException("消息", "id", id);
        }
        return ResponseEntity.ok(ApiResponse.success("已标记为已读", message));
    }
    
    @Operation(summary = "更新消息", description = "更新指定消息（管理员）")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Message>> updateMessage(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Message message = new Message();
        if (request.containsKey("title")) {
            message.setTitle((String) request.get("title"));
        }
        if (request.containsKey("content")) {
            message.setContent((String) request.get("content"));
        }
        if (request.containsKey("status")) {
            message.setStatus((String) request.get("status"));
        }
        if (request.containsKey("publishTime")) {
            Object publishTimeObj = request.get("publishTime");
            Date publishTime = null;
            if (publishTimeObj instanceof Long) {
                publishTime = new Date((Long) publishTimeObj);
            } else if (publishTimeObj instanceof String) {
                String publishTimeStr = (String) publishTimeObj;
                try {
                    publishTime = new Date(Long.parseLong(publishTimeStr));
                } catch (NumberFormatException e) {
                    try {
                        java.time.LocalDateTime localDateTime = java.time.LocalDateTime.parse(publishTimeStr, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        publishTime = java.sql.Timestamp.valueOf(localDateTime);
                    } catch (java.time.format.DateTimeParseException e2) {
                        return ResponseEntity.ok(ApiResponse.error("无效的发布时间格式"));
                    }
                }
            }
            message.setPublishTime(publishTime);
        }
        if (request.containsKey("expireTime")) {
            Object expireTimeObj = request.get("expireTime");
            Date expireTime = null;
            if (expireTimeObj instanceof Long) {
                expireTime = new Date((Long) expireTimeObj);
            } else if (expireTimeObj instanceof String) {
                String expireTimeStr = (String) expireTimeObj;
                try {
                    expireTime = new Date(Long.parseLong(expireTimeStr));
                } catch (NumberFormatException e) {
                    try {
                        java.time.LocalDateTime localDateTime = java.time.LocalDateTime.parse(expireTimeStr, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        expireTime = java.sql.Timestamp.valueOf(localDateTime);
                    } catch (java.time.format.DateTimeParseException e2) {
                        return ResponseEntity.ok(ApiResponse.error("无效的过期时间格式"));
                    }
                }
            }
            message.setExpireTime(expireTime);
        }

        Message existingMessage = messageService.findById(id);
        if (existingMessage == null) {
            throw new ResourceNotFoundException("消息", "id", id);
        }

        if (existingMessage.getNotificationId() != null) {
            List<Message> updatedMessages = messageService.updateMessageByNotificationId(existingMessage.getNotificationId(), message);
            return ResponseEntity.ok(ApiResponse.success("更新成功（共更新 " + updatedMessages.size() + " 条消息）", updatedMessages.isEmpty() ? null : updatedMessages.get(0)));
        } else {
            Message updatedMessage = messageService.updateMessage(id, message);
            return ResponseEntity.ok(ApiResponse.success("更新成功", updatedMessage));
        }
    }
    
    @Operation(summary = "删除消息", description = "删除指定消息")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteMessage(@PathVariable Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> principal = (Map<String, Object>) authentication.getPrincipal();
        Long userId = (Long) principal.get("userId");
        String role = (String) principal.get("role");
        boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
        
        messageService.deleteMessage(id, userId, isAdmin);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }
    
    @Operation(summary = "创建消息", description = "创建系统消息（管理员）")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Message>>> createMessage(@RequestBody Map<String, Object> request) {
        String title = (String) request.get("title");
        String content = (String) request.get("content");
        String type = (String) request.get("type");
        String status = (String) request.get("status");
        String receiverRole = (String) request.get("receiverRole");
        Object publishTimeObj = request.get("publishTime");
        Object expireTimeObj = request.get("expireTime");

        Date publishTime = null;
        if (publishTimeObj != null) {
            if (publishTimeObj instanceof Long) {
                publishTime = new Date((Long) publishTimeObj);
            } else if (publishTimeObj instanceof String) {
                String publishTimeStr = (String) publishTimeObj;
                try {
                    publishTime = new Date(Long.parseLong(publishTimeStr));
                } catch (NumberFormatException e) {
                    try {
                        java.time.LocalDateTime localDateTime = java.time.LocalDateTime.parse(publishTimeStr, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        publishTime = java.sql.Timestamp.valueOf(localDateTime);
                    } catch (java.time.format.DateTimeParseException e2) {
                        try {
                            java.time.LocalDate localDate = java.time.LocalDate.parse(publishTimeStr);
                            publishTime = java.sql.Date.valueOf(localDate);
                        } catch (java.time.format.DateTimeParseException e3) {
                            return ResponseEntity.ok(ApiResponse.error("无效的发布时间格式"));
                        }
                    }
                }
            }
        }

        Date expireTime = null;
        if (expireTimeObj != null) {
            if (expireTimeObj instanceof Long) {
                expireTime = new Date((Long) expireTimeObj);
            } else if (expireTimeObj instanceof String) {
                String expireTimeStr = (String) expireTimeObj;
                try {
                    expireTime = new Date(Long.parseLong(expireTimeStr));
                } catch (NumberFormatException e) {
                    try {
                        java.time.LocalDateTime localDateTime = java.time.LocalDateTime.parse(expireTimeStr, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                        expireTime = java.sql.Timestamp.valueOf(localDateTime);
                    } catch (java.time.format.DateTimeParseException e2) {
                        try {
                            java.time.LocalDate localDate = java.time.LocalDate.parse(expireTimeStr);
                            expireTime = java.sql.Date.valueOf(localDate);
                        } catch (java.time.format.DateTimeParseException e3) {
                            return ResponseEntity.ok(ApiResponse.error("无效的过期时间格式"));
                        }
                    }
                }
            }
        }

        List<User> targetUsers;

        if ("ALL".equals(receiverRole)) {
            targetUsers = userRepository.findAll();
        } else {
            targetUsers = userRepository.findByRoleIgnoreCase(receiverRole);
            
            // 总是发送给所有管理员，便于管理
            List<User> admins = userRepository.findByRoleIgnoreCase("ADMIN");
            // 添加管理员到目标用户列表，但避免重复（如果选择的角色已经包含管理员）
            for (User admin : admins) {
                boolean exists = targetUsers.stream().anyMatch(u -> u.getId().equals(admin.getId()));
                if (!exists) {
                    targetUsers.add(admin);
                }
            }
        }

        if (targetUsers.isEmpty()) {
            return ResponseEntity.ok(ApiResponse.error("没有找到符合条件的接收者"));
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> principal = (Map<String, Object>) authentication.getPrincipal();
        Long senderId = (Long) principal.get("userId");

        if (messageService.existsByTitleAndSenderId(title, senderId)) {
            return ResponseEntity.ok(ApiResponse.error("已存在相同标题的公告"));
        }

        String notificationId = "announcement_" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString();

        List<Message> createdMessages = new ArrayList<>();
        for (User user : targetUsers) {
            Message message = new Message();
            message.setTitle(title);
            message.setContent(content);
            message.setType(type);
            message.setStatus(status);
            message.setReceiverId(user.getId());
            message.setSenderId(senderId);
            message.setPublishTime(publishTime);
            message.setExpireTime(expireTime);
            message.setNotificationId(notificationId);

            Message createdMessage = messageService.createMessage(message);
            createdMessages.add(createdMessage);
        }

        return ResponseEntity.ok(ApiResponse.success("消息创建成功，已发送给 " + createdMessages.size() + " 个用户", createdMessages));
    }
    
    @Operation(summary = "批量删除消息", description = "根据 notificationId 批量删除消息")
    @DeleteMapping("/notification/{notificationId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteMessagesByNotificationId(@PathVariable String notificationId) {
        messageService.deleteAllByNotificationId(notificationId);
        return ResponseEntity.ok(ApiResponse.success("批量删除成功", null));
    }
    
    @Operation(summary = "教师发送消息给学生", description = "教师向自己的学生发送消息")
    @PostMapping("/to-students")
    @PreAuthorize("hasRole('TEACHER')")
    public ResponseEntity<ApiResponse<Message>> sendMessageToStudents(@RequestBody Message message) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> principal = (Map<String, Object>) authentication.getPrincipal();
        String username = (String) principal.get("username");
        Long teacherId = homeworkService.findByUsername(username).getId();
        
        // 这里需要实现发送消息给学生的逻辑
        // 暂时先返回成功
        return ResponseEntity.ok(ApiResponse.success("消息发送成功", message));
    }
}
