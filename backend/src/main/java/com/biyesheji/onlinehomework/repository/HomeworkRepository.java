package com.biyesheji.onlinehomework.repository;

import com.biyesheji.onlinehomework.model.Homework;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

/**
 * 作业数据访问层接口
 *
 * 描述：负责与数据库中的 homework 表进行交互
 * 继承 JpaRepository，自动提供基础的 CRUD 操作
 *
 * @Repository: 标记为 Spring 数据访问组件
 */
@Repository
public interface HomeworkRepository extends JpaRepository<Homework, Long> {

    /**
     * 根据教师ID查询该教师发布的所有作业
     *
     * @param teacherId 教师用户ID
     * @return 该教师发布的作业列表
     */
    List<Homework> findByTeacherId(Long teacherId);

    /**
     * 根据作业状态查询作业列表
     *
     * @param status 作业状态 (draft/published/closed)
     * @return 符合条件状态的作业列表
     */
    List<Homework> findByStatus(String status);

    /**
     * 查询已过截止日期的作业
     *
     * @param deadline 截止日期
     * @return 已过期的作业列表
     */
    List<Homework> findByDeadlineBefore(Date deadline);

    /**
     * 根据课程名称查询作业
     *
     * @param courseName 课程名称
     * @return 符合课程名称的作业列表
     */
    List<Homework> findByCourseName(String courseName);

    /**
     * 根据多个教师ID查询作业
     * 用于查询特定教师列表发布的作业
     *
     * @param teacherIds 教师ID列表
     * @return 符合条件的所有作业
     */
    List<Homework> findByTeacherIdIn(List<Long> teacherIds);

    /**
     * 根据教师ID删除该教师发布的所有作业
     * 用于删除教师时级联删除其作业
     *
     * @param teacherId 教师用户ID
     */
    void deleteByTeacherId(Long teacherId);
}
