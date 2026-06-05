package com.biyesheji.onlinehomework.repository;

import com.biyesheji.onlinehomework.model.ProfileRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 个人信息修改申请数据访问层接口
 *
 * 描述：负责与数据库中的 profile_requests 表进行交互
 * 继承 JpaRepository，自动提供基础的 CRUD 操作
 *
 * @Repository: 标记为 Spring 数据访问组件
 */
@Repository
public interface ProfileRequestRepository extends JpaRepository<ProfileRequest, Long> {

    /**
     * 根据用户ID查询该用户提交的所有申请
     *
     * @param userId 用户ID
     * @return 该用户的所有申请记录
     */
    List<ProfileRequest> findByUserId(Long userId);

    /**
     * 根据申请状态查询申请列表
     *
     * @param status 申请状态 (pending/approved/rejected)
     * @return 符合状态的申请列表
     */
    List<ProfileRequest> findByStatus(String status);

    /**
     * 根据用户ID和申请状态查询申请
     * 用于查看某用户的特定状态的申请
     *
     * @param userId 用户ID
     * @param status 申请状态
     * @return 符合条件的申请列表
     */
    List<ProfileRequest> findByUserIdAndStatus(Long userId, String status);

    /**
     * 根据用户ID删除该用户的所有申请记录（修改操作）
     * 用于删除用户时级联删除其申请
     *
     * @param userId 用户ID
     * @Modifying: 标记为修改操作，用于 DELETE
     */
    @Modifying
    @Query("DELETE FROM ProfileRequest p WHERE p.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
