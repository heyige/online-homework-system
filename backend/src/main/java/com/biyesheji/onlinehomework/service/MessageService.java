package com.biyesheji.onlinehomework.service;

import com.biyesheji.onlinehomework.model.Message;
import com.biyesheji.onlinehomework.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 消息通知业务逻辑服务类
 *
 * 描述：处理系统通知消息相关的所有业务逻辑，包括发送消息、查询消息、标记已读、删除消息等
 *
 * @Service: 标记为 Spring 业务逻辑组件
 * @Transactional: 开启事务管理，所有方法均在事务中执行
 */
@Service
@Transactional
public class MessageService {

    /**
     * 消息数据访问接口
     * @Autowired: 自动注入 Spring 容器中的 MessageRepository Bean
     */
    @Autowired
    private MessageRepository messageRepository;

    /**
     * 创建并发送消息
     *
     * @param message 消息实体（包含标题、内容、发送者、接收者等信息）
     * @return 创建成功的消息实体
     */
    public Message createMessage(Message message) {
        // 如果未设置已读状态，默认设置为未读
        if (message.getIsRead() == null) {
            message.setIsRead(false);
        }
        return messageRepository.save(message);
    }

    /**
     * 根据消息ID查询消息
     *
     * @param id 消息ID
     * @return 消息实体，如果不存在则返回 null
     */
    public Message findById(Long id) {
        Optional<Message> optionalMessage = messageRepository.findById(id);
        return optionalMessage.orElse(null);
    }

    /**
     * 更新消息内容
     * 用于修改已发送的消息
     *
     * @param id 消息ID
     * @param message 包含更新信息的消息实体
     * @return 更新后的消息实体
     */
    public Message updateMessage(Long id, Message message) {
        Optional<Message> optionalMessage = messageRepository.findById(id);
        if (optionalMessage.isPresent()) {
            Message existingMessage = optionalMessage.get();
            if (message.getTitle() != null) {
                existingMessage.setTitle(message.getTitle());
            }
            if (message.getContent() != null) {
                existingMessage.setContent(message.getContent());
            }
            if (message.getType() != null) {
                existingMessage.setType(message.getType());
            }
            if (message.getStatus() != null) {
                existingMessage.setStatus(message.getStatus());
            }
            if (message.getExpireTime() != null) {
                existingMessage.setExpireTime(message.getExpireTime());
            }
            if (message.getPublishTime() != null) {
                existingMessage.setPublishTime(message.getPublishTime());
            }
            if (message.getIsRead() != null) {
                existingMessage.setIsRead(message.getIsRead());
            }
            return messageRepository.save(existingMessage);
        }
        return null;
    }

    public List<Message> updateMessageByNotificationId(String notificationId, Message message) {
        List<Message> messages = messageRepository.findByNotificationId(notificationId);
        List<Message> updatedMessages = new ArrayList<>();
        for (Message existingMessage : messages) {
            if (message.getTitle() != null) {
                existingMessage.setTitle(message.getTitle());
            }
            if (message.getContent() != null) {
                existingMessage.setContent(message.getContent());
            }
            if (message.getStatus() != null) {
                existingMessage.setStatus(message.getStatus());
            }
            if (message.getExpireTime() != null) {
                existingMessage.setExpireTime(message.getExpireTime());
            }
            if (message.getPublishTime() != null) {
                existingMessage.setPublishTime(message.getPublishTime());
            }
            updatedMessages.add(messageRepository.save(existingMessage));
        }
        return updatedMessages;
    }

    /**
     * 查询用户收到的所有消息
     * 同时过滤掉已过期的消息
     *
     * @param receiverId 接收者用户ID
     * @return 该用户的所有有效消息列表
     */
    public List<Message> findByReceiverId(Long receiverId) {
        List<Message> allMessages = messageRepository.findByReceiverId(receiverId);
        return filterValidMessages(allMessages);
    }

    /**
     * 查询用户收到的所有消息（包括过期的，用于管理员）
     * 不过滤已过期的消息
     *
     * @param receiverId 接收者用户ID
     * @return 该用户的所有消息列表（包括过期）
     */
    public List<Message> findByReceiverIdIncludingExpired(Long receiverId) {
        return messageRepository.findByReceiverId(receiverId);
    }

