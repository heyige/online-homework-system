package com.biyesheji.onlinehomework.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Date;

/**
 * 个人信息修改申请实体类 - 对应数据库中的 profile_requests 表
 *
 * 描述：存储用户提交的个人信息修改申请，如修改姓名、邮箱、手机号、专业等信息
 * 申请需要管理员审核通过后才能生效
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
@Table(name = "profile_requests")
public class ProfileRequest {

    /**
     * 申请记录唯一标识符
     * @Id: 标记为主键
     * @GeneratedValue: 主键生成策略，IDENTITY 表示由数据库自增生成
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 申请创建时间
     */
    @Column(name = "created_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    /**
     * 申请修改后的院系/部门
     */
    @Column(length = 255)
    private String department;

    /**
     * 申请修改后的电子邮箱
     */
    @Column(length = 255)
    private String email;

    /**
     * 申请修改后的专业
     */
    @Column(length = 255)
    private String major;

    /**
     * 申请修改后的真实姓名
     * nullable = false: 不能为空
     */
    @Column(nullable = false, length = 255)
    private String name;

    /**
     * 申请修改后的手机号码
     */
    @Column(length = 255)
    private String phone;

    /**
     * 申请状态
     * 取值范围: "pending"（待审核）、"approved"（已通过）、"rejected"（已拒绝）
     * nullable = false: 不能为空
     */
    @Column(nullable = false, length = 255)
    private String status;

    /**
     * 申请修改后的学号
     */
    @Column(name = "student_id", length = 255)
    private String studentId;

    /**
     * 申请最后更新时间
     */
    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    /**
     * 申请提交者的用户ID
     * nullable = false: 不能为空
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 申请提交者的用户名
     * nullable = false: 不能为空
     */
    @Column(nullable = false, length = 255)
    private String username;

    /**
     * 申请修改后的头像 URL 或 Base64 数据
     */
    @Column(columnDefinition = "MEDIUMTEXT")
    private String avatar;

    /**
     * JPA 生命周期回调 - 在 entity 首次插入数据库前自动调用
     * 用于自动设置创建时间和更新时间
     */
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = new Date();
        }
        if (updatedAt == null) {
            updatedAt = new Date();
        }
    }

    /**
     * JPA 生命周期回调 - 在 entity 更新到数据库前自动调用
     * 用于自动更新修改时间
     */
    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }
}
