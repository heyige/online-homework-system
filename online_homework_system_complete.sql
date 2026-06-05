-- ============================================================
-- 在线作业管理系统 - 数据库初始化脚本
-- 版本：1.0.0
-- 日期：2024-04-12
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================================
-- 创建数据库
-- ============================================================
CREATE DATABASE IF NOT EXISTS `online_homework_system` 
DEFAULT CHARACTER SET utf8mb4 
DEFAULT COLLATE utf8mb4_general_ci;

USE `online_homework_system`;

-- ============================================================
-- 用户表
-- ============================================================
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `username` varchar(255) NOT NULL COMMENT '用户名',
  `password` varchar(255) NOT NULL COMMENT '密码（BCrypt 加密）',
  `name` varchar(255) NOT NULL COMMENT '姓名',
  `role` varchar(50) NOT NULL COMMENT '角色（STUDENT/TEACHER/ADMIN）',
  `email` varchar(255) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(50) DEFAULT NULL COMMENT '手机号',
  `department` varchar(255) DEFAULT NULL COMMENT '院系/部门',
  `major` varchar(255) DEFAULT NULL COMMENT '专业',
  `student_id` varchar(50) DEFAULT NULL COMMENT '学号',
  `avatar` mediumtext DEFAULT NULL COMMENT '头像 URL 或 Base64',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_role` (`role`),
  KEY `idx_email` (`email`),
  KEY `idx_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ============================================================
-- 作业表
-- ============================================================
DROP TABLE IF EXISTS `homework`;
CREATE TABLE `homework` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL COMMENT '作业标题',
  `description` text COMMENT '作业描述',
  `teacher_id` bigint(20) NOT NULL COMMENT '教师 ID',
  `course_name` varchar(255) DEFAULT NULL COMMENT '课程名称',
  `deadline` datetime DEFAULT NULL COMMENT '截止时间',
  `max_score` double DEFAULT NULL COMMENT '满分',
  `allow_modification` tinyint(1) DEFAULT 1 COMMENT '是否允许修改',
  `status` varchar(50) DEFAULT 'published' COMMENT '状态（published/expired/withdrawn）',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_teacher_id` (`teacher_id`),
  KEY `idx_status` (`status`),
  KEY `idx_deadline` (`deadline`),
  KEY `idx_course_name` (`course_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业表';

