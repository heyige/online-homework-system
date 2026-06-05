package com.biyesheji.onlinehomework.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 作业-学生关联实体类 - 对应数据库中的 homework_student 表
 *
 * 描述：表示作业与学生之间的分配关系
 * 一条记录表示某个作业分配给了某个学生
 * 用于实现作业的针对性分配功能（而非全班统一布置）
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
@Table(name = "homework_student")
public class HomeworkStudent {

    /**
     * 关联记录唯一标识符
     * @Id: 标记为主键
     * @GeneratedValue: 主键生成策略，IDENTITY 表示由数据库自增生成
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 作业ID
     * 关联的作业标识符
     * nullable = false: 不能为空
     */
    @Column(name = "homework_id", nullable = false)
    private Long homeworkId;

    /**
     * 学生用户ID
     * 接收该作业的学生标识符
     * nullable = false: 不能为空
     */
    @Column(name = "student_id", nullable = false)
    private Long studentId;
}
