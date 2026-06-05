package com.biyesheji.onlinehomework.repository;

import com.biyesheji.onlinehomework.model.HomeworkStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 作业-学生关联数据访问层接口
 *
 * 描述：负责与数据库中的 homework_student 表进行交互
 * 实现作业分配给学生功能的数据访问
 * 继承 JpaRepository，自动提供基础的 CRUD 操作
 *
 * @Repository: 标记为 Spring 数据访问组件
 */
@Repository
public interface HomeworkStudentRepository extends JpaRepository<HomeworkStudent, Long> {

    /**
     * 根据作业ID查询所有分配的学生
     * 获取某个作业分配给了哪些学生
     *
     * @param homeworkId 作业ID
     * @return 该作业分配的所有学生关联记录
     */
    List<HomeworkStudent> findByHomeworkId(Long homeworkId);

    /**
     * 根据学生ID查询该学生收到的所有作业分配
     * 获取某个学生被分配了哪些作业
     *
     * @param studentId 学生用户ID
     * @return 该学生收到的所有作业分配记录
     */
    List<HomeworkStudent> findByStudentId(Long studentId);

    /**
     * 根据作业ID删除该作业的所有学生分配记录（修改操作）
     * 用于删除作业时清理关联数据
     *
     * @param homeworkId 作业ID
     * @Modifying: 标记为修改操作，用于 DELETE
     */
    @Modifying
    @Query("DELETE FROM HomeworkStudent hs WHERE hs.homeworkId = :homeworkId")
    void deleteByHomeworkId(@Param("homeworkId") Long homeworkId);

    /**
     * 根据学生ID删除该学生的所有作业分配记录（修改操作）
     * 用于删除学生时清理关联数据
     *
     * @param studentId 学生用户ID
     */
    @Modifying
    @Query("DELETE FROM HomeworkStudent hs WHERE hs.studentId = :studentId")
    void deleteByStudentId(@Param("studentId") Long studentId);

    /**
     * 判断某个作业是否已分配给某个学生
     *
     * @param homeworkId 作业ID
     * @param studentId 学生用户ID
     * @return true 表示已存在分配关系，false 表示不存在
     */
    boolean existsByHomeworkIdAndStudentId(Long homeworkId, Long studentId);
}