    public List<Message> findRecentMessagesByReceiverId(Long receiverId, int limit) {
        List<Message> messages = new ArrayList<>(findByReceiverIdIncludingExpired(receiverId));
        messages.sort(Comparator.comparing(
                Message::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        if (limit > 0 && messages.size() > limit) {
            return new ArrayList<>(messages.subList(0, limit));
        }
        return messages;
    }

    public List<Message> findAllMessages() {
        return messageRepository.findAll();
    }

    public List<Message> findAllMessagesForAdmin(Long adminId) {
        // 首先获取管理员自己收到的所有消息
        List<Message> adminOwnMessages = messageRepository.findByReceiverId(adminId);
        
        // 然后获取管理员发送的所有通知（用于管理查看）
        List<Message> adminSentMessages = messageRepository.findBySenderId(adminId);
        Set<String> sentNotificationIds = new HashSet<>();
        
        // 添加管理员发送的通知（每个通知只显示一次）
        for (Message message : adminSentMessages) {
            if (message.getNotificationId() != null && !sentNotificationIds.contains(message.getNotificationId())) {
                sentNotificationIds.add(message.getNotificationId());
                // 检查是否已经在自己的消息列表中
                boolean exists = adminOwnMessages.stream()
                    .anyMatch(m -> message.getNotificationId().equals(m.getNotificationId()));
                if (!exists) {
                    adminOwnMessages.add(message);
                }
            }
        }
        
        return adminOwnMessages;
    }

    public List<Message> findAllMessagesForAdminWithAllStatus() {
        return messageRepository.findAll();
    }

    public boolean existsByTitleAndSenderId(String title, Long senderId) {
        return messageRepository.existsByTitleAndSenderId(title, senderId);
    }

    /**
     * 过滤有效消息，排除已过期的定时公告
     *
     * @param messages 原始消息列表
     * @return 过滤后的有效消息列表
     */
    private List<Message> filterValidMessages(List<Message> messages) {
        List<Message> validMessages = new ArrayList<>();
        Date now = new Date();

        for (Message message : messages) {
            if (isMessageValid(message, now)) {
                validMessages.add(message);
            }
        }
        return validMessages;
    }

    /**
     * 判断消息是否有效
     *
     * @param message 消息实体
     * @param now 当前时间
     * @return 消息是否有效
     */
    private boolean isMessageValid(Message message, Date now) {
        if ("notification".equals(message.getType()) ||
            "homework_create".equals(message.getType()) ||
            "homework_update".equals(message.getType()) ||
            "homework_delete".equals(message.getType()) ||
            "submission_graded".equals(message.getType())) {
            return true;
        }
        if (message.getPublishTime() != null && message.getPublishTime().after(now)) {
            return false;
        }
        if (message.getExpireTime() == null) {
            return "active".equals(message.getStatus());
        }
        return message.getExpireTime().after(now);
    }

    /**
     * 查询用户的未读消息
     * 同时过滤掉已过期的消息
     *
     * @param receiverId 接收者用户ID
     * @return 有效的未读消息列表
     */
    public List<Message> findUnreadMessages(Long receiverId) {
        // 先从数据库查询所有未读消息
        List<Message> allUnreadMessages = messageRepository.findByReceiverIdAndIsReadFalse(receiverId);
        List<Message> validUnreadMessages = new ArrayList<>();
        Date now = new Date();

        // 过滤有效的消息
        for (Message message : allUnreadMessages) {
            // 通知类消息（作业相关）始终有效
            if ("notification".equals(message.getType()) ||
                "homework_create".equals(message.getType()) ||
                "homework_update".equals(message.getType()) ||
                "homework_delete".equals(message.getType()) ||
                "submission_graded".equals(message.getType())) {
                validUnreadMessages.add(message);
            }
            // 消息尚未发布（发布时间晚于当前时间）
            else if (message.getPublishTime() != null && message.getPublishTime().after(now)) {
                continue;
            }
            // 无过期时间且状态为活跃的消息
            else if (message.getExpireTime() == null) {
                if ("active".equals(message.getStatus())) {
                    validUnreadMessages.add(message);
                }
            }
            // 有过期时间且未过期的消息
            else {
                if (message.getExpireTime().after(now)) {
                    validUnreadMessages.add(message);
                }
            }
        }
        return validUnreadMessages;
    }

    /**
     * 标记消息为已读
     * - 所有用户（包括管理员）都只能标记自己收到的消息为已读
     *
     * @param id 消息ID
     * @param currentUserId 当前登录用户ID
     * @return 更新后的消息实体
     */
    public Message markAsRead(Long id, Long currentUserId) {
        Optional<Message> optionalMessage = messageRepository.findById(id);
        if (optionalMessage.isPresent()) {
            Message message = optionalMessage.get();
            
            // 检查权限：所有用户都只能标记自己的消息为已读
            if (!message.getReceiverId().equals(currentUserId)) {
                throw new RuntimeException("无权限标记此消息为已读");
            }
            
            message.setIsRead(true);
            return messageRepository.save(message);
        }
        return null;
    }

    /**
     * 删除消息
     * - 普通用户（学生/教师）：只能删除自己收到的消息
     * - 管理员：可以删除任何消息，如果有关联通知ID，可以删除所有关联消息
     *
     * @param id 消息ID
     * @param currentUserId 当前登录用户ID
     * @param isAdmin 是否是管理员
     */
    public void deleteMessage(Long id, Long currentUserId, boolean isAdmin) {
        Message message = findById(id);
        if (message != null) {
            // 检查权限：只有管理员或消息接收者本人才能删除
            boolean hasPermission = isAdmin || 
                (message.getReceiverId() != null && message.getReceiverId().equals(currentUserId));
            
            if (!hasPermission) {
                throw new RuntimeException("无权限删除此消息");
            }
            
            if (message.getNotificationId() != null && isAdmin) {
                // 只有管理员可以通过 notificationId 删除所有关联消息
                messageRepository.deleteByNotificationId(message.getNotificationId());
            } else {
                // 普通用户只能删除自己的消息（单条删除）
                messageRepository.deleteById(id);
            }
        }
    }

    /**
     * 根据通知ID删除所有相关消息
     * 用于删除与某个作业或提交相关的所有通知
     *
     * @param notificationId 通知ID
     */
    public void deleteAllByNotificationId(String notificationId) {
        messageRepository.deleteByNotificationId(notificationId);
    }

    /**
     * 根据接收者和消息类型查询消息列表
     *
     * @param receiverId 接收者用户ID
     * @param type 消息类型
     * @return 符合条件的消息列表
     */
    public List<Message> findByReceiverIdAndType(Long receiverId, String type) {
        return messageRepository.findByReceiverIdAndType(receiverId, type);
    }

    /**
     * 根据通知ID查询消息列表
     *
     * @param notificationId 通知ID
     * @return 符合条件的消息列表
     */
    public List<Message> findByNotificationId(String notificationId) {
        return messageRepository.findByNotificationId(notificationId);
    }
}
