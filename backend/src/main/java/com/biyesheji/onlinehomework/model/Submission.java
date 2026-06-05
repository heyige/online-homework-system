package com.biyesheji.onlinehomework.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.Date;

/**
 * 作业提交实体类 - 对应数据库中的 submissions 表
 *
 * 描述：存储学生提交的作业信息，包括提交的文本内容、附件文件、成绩、批语等
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
@Table(name = "submissions")
public class Submission {

    /**
     * 提交记录唯一标识符
     * @Id: 标记为主键
     * @GeneratedValue: 主键生成策略，IDENTITY 表示由数据库自增生成
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 所属作业的ID
     * name = "homework_id": 对应数据库列名 homework_id
     * nullable = false: 不能为空
     */
    @Column(name = "homework_id", nullable = false)
    private Long homeworkId;

    /**
     * 提交作业的学生用户ID
     * name = "student_id": 对应数据库列名 student_id
     * nullable = false: 不能为空
     */
    @Column(name = "student_id", nullable = false)
    private Long studentId;

    /**
     * 提交的文本内容
     * columnDefinition = "TEXT": 使用 TEXT 类型存储，支持长文本
     */
    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * 附件文件的服务器存储路径
     * 用于标识文件在服务器上的实际存储位置
     */
    @Column(name = "file_path", length = 255)
    private String filePath;

    /**
     * 附件文件的原始文件名
     * 保存用户上传时的原始文件名，便于下载时使用
     */
    @Column(name = "file_name", length = 255)
    private String fileName;

    /**
     * 提交时间
     * @Temporal: 指定日期时间格式，TIMESTAMP 表示包含日期和时间
     * updatable = false: 创建后不可修改
     */
    @Column(name = "submitted_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private Date submittedAt;

    /**
     * 教师批改给出的成绩分数
     * 允许为 null，表示尚未批改
     */
    @Column
    private Double score;

    /**
     * 教师批改时给出的反馈/评语
     * columnDefinition = "TEXT": 使用 TEXT 类型存储，支持长文本
     */
    @Column(columnDefinition = "TEXT")
    private String feedback;

    /**
     * 批改时间
     * 记录教师完成批改的时间戳
     */
    @Column(name = "graded_at")
    @Temporal(TemporalType.TIMESTAMP)
    private Date gradedAt;

    /**
     * 提交状态
     * 取值范围: "submitted"（已提交）、"graded"（已批改）等
     */
    @Column(length = 255)
    private String status;

    /**
     * 作业查重相似度分数
     * 用于存储作业查重功能的相似度检测结果
     * 值范围 0.0 - 1.0，表示与其它作业的相似程度
     */
    @Column(name = "similarity_score")
    private Double similarityScore;

    /**
     * JPA 生命周期回调 - 在 entity 首次插入数据库前自动调用
     * 用于自动设置提交时间
     */
    @PrePersist
    protected void onCreate() {
        submittedAt = new Date();
    }
}
