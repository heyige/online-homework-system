package com.biyesheji.onlinehomework.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Date;

/**
 * 用户实体类 - 对应数据库中的 users 表
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
@Table(name = "users")
public class User {

    /**
     * 用户唯一标识符
     * @Id: 标记为主键
     * @GeneratedValue: 主键生成策略，IDENTITY 表示由数据库自增生成
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户名 - 登录账号
     * @Column: 列配置
     *   - nullable = false: 不能为空
     *   - unique = true: 值唯一，不能重复
     *   - length = 255: 字符串最大长度 255 字符
     */
    @Column(nullable = false, unique = true, length = 255)
    private String username;

    /**
     * 密码 - 经过 BCrypt 加密存储
     * nullable = false: 不能为空
     */
    @Column(nullable = false, length = 255)
    private String password;

    /**
     * 真实姓名
     * nullable = false: 不能为空
     */
    @Column(nullable = false, length = 255)
    private String name;

    /**
     * 用户角色 - ADMIN（管理员）/ TEACHER（教师）/ STUDENT（学生）
     * nullable = false: 不能为空
     */
    @Column(nullable = false, length = 255)
    private String role;

    /**
     * 电子邮箱
     */
    @Column(length = 255)
    private String email;

    /**
     * 手机号码
     */
    @Column(length = 255)
    private String phone;

    /**
     * 所属院系/部门
     */
    @Column(length = 255)
    private String department;

    /**
     * 所学专业
     */
    @Column(length = 255)
    private String major;

    /**
     * 学号 - 仅学生用户有此字段
     * name = "student_id": 对应数据库列名 student_id
     */
    @Column(name = "student_id", length = 255)
    private String studentId;

    /**
     * 创建时间 - 用户注册时间
     * @Temporal: 指定日期时间格式，TIMESTAMP 表示包含日期和时间
     * updatable = false: 创建后不可修改
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    /**
     * 更新时间 - 用户信息最后修改时间
     */
    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    /**
     * 头像图片路径/URL 或 Base64 数据
     */
    @Column(columnDefinition = "MEDIUMTEXT")
    private String avatar;

    /**
     * JPA 生命周期回调 - 在 entity 首次插入数据库前自动调用
     * 用于自动设置创建时间和更新时间
     */
    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        updatedAt = new Date();
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
