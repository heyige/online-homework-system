package com.biyesheji.onlinehomework.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Date;

/**
 * 消息实体类 - 对应数据库中的 message 表
 *
 * 描述：存储系统内的消息/通知信息，包括消息标题、内容、发送者、接收者、已读状态等
 * 用于系统向用户发送通知，如作业发布提醒、成绩发布通知等
 *
 * @Data: Lombok 注解，自动生成 get/set/toString 等方法
 * @NoArgsConstructor: 生成无参构造函数
 * @AllArgsConstructor: 生成全参构造函数
 * @Entity: 标记为 JPA 实体类
 * @Table: 指定对应的数据库表名
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "message")
public class Message {

    /**
     * 消息唯一标识符
     * @Id: 标记为主键
     * @GeneratedValue: 主键生成策略，IDENTITY 表示由数据库自增生成
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 消息标题
     * nullable = false: 不能为空
     */
    @Column(nullable = false, length = 255)
    private String title;

    /**
     * 消息正文内容
     * columnDefinition = "TEXT": 使用 TEXT 类型存储，支持长文本
     */
    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * 消息发送者用户ID
     * 可以为 null（系统消息无发送者）
     */
    @Column(name = "sender_id")
    private Long senderId;

    /**
     * 消息接收者用户ID
     * nullable = false: 不能为空
     */
    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    /**
     * 消息类型
     * 取值范围: "homework"（作业通知）、"grade"（成绩通知）、"system"（系统消息）等
     */
    @Column(length = 255)
    private String type;

    /**
     * 消息状态
     * 取值范围: "sent"（已发送）、"read"（已读）等
     */
    @Column(length = 255)
    private String status;

    /**
     * 是否已读标记
     * 默认值为 false（未读）
     */
    @Column(name = "is_read")
    private Boolean isRead = false;

    /**
     * 消息创建时间
     * updatable = false: 创建后不可修改
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    /**
     * 消息过期时间
     * 可选字段，用于设置消息的有效期，过期后自动失效
     */
    @Column(name = "expire_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date expireTime;

    @Column(name = "publish_time")
    @Temporal(TemporalType.TIMESTAMP)
    private Date publishTime;

    /**
     * 关联的通知ID
     * 用于关联到具体的作业ID或其他业务ID
     * 例如：作业ID、提交ID等
     */
    @Column(name = "notification_id", length = 255)
    private String notificationId;

    /**
     * JPA 生命周期回调 - 在 entity 首次插入数据库前自动调用
     * 用于自动设置创建时间
     */
    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
    }

    /**
     * 判断消息是否已读的便捷方法
     * 避免空指针异常
     */
    public boolean isRead() {
        return isRead != null && isRead;
    }
}
