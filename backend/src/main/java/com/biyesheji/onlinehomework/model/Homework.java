package com.biyesheji.onlinehomework.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Date;

/**
 * 作业实体类 - 对应数据库中的 homework 表
 *
 * 描述：存储教师发布的作业信息，包括作业标题、描述、课程名称、截止时间等
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
@Table(name = "homework")
public class Homework {

    /**
     * 作业唯一标识符
     * @Id: 标记为主键
     * @GeneratedValue: 主键生成策略，IDENTITY 表示由数据库自增生成
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 作业标题
     * nullable = false: 不能为空
     * length = 255: 最大 255 字符
     */
    @Column(nullable = false, length = 255)
    private String title;

    /**
     * 作业描述/内容详情
     * columnDefinition = "TEXT": 使用 TEXT 类型存储，支持长文本
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 发布作业的教师用户ID
     * name = "teacher_id": 对应数据库列名 teacher_id
     * nullable = false: 不能为空
     */
    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    /**
     * 所属课程名称
     * name = "course_name": 对应数据库列名 course_name
     */
    @Column(name = "course_name", length = 255)
    private String courseName;

    /**
     * 作业截止时间
     * @Temporal: 指定日期时间格式，TIMESTAMP 表示包含日期和时间
     */
    @Column
    @Temporal(TemporalType.TIMESTAMP)
    private Date deadline;

    /**
     * 创建时间 - 作业发布时间
     * updatable = false: 创建后不可修改
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    /**
     * 更新时间 - 作业信息最后修改时间
     */
    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    /**
     * 作业状态
     * 取值范围: "draft"（草稿）、"published"（已发布）、"closed"（已关闭）
     */
    @Column(length = 255)
    private String status;

    /**
     * 作业满分分数
     */
    @Column(name = "max_score")
    private Double maxScore;

    /**
     * 是否允许学生修改已提交的作业
     * 默认值为 true（允许修改）
     */
    @Column(name = "allow_modification")
    private Boolean allowModification = true;

    /**
     * 发布作业的教师姓名（非持久化字段，用于接口返回）
     */
    @Transient
    private String teacherName;

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
