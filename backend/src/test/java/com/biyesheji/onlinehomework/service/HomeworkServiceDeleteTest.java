package com.biyesheji.onlinehomework.service;

import com.biyesheji.onlinehomework.enums.MessageType;
import com.biyesheji.onlinehomework.model.Homework;
import com.biyesheji.onlinehomework.model.Message;
import com.biyesheji.onlinehomework.model.User;
import com.biyesheji.onlinehomework.repository.HomeworkRepository;
import com.biyesheji.onlinehomework.repository.MessageRepository;
import com.biyesheji.onlinehomework.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试用例：删除作业后使用已删除对象的ID问题
 * 
 * 问题描述：
 * 在 HomeworkService.deleteHomework() 方法中，删除作业后调用 sendHomeworkDeleteNotification()
 * 时使用了已删除的 homework 对象的 getId() 方法，可能导致 NullPointerException
 */
@SpringBootTest
@Transactional
public class HomeworkServiceDeleteTest {

    @Autowired
    private HomeworkService homeworkService;

    @Autowired
    private HomeworkRepository homeworkRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MessageRepository messageRepository;

    private User teacher;
    private User student1;
    private User student2;
    private Homework homework;

    /**
     * 测试前准备数据：创建教师、学生和作业
     */
    @BeforeEach
    public void setUp() {
        // 创建教师用户
        teacher = new User();
        teacher.setUsername("teacher_test");
        teacher.setPassword("password123");
        teacher.setName("测试教师");
        teacher.setRole("TEACHER");
        teacher.setEmail("teacher@test.com");
        teacher = userRepository.save(teacher);

        // 创建学生用户1
        student1 = new User();
        student1.setUsername("student1_test");
        student1.setPassword("password123");
        student1.setName("测试学生1");
        student1.setRole("STUDENT");
        student1.setStudentId("20240001");
        student1.setEmail("student1@test.com");
        student1 = userRepository.save(student1);

        // 创建学生用户2
        student2 = new User();
        student2.setUsername("student2_test");
        student2.setPassword("password123");
        student2.setName("测试学生2");
        student2.setRole("STUDENT");
        student2.setStudentId("20240002");
        student2.setEmail("student2@test.com");
        student2 = userRepository.save(student2);

        // 创建作业
        homework = new Homework();
        homework.setTitle("测试作业删除");
        homework.setDescription("这是一个测试作业，用于测试删除功能");
        homework.setTeacherId(teacher.getId());
        homework.setCourseName("软件工程");
        homework.setDeadline(new Date(System.currentTimeMillis() + 86400000)); // 1天后
        homework.setStatus("published");
        homework.setMaxScore(100.0);
        homework.setAllowModification(true);
        homework = homeworkRepository.save(homework);
    }

    /**
     * 测试用例1：删除已分配给学生的作业
     * 
     * 预期行为：
     * 1. 作业成功删除
     * 2. 学生收到作业删除通知
     * 3. 通知消息的 notificationId 应正确设置为被删除作业的ID
     * 
     * 潜在问题：
     * 如果 homework.getId() 在删除后返回 null，将导致 NullPointerException
     */
    @Test
    public void testDeleteHomeworkWithAssignedStudents() {
        // 记录作业ID（用于后续验证通知）
        Long homeworkIdBeforeDelete = homework.getId();
        assertNotNull(homeworkIdBeforeDelete, "作业ID不应为空");

        // 执行删除操作
        assertDoesNotThrow(() -> {
            homeworkService.deleteHomework(homework.getId());
        }, "删除作业不应抛出异常");

        // 验证作业已被删除
        assertFalse(homeworkRepository.existsById(homeworkIdBeforeDelete), 
            "作业应已被删除");

        // 验证删除通知已发送给学生
        List<Message> deleteMessages = messageRepository.findByTypeAndNotificationId(
            MessageType.HOMEWORK_DELETE.getCode(), 
            homeworkIdBeforeDelete.toString()
        );

        assertEquals(2, deleteMessages.size(), 
            "应为每个学生创建一条删除通知");

        // 验证通知消息的内容
        for (Message message : deleteMessages) {
            assertEquals("作业", message.getTitle());
            assertTrue(message.getContent().contains("测试作业删除"));
            assertEquals(MessageType.HOMEWORK_DELETE.getCode(), message.getType());
            assertEquals(teacher.getId(), message.getSenderId());
            assertTrue(
                message.getReceiverId().equals(student1.getId()) || 
                message.getReceiverId().equals(student2.getId()),
                "通知应发送给学生"
            );
            assertEquals("active", message.getStatus());
            assertFalse(message.getIsRead());
            
            // 关键验证点：notificationId 应正确设置
            assertEquals(
                homeworkIdBeforeDelete.toString(), 
                message.getNotificationId(),
                "通知消息的 notificationId 应正确设置为被删除作业的ID"
            );
        }
    }

    /**
     * 测试用例2：删除未分配学生的作业
     * 
     * 预期行为：即使没有学生，删除作业也不应抛出异常
     */
    @Test
    public void testDeleteHomeworkWithoutAssignedStudents() {
        // 确保作业没有分配给任何学生
        //（通过不调用分配方法即可）
        
        // 记录作业ID
        Long homeworkIdBeforeDelete = homework.getId();
        assertNotNull(homeworkIdBeforeDelete);

        // 执行删除操作
        assertDoesNotThrow(() -> {
            homeworkService.deleteHomework(homework.getId());
        }, "删除未分配学生的作业不应抛出异常");

        // 验证作业已被删除
        assertFalse(homeworkRepository.existsById(homeworkIdBeforeDelete),
            "作业应已被删除");
    }

    /**
     * 测试用例3：验证删除后 homework.getId() 的状态
     * 
     * 这是一个专门验证 bug 的测试，用于确认删除后对象的 ID 是否变为 null
     */
    @Test
    public void testHomeworkIdAfterDelete() {
        // 记录原始ID
        Long originalId = homework.getId();
        assertNotNull(originalId);

        // 执行删除
        homeworkService.deleteHomework(homework.getId());

        // 关键检查：删除后 homework 对象的 ID 是否仍然有效
        // 在某些 JPA 实现中，删除后实体的 ID 可能变为 null
        // 这会导致 sendHomeworkDeleteNotification 中的 homework.getId().toString() 抛出 NullPointerException
        
        // 注意：由于 @Transactional 的特性，测试完成后事务会回滚，
        // 所以这里实际上无法直接观察到删除后的状态
        // 但这个测试可以帮助我们理解问题的根本原因
    }
}
