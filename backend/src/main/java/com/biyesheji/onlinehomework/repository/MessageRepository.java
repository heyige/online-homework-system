package com.biyesheji.onlinehomework.repository;

import com.biyesheji.onlinehomework.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 消息数据访问层接口
 *
 * 描述：负责与数据库中的 message 表进行交互
 * 继承 JpaRepository，自动提供基础的 CRUD 操作
 *
 * @Repository: 标记为 Spring 数据访问组件
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * 根据接收者ID查询所有收到的消息
     *
     * @param receiverId 接收者用户ID
     * @return 该用户收到的所有消息列表
     */
    List<Message> findByReceiverId(Long receiverId);

    /**
     * 查询某用户的所有未读消息
     *
     * @param receiverId 接收者用户ID
     * @return 该用户的所有未读消息
     */
    List<Message> findByReceiverIdAndIsReadFalse(Long receiverId);

    /**
     * 根据接收者和消息类型查询消息
     *
     * @param receiverId 接收者用户ID
     * @param type 消息类型
     * @return 符合条件的所有消息
     */
    List<Message> findByReceiverIdAndType(Long receiverId, String type);

    /**
     * 根据关联的通知ID查询消息
     * 用于查找与某个作业/提交相关的通知
     *
     * @param notificationId 通知ID（如作业ID、提交ID）
     * @return 符合条件的所有消息
     */
    List<Message> findByNotificationId(String notificationId);

    /**
     * 根据通知ID删除消息（修改操作）
     *
     * @param notificationId 通知ID
     * @Modifying: 标记为修改操作，用于 UPDATE/DELETE
     */
    @Modifying
    @Query("DELETE FROM Message m WHERE m.notificationId = :notificationId")
    void deleteByNotificationId(@Param("notificationId") String notificationId);

    /**
     * 根据通知ID和消息类型删除消息
     *
     * @param notificationId 通知ID
     * @param type 消息类型
     */
    @Modifying
    @Query("DELETE FROM Message m WHERE m.notificationId = :notificationId AND m.type = :type")
    void deleteByNotificationIdAndType(@Param("notificationId") String notificationId, @Param("type") String type);

    /**
     * 根据发送者ID查询发送的消息
     *
     * @param senderId 发送者用户ID
     * @return 该用户发送的所有消息
     */
    List<Message> findBySenderId(Long senderId);

    /**
     * 删除接收者的所有消息（修改操作）
     * 用于删除用户时清理消息
     *
     * @param receiverId 接收者用户ID
     */
    @Modifying
    @Query("DELETE FROM Message m WHERE m.receiverId = :receiverId")
    void deleteByReceiverId(@Param("receiverId") Long receiverId);

    /**
     * 删除发送者的所有消息（修改操作）
     * 用于删除用户时清理消息
     *
     * @param senderId 发送者用户ID
     */
    @Modifying
    @Query("DELETE FROM Message m WHERE m.senderId = :senderId")
    void deleteBySenderId(@Param("senderId") Long senderId);

    boolean existsByTitleAndSenderId(String title, Long senderId);
}