-- ============================================================
-- 作业学生关联表
-- ============================================================
DROP TABLE IF EXISTS `homework_student`;
CREATE TABLE `homework_student` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `homework_id` bigint(20) NOT NULL COMMENT '作业 ID',
  `student_id` bigint(20) NOT NULL COMMENT '学生 ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_homework_student` (`homework_id`, `student_id`),
  KEY `idx_student_id` (`student_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作业学生关联表';

-- ============================================================
-- 提交表
-- ============================================================
DROP TABLE IF EXISTS `submissions`;
CREATE TABLE `submissions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `homework_id` bigint(20) NOT NULL COMMENT '作业 ID',
  `student_id` bigint(20) NOT NULL COMMENT '学生 ID',
  `content` text COMMENT '作业内容',
  `file_path` varchar(500) DEFAULT NULL COMMENT '文件路径',
  `submitted_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  `score` double DEFAULT NULL COMMENT '分数',
  `feedback` text COMMENT '评语',
  `graded_at` timestamp NULL DEFAULT NULL COMMENT '批改时间',
  `status` varchar(50) DEFAULT 'submitted' COMMENT '状态（submitted/graded）',
  `similarity_score` double DEFAULT NULL COMMENT '相似度分数',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_homework_student` (`homework_id`, `student_id`),
  KEY `idx_homework_id` (`homework_id`),
  KEY `idx_student_id` (`student_id`),
  KEY `idx_status` (`status`),
  CONSTRAINT `fk_submission_homework` FOREIGN KEY (`homework_id`) REFERENCES `homework` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_submission_student` FOREIGN KEY (`student_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提交表';

-- ============================================================
-- 消息表
-- ============================================================
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL COMMENT '消息标题',
  `content` text COMMENT '消息内容',
  `sender_id` bigint(20) DEFAULT NULL COMMENT '发送者 ID',
  `receiver_id` bigint(20) NOT NULL COMMENT '接收者 ID',
  `type` varchar(50) DEFAULT 'notification' COMMENT '类型（notification/permanent/system）',
  `status` varchar(50) DEFAULT 'active' COMMENT '状态（active/inactive）',
  `is_read` tinyint(1) DEFAULT 0 COMMENT '是否已读',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间',
  `notification_id` varchar(255) DEFAULT NULL COMMENT '通知 ID（用于批量操作）',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_receiver_id` (`receiver_id`),
  KEY `idx_sender_id` (`sender_id`),
  KEY `idx_type` (`type`),
  KEY `idx_is_read` (`is_read`),
  KEY `idx_notification_id` (`notification_id`),
  CONSTRAINT `fk_message_sender` FOREIGN KEY (`sender_id`) REFERENCES `users` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_message_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- ============================================================
-- 个人信息修改申请表
-- ============================================================
DROP TABLE IF EXISTS `profile_requests`;
CREATE TABLE `profile_requests` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) NOT NULL COMMENT '用户 ID',
  `username` varchar(255) NOT NULL COMMENT '用户名',
  `name` varchar(255) NOT NULL COMMENT '姓名',
  `email` varchar(255) DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(50) DEFAULT NULL COMMENT '手机号',
  `department` varchar(255) DEFAULT NULL COMMENT '院系',
  `major` varchar(255) DEFAULT NULL COMMENT '专业',
  `student_id` varchar(50) DEFAULT NULL COMMENT '学号',
  `avatar` mediumtext DEFAULT NULL COMMENT '头像',
  `status` varchar(50) NOT NULL DEFAULT 'pending' COMMENT '状态（pending/approved/rejected）',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个人信息修改申请表';

-- ============================================================
-- 初始化测试数据
-- ============================================================

-- 插入管理员账号（密码：admin123）
INSERT INTO `users` (`username`, `password`, `name`, `role`, `email`) 
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5VfKzqJLqKzXqY8K5qJ5qJ5', '系统管理员', 'ADMIN', 'admin@example.com');

-- 插入教师账号（密码：teacher123）
INSERT INTO `users` (`username`, `password`, `name`, `role`, `email`, `department`) 
VALUES ('teacher1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5VfKzqJLqKzXqY8K5qJ5qJ5', '张老师', 'TEACHER', 'teacher@example.com', '计算机学院');

-- 插入学生账号（密码：student123）
INSERT INTO `users` (`username`, `password`, `name`, `role`, `email`, `student_id`, `major`) 
VALUES ('student1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5VfKzqJLqKzXqY8K5qJ5qJ5', '李学生', 'STUDENT', 'student@example.com', '2024100001', '软件工程');

INSERT INTO `users` (`username`, `password`, `name`, `role`, `email`, `student_id`, `major`) 
VALUES ('student2', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5VfKzqJLqKzXqY8K5qJ5qJ5', '王学生', 'STUDENT', 'student2@example.com', '2024100002', '软件工程');

-- 插入示例作业
INSERT INTO `homework` (`title`, `description`, `teacher_id`, `course_name`, `deadline`, `max_score`, `allow_modification`, `status`) 
VALUES 
('第一次作业 - Java 基础', '完成以下编程题：\n1. 实现一个计算器\n2. 实现学生管理系统', 2, 'Java 程序设计', DATE_ADD(NOW(), INTERVAL 7 DAY), 100, 1, 'published'),
('第二次作业 - 面向对象', '设计一个图书馆管理系统', 2, 'Java 程序设计', DATE_ADD(NOW(), INTERVAL 14 DAY), 100, 1, 'published'),
('数据库原理作业', '完成 SQL 练习题 1-10', 2, '数据库原理', DATE_ADD(NOW(), INTERVAL 10 DAY), 100, 0, 'published');

-- 插入作业与学生关联
INSERT INTO `homework_student` (`homework_id`, `student_id`) VALUES (1, 3), (1, 4), (2, 3), (2, 4), (3, 3), (3, 4);

-- 插入示例提交
INSERT INTO `submissions` (`homework_id`, `student_id`, `content`, `status`, `score`, `feedback`) 
VALUES 
(1, 3, '这是我的作业答案...\n\n计算器实现代码：\npublic class Calculator {...}', 'graded', 95, '完成得很好，代码规范！'),
(1, 4, '作业答案...\n\n计算器代码实现', 'submitted', NULL, NULL);

-- 插入示例消息
INSERT INTO `message` (`title`, `content`, `sender_id`, `receiver_id`, `type`, `status`, `is_read`) 
VALUES 
('欢迎使用在线作业管理系统', '感谢您使用本系统，祝您学习愉快！', 1, 3, 'permanent', 'active', 0),
('欢迎使用在线作业管理系统', '感谢您使用本系统，祝您教学愉快！', 1, 2, 'permanent', 'active', 0),
('作业提交成功', '您的作业已成功提交', 2, 3, 'notification', 'active', 0);

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 数据库初始化完成
-- ============================================================
