package com.biyesheji.onlinehomework.dto;

import com.biyesheji.onlinehomework.model.Homework;
import com.biyesheji.onlinehomework.model.Submission;
import com.biyesheji.onlinehomework.model.User;

/**
 * 学生作业 DTO
 *
 * 描述：用于学生端展示作业列表
 * 包含作业信息和学生的提交状态
 *
 * 聚合了 Homework（作业实体）、Submission（提交实体）、教师用户等信息
 */
public class StudentHomeworkDTO {

    /**
     * 作业实体（完整信息）
     */
    private Homework homework;

    /**
     * 提交状态
     * 取值: "未提交"、"已提交"、"已批改"
     */
    private String submissionStatus;

    /**
     * 成绩分数（如果有）
     */
    private Double score;

    /**
     * 教师批语（如果有）
     */
    private String feedback;

    /**
     * 发布作业的教师姓名
     */
    private String teacherName;

    /**
     * 无参构造函数
     */
    public StudentHomeworkDTO() {
    }

    /**
     * 构造函数 - 仅作业信息（未提交状态）
     *
     * @param homework 作业实体
     */
    public StudentHomeworkDTO(Homework homework) {
        this.homework = homework;
        this.submissionStatus = "未提交";
    }

    /**
     * 构造函数 - 作业和提交信息
     *
     * @param homework 作业实体
     * @param submission 提交实体（可能为 null）
     */
    public StudentHomeworkDTO(Homework homework, Submission submission) {
        this.homework = homework;
        if (submission == null) {
            this.submissionStatus = "未提交";
        } else if ("graded".equals(submission.getStatus())) {
            this.submissionStatus = "已批改";
            this.score = submission.getScore();
            this.feedback = submission.getFeedback();
        } else if ("submitted".equals(submission.getStatus())) {
            this.submissionStatus = "已提交";
        }
    }

    /**
     * 构造函数 - 包含教师信息
     *
     * @param homework 作业实体
     * @param submission 提交实体（可能为 null）
     * @param teacher 教师用户实体
     */
    public StudentHomeworkDTO(Homework homework, Submission submission, User teacher) {
        this.homework = homework;
        this.teacherName = teacher != null ? teacher.getName() : null;
        if (submission == null) {
            this.submissionStatus = "未提交";
        } else if ("graded".equals(submission.getStatus())) {
            this.submissionStatus = "已批改";
            this.score = submission.getScore();
            this.feedback = submission.getFeedback();
        } else if ("submitted".equals(submission.getStatus())) {
            this.submissionStatus = "已提交";
        }
    }

    // ========== Getter 和 Setter 方法 ==========

    public Homework getHomework() {
        return homework;
    }

    public void setHomework(Homework homework) {
        this.homework = homework;
    }

    public String getSubmissionStatus() {
        return submissionStatus;
    }

    public void setSubmissionStatus(String submissionStatus) {
        this.submissionStatus = submissionStatus;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public String getTeacherName() {
        return teacherName;
    }

    public void setTeacherName(String teacherName) {
        this.teacherName = teacherName;
    }
}
