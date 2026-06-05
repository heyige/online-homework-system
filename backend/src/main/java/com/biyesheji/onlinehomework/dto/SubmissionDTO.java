package com.biyesheji.onlinehomework.dto;

import com.biyesheji.onlinehomework.model.Homework;
import com.biyesheji.onlinehomework.model.Submission;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * 作业提交详情 DTO
 *
 * 描述：包含提交信息和关联的作业信息
 * 用于在提交列表中同时展示作业详情和学生信息
 *
 * 聚合了 Submission（提交实体）、Homework（作业实体）、学生姓名等信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionDTO {

    /**
     * 提交记录ID
     */
    private Long id;

    /**
     * 作业ID
     */
    private Long homeworkId;

    /**
     * 学生用户ID
     */
    private Long studentId;

    /**
     * 提交的文本内容
     */
    private String content;

    /**
     * 附件文件服务器存储路径
     */
    private String filePath;

    /**
     * 附件文件原始名称
     */
    private String fileName;

    /**
     * 提交时间
     */
    private java.util.Date submittedAt;

    /**
     * 成绩分数
     */
    private Double score;

    /**
     * 教师批语
     */
    private String feedback;

    /**
     * 批改时间
     */
    private java.util.Date gradedAt;

    /**
     * 提交状态
     * 取值: "submitted"（已提交）、"graded"（已批改）
     */
    private String status;

    /**
     * 查重相似度分数
     */
    private Double similarityScore;

    // ========== 作业信息（关联查询得到）==========

    /**
     * 作业标题
     */
    private String homeworkTitle;

    /**
     * 课程名称
     */
    private String courseName;

    /**
     * 发布作业的教师姓名
     */
    private String teacherName;

    /**
     * 作业截止时间
     */
    private java.util.Date deadline;

    /**
     * 作业满分
     */
    private Double maxScore;

    /**
     * 是否允许修改
     */
    private Boolean allowModification;

    /**
     * 作业描述
     */
    private String description;

    // ========== 学生信息（关联查询得到）==========

    /**
     * 提交作业的学生姓名
     */
    private String studentName;

    /**
     * 构造函数 - 从提交和作业创建 DTO
     *
     * @param submission 提交实体
     * @param homework 作业实体
     */
    public SubmissionDTO(Submission submission, Homework homework) {
        this.id = submission.getId();
        this.homeworkId = submission.getHomeworkId();
        this.studentId = submission.getStudentId();
        this.content = submission.getContent();
        this.filePath = submission.getFilePath();
        this.submittedAt = submission.getSubmittedAt();
        this.score = submission.getScore();
        this.feedback = submission.getFeedback();
        this.gradedAt = submission.getGradedAt();
        this.status = submission.getStatus();
        this.similarityScore = submission.getSimilarityScore();

        if (homework != null) {
            this.homeworkTitle = homework.getTitle();
            this.courseName = homework.getCourseName();
            this.deadline = homework.getDeadline();
            this.maxScore = homework.getMaxScore();
            this.allowModification = homework.getAllowModification();
            this.description = homework.getDescription();
        }
    }

    /**
     * 构造函数 - 从提交、作业和学生姓名创建 DTO
     *
     * @param submission 提交实体
     * @param homework 作业实体
     * @param studentName 学生姓名
     */
    public SubmissionDTO(Submission submission, Homework homework, String studentName) {
        this(submission, homework);
        this.studentName = studentName;
    }
}
