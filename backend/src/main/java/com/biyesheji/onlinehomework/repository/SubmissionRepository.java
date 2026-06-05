package com.biyesheji.onlinehomework.repository;

import com.biyesheji.onlinehomework.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 作业提交数据访问层接口
 *
 * 描述：负责与数据库中的 submissions 表进行交互
 * 继承 JpaRepository，自动提供基础的 CRUD 操作
 *
 * @Repository: 标记为 Spring 数据访问组件
 */
@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {

    /**
     * 根据作业ID查询该作业的所有提交记录
     *
     * @param homeworkId 作业ID
     * @return 该作业的所有提交列表
     */
    List<Submission> findByHomeworkId(Long homeworkId);

    /**
     * 根据学生ID查询该学生的所有提交记录
     *
     * @param studentId 学生用户ID
     * @return 该学生的所有提交列表
     */
    List<Submission> findByStudentId(Long studentId);

    /**
     * 根据作业ID和学生ID查询特定提交
     * 用于查询某个学生对某个作业的提交
     *
     * @param homeworkId 作业ID
     * @param studentId 学生用户ID
     * @return 符合条件的提交记录（最多一条）
     */
    List<Submission> findByHomeworkIdAndStudentId(Long homeworkId, Long studentId);

    /**
     * 根据作业ID和提交状态查询提交
     *
     * @param homeworkId 作业ID
     * @param status 提交状态 (submitted/graded)
     * @return 符合条件的所有提交
     */
    List<Submission> findByHomeworkIdAndStatus(Long homeworkId, String status);

    /**
     * 根据学生ID和提交状态查询提交
     *
     * @param studentId 学生用户ID
     * @param status 提交状态
     * @return 符合条件的所有提交
     */
    List<Submission> findByStudentIdAndStatus(Long studentId, String status);

    /**
     * 根据学生ID删除该学生的所有提交记录
     * 用于删除学生时级联删除其提交
     *
     * @param studentId 学生用户ID
     */
    void deleteByStudentId(Long studentId);
}
