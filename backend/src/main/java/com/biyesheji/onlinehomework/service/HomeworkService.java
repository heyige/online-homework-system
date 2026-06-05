package com.biyesheji.onlinehomework.service;

import com.biyesheji.onlinehomework.dto.StudentHomeworkDTO;
import com.biyesheji.onlinehomework.enums.MessageType;
import com.biyesheji.onlinehomework.model.Homework;
import com.biyesheji.onlinehomework.model.HomeworkStudent;
import com.biyesheji.onlinehomework.model.Message;
import com.biyesheji.onlinehomework.model.Submission;
import com.biyesheji.onlinehomework.model.User;
import com.biyesheji.onlinehomework.repository.HomeworkRepository;
import com.biyesheji.onlinehomework.repository.HomeworkStudentRepository;
import com.biyesheji.onlinehomework.repository.MessageRepository;
import com.biyesheji.onlinehomework.repository.SubmissionRepository;
import com.biyesheji.onlinehomework.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 作业业务逻辑服务类
 *
 * 描述：处理作业相关的所有业务逻辑，包括作业的创建、查询、修改、删除、分配学生、发送通知等
 *
 * @Service: 标记为 Spring 业务逻辑组件
 * @Transactional: 开启事务管理，所有方法均在事务中执行
 */
@Service
@Transactional
public class HomeworkService {

    /**
     * 作业数据访问接口
     * @Autowired: 自动注入 Spring 容器中的 HomeworkRepository Bean
     */
    @Autowired
    private HomeworkRepository homeworkRepository;

    /**
     * 作业-学生关联数据访问接口
     */
    @Autowired
    private HomeworkStudentRepository homeworkStudentRepository;

    /**
     * 提交业务逻辑服务
     */
    @Autowired
    private SubmissionService submissionService;

    /**
     * 用户数据访问接口
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * 消息数据访问接口
     */
    @Autowired
    private MessageRepository messageRepository;

    /**
     * 提交数据访问接口
     */
    @Autowired
    private SubmissionRepository submissionRepository;

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户实体
     * @throws RuntimeException 如果用户不存在
     */
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在：" + username));
    }

    /**
     * 创建作业
     *
     * 业务流程：
     * 1. 保存作业记录到数据库
     * 2. 如果没有指定学生，则获取所有学生；如果指定了学生，则只分配给指定学生
     * 3. 创建作业与学生的关联关系
     * 4. 如果作业状态为"已发布"，发送通知给所有相关学生
     *
     * @param homework 作业实体（包含标题、描述、截止时间等信息）
     * @param studentIds 要分配的学生ID列表（可为null或空，表示分配给所有学生）
     * @return 创建成功的作业实体
     * @CacheEvict: 清除相关缓存
     */
    @CacheEvict(value = {"homework", "homeworkByTeacher", "homeworkByStudent"}, allEntries = true)
    public Homework createHomework(Homework homework, List<Long> studentIds) {
        // 如果没有设置状态，默认设置为已发布
        if (homework.getStatus() == null) {
            homework.setStatus("published");
        }
        // 保存作业到数据库
        Homework savedHomework = homeworkRepository.save(homework);

        // 确定学生ID列表
        List<Long> finalStudentIds = resolveTargetStudentIds(studentIds);

        // 创建作业 - 学生关联
        if (!finalStudentIds.isEmpty()) {
            // 将每个学生与作业创建关联关系
            List<HomeworkStudent> homeworkStudents = finalStudentIds.stream()
                .map(studentId -> {
                    HomeworkStudent hs = new HomeworkStudent();
                    hs.setHomeworkId(savedHomework.getId());
                    hs.setStudentId(studentId);
                    return hs;
                })
                .collect(Collectors.toList());
            // 批量保存关联关系
            homeworkStudentRepository.saveAll(homeworkStudents);

            // 只有当作业状态为已发布时才发送通知
            if ("published".equals(savedHomework.getStatus())) {
                // 发送通知给所有相关学生
                sendHomeworkCreateNotification(savedHomework, finalStudentIds);
            }
        }

        return savedHomework;
    }

    /**
     * 发送作业创建通知
     * 为每个学生创建一条通知消息，告知有新作业发布
     *
     * @param homework 作业实体
     * @param studentIds 要通知的学生ID列表
     */
    private void sendHomeworkCreateNotification(Homework homework, List<Long> studentIds) {
        // 为每个学生创建通知消息
        List<Message> messages = studentIds.stream()
            .map(studentId -> {
                Message message = new Message();
                message.setTitle("作业");
                message.setContent("老师发布了名为" + homework.getTitle() + "的作业");
                message.setType(MessageType.HOMEWORK_CREATE.getCode());
                message.setReceiverId(studentId);
                message.setSenderId(homework.getTeacherId());
                message.setNotificationId(homework.getId().toString());
                message.setStatus("active");
                message.setIsRead(false);
                return message;
            })
            .collect(Collectors.toList());

        // 批量保存消息
        messageRepository.saveAll(messages);
    }

    /**
     * 根据 ID 查询作业
     *
     * @param id 作业ID
     * @return 包含作业的 Optional 对象
     * @Cacheable: 缓存结果
     */
    @Cacheable(value = "homework", key = "#id", unless = "#result == null")
    public Optional<Homework> findById(Long id) {
        return homeworkRepository.findById(id);
    }

    /**
     * 填充作业的教师姓名（非持久化字段）
     *
     * @param homework 作业实体
     * @return 填充后的作业实体
     */
    public Homework enrichWithTeacherName(Homework homework) {
        if (homework == null || homework.getTeacherId() == null) {
            return homework;
        }
        userRepository.findById(homework.getTeacherId()).ifPresent(teacher -> {
            String name = teacher.getName();
            homework.setTeacherName(name != null && !name.isBlank() ? name : teacher.getUsername());
        });
        return homework;
    }

    /**
     * 根据教师 ID 查询该教师发布的所有作业
     *
     * @param teacherId 教师用户ID
     * @return 该教师发布的所有作业列表
     * @Cacheable: 缓存结果
     */
    @Cacheable(value = "homeworkByTeacher", key = "#teacherId")
    public List<Homework> findByTeacherId(Long teacherId) {
        return homeworkRepository.findByTeacherId(teacherId);
    }

    /**
     * 根据状态查询作业列表
     *
     * @param status 作业状态 (draft/published/closed)
     * @return 符合状态的作业列表
     */
    public List<Homework> findByStatus(String status) {
        return homeworkRepository.findByStatus(status);
    }

    /**
     * 更新作业
     *
     * 业务流程：
     * 1. 保存更新后的作业信息
     * 2. 发送更新通知给相关学生
     *
     * @param homework 包含更新信息的作业实体
     * @return 更新后的作业实体
     * @CacheEvict: 清除相关缓存
     */
    @CacheEvict(value = {"homework", "homeworkByTeacher", "homeworkByStudent"}, allEntries = true)
    public Homework updateHomework(Homework homework) {
        // 保存更新后的作业
        Homework updatedHomework = homeworkRepository.save(homework);

        // 发送更新通知给相关学生
        List<Long> studentIds = getStudentIdsByHomeworkId(homework.getId());
        if (!studentIds.isEmpty()) {
            sendHomeworkUpdateNotification(updatedHomework, studentIds);
        }

        return updatedHomework;
    }

    /**
     * 发送作业更新通知
     *
     * @param homework 作业实体
     * @param studentIds 要通知的学生ID列表
     */
    private void sendHomeworkUpdateNotification(Homework homework, List<Long> studentIds) {
        List<Message> messages = studentIds.stream()
            .map(studentId -> {
                Message message = new Message();
                message.setTitle("作业");
                message.setContent("老师更新了名为" + homework.getTitle() + "的作业");
                message.setType(MessageType.HOMEWORK_UPDATE.getCode());
                message.setReceiverId(studentId);
                message.setSenderId(homework.getTeacherId());
                message.setNotificationId(homework.getId().toString());
                message.setStatus("active");
                message.setIsRead(false);
                return message;
            })
            .collect(Collectors.toList());

        messageRepository.saveAll(messages);
    }

    /**
     * 删除作业
     *
     * 业务流程：
     * 1. 查询要删除的作业
     * 2. 获取相关学生ID列表（用于发送通知）
     * 3. 删除该作业的所有提交记录
     * 4. 删除作业与学生的关联关系
     * 5. 删除相关通知消息
     * 6. 删除作业本身
     * 7. 发送删除通知给相关学生
     *
     * @param id 要删除的作业ID
     * @throws RuntimeException 如果作业不存在
     * @CacheEvict: 清除相关缓存
     */
    @Transactional
    @CacheEvict(value = {"homework", "homeworkByTeacher", "homeworkByStudent"}, allEntries = true)
    public void deleteHomework(Long id) {
        // 查询作业
        Homework homework = homeworkRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("作业不存在：" + id));

        // 提前保存作业信息，避免删除后无法访问
        String homeworkTitle = homework.getTitle();
        Long teacherId = homework.getTeacherId();

        // 获取相关学生 ID 列表（用于发送通知）
        List<Long> studentIds = getStudentIdsByHomeworkId(id);

        // 删除该作业的所有提交记录
        List<Submission> submissions = submissionRepository.findByHomeworkId(id);
        for (Submission submission : submissions) {
            submissionRepository.deleteById(submission.getId());
        }

        // 删除作业与学生的关联关系
        homeworkStudentRepository.deleteByHomeworkId(id);

        // 删除与该作业相关的所有通知（创建、更新、批改等）
        messageRepository.deleteByNotificationId(id.toString());

        // 删除作业
        homeworkRepository.deleteById(id);

        // 发送删除通知给相关学生（使用提前保存的作业信息）
        sendHomeworkDeleteNotification(id, homeworkTitle, teacherId, studentIds);
    }

    /**
     * 发送作业删除通知
     *
     * @param homeworkId 作业ID
     * @param homeworkTitle 作业标题
     * @param teacherId 教师ID
     * @param studentIds 要通知的学生ID列表
     */
    private void sendHomeworkDeleteNotification(Long homeworkId, String homeworkTitle, Long teacherId, List<Long> studentIds) {
        List<Message> messages = studentIds.stream()
            .map(studentId -> {
                Message message = new Message();
                message.setTitle("作业");
                message.setContent("老师删除了名为" + homeworkTitle + "的作业");
                message.setType(MessageType.HOMEWORK_DELETE.getCode());
                message.setReceiverId(studentId);
                message.setSenderId(teacherId);
                message.setNotificationId(homeworkId.toString());
                message.setStatus("active");
                message.setIsRead(false);
                return message;
            })
            .collect(Collectors.toList());

        messageRepository.saveAll(messages);
    }

    /**
     * 获取所有作业
     *
     * @return 所有作业的列表
     */
    public List<Homework> getAllHomework() {
        return homeworkRepository.findAll();
    }

    /**
     * 查询已过期的作业
     * 用于查找截止时间早于当前时间的作业
     *
     * @return 已过期作业的列表
     */
    public List<Homework> findExpiredHomework() {
        return homeworkRepository.findByDeadlineBefore(new Date());
    }

    /**
     * 获取学生的作业列表（包含提交状态）
     *
     * 业务流程：
     * 1. 从 homework_student 表查询学生被布置的所有作业 ID
     * 2. 查询作业详情列表
     * 3. 为每个作业查询 submission 表，获取提交记录
     * 4. 根据提交记录设置提交状态（未提交/已提交/已批改）
     * 5. 查询教师信息，设置教师姓名
     *
     * @param studentId 学生用户ID
     * @return 包含作业信息和提交状态的 DTO 列表
     * @Cacheable: 缓存结果
     */
    @Cacheable(value = "homeworkByStudent", key = "#studentId")
    public List<StudentHomeworkDTO> findHomeworkByStudentIdWithStatus(Long studentId) {
        // 步骤 1：从 homework_student 表查询该学生被分配的所有作业
        List<HomeworkStudent> homeworkStudents = homeworkStudentRepository.findByStudentId(studentId);
        // 提取作业ID列表
        List<Long> homeworkIds = homeworkStudents.stream()
                .map(HomeworkStudent::getHomeworkId)
                .collect(Collectors.toList());

        // 步骤 2：查询作业详情
        List<Homework> homeworkList = homeworkRepository.findAllById(homeworkIds);

        // 步骤 3 & 4 & 5：为每个作业查询提交记录、教师信息并设置状态
        List<StudentHomeworkDTO> result = new ArrayList<>();
        for (Homework homework : homeworkList) {
            // 查询该学生对该作业的提交记录
            List<Submission> submissions = submissionRepository.findByHomeworkIdAndStudentId(homework.getId(), studentId);
            Submission submission = null;
            if (!submissions.isEmpty()) {
                // 返回最新的提交记录
                submission = submissions.stream()
                        .max((s1, s2) -> s1.getSubmittedAt().compareTo(s2.getSubmittedAt()))
                        .orElse(null);
            }

            // 查询教师信息
            User teacher = userRepository.findById(homework.getTeacherId()).orElse(null);

            // 创建 DTO 并设置提交状态和教师姓名
            StudentHomeworkDTO dto = new StudentHomeworkDTO(homework, submission, teacher);
            result.add(dto);
        }

        return result;
    }

    /**
     * 获取学生的作业列表（旧方法，保留兼容性）
     * 仅返回作业列表，不包含提交状态
     *
     * @param studentId 学生用户ID
     * @return 作业列表
     * @Cacheable: 缓存结果
     */
    @Cacheable(value = "homeworkByStudent", key = "#studentId")
    public List<Homework> findHomeworkByStudentId(Long studentId) {
        // 查询该学生被分配的所有作业
        List<HomeworkStudent> homeworkStudents = homeworkStudentRepository.findByStudentId(studentId);
        List<Long> homeworkIds = homeworkStudents.stream()
                .map(HomeworkStudent::getHomeworkId)
                .collect(Collectors.toList());

        return homeworkRepository.findAllById(homeworkIds);
    }

    /**
     * 获取老师的作业列表，只包含有学生提交的作业
     * 用于教师查看哪些作业有学生提交
     *
     * @param teacherId 教师用户ID
     * @return 有学生提交的作业列表
     */
    public List<Homework> findHomeworkWithSubmissionsByTeacherId(Long teacherId) {
        // 获取该教师发布的所有作业
        List<Homework> allHomework = homeworkRepository.findByTeacherId(teacherId);

        // 过滤出有学生提交的作业
        return allHomework.stream()
                .filter(homework -> {
                    // 查询该作业的提交记录
                    List<Submission> submissions = submissionService.findByHomeworkId(homework.getId());
                    return !submissions.isEmpty();  // 只保留有提交的作业
                })
                .collect(Collectors.toList());
    }

    /**
     * 获取作业相关的学生 ID 列表
     *
     * @param homeworkId 作业ID
     * @return 被分配该作业的学生ID列表
     */
    public List<Long> getStudentIdsByHomeworkId(Long homeworkId) {
        List<HomeworkStudent> homeworkStudents = homeworkStudentRepository.findByHomeworkId(homeworkId);
        return homeworkStudents.stream()
                .map(HomeworkStudent::getStudentId)
                .collect(Collectors.toList());
    }

    /**
     * 更新作业与学生的关联关系
     * 用于修改作业分配的学生名单
     *
     * @param homeworkId 作业ID
     * @param studentIds 新的学生ID列表（null 或空表示分配给所有学生）
     */
    @CacheEvict(value = {"homework", "homeworkByTeacher", "homeworkByStudent"}, allEntries = true)
    public void updateHomeworkStudents(Long homeworkId, List<Long> studentIds) {
        List<Long> previousStudentIds = getStudentIdsByHomeworkId(homeworkId);

        // 先删除原有的关联关系
        homeworkStudentRepository.deleteByHomeworkId(homeworkId);

        List<Long> finalStudentIds = resolveTargetStudentIds(studentIds);
        if (!finalStudentIds.isEmpty()) {
            List<HomeworkStudent> homeworkStudents = finalStudentIds.stream()
                .map(studentId -> {
                    HomeworkStudent hs = new HomeworkStudent();
                    hs.setHomeworkId(homeworkId);
                    hs.setStudentId(studentId);
                    return hs;
                })
                .collect(Collectors.toList());
            homeworkStudentRepository.saveAll(homeworkStudents);
        }

        List<Long> newlyAddedStudentIds = finalStudentIds.stream()
            .filter(id -> !previousStudentIds.contains(id))
            .collect(Collectors.toList());
        if (!newlyAddedStudentIds.isEmpty()) {
            homeworkRepository.findById(homeworkId).ifPresent(homework -> {
                if ("published".equals(homework.getStatus())) {
                    sendHomeworkCreateNotification(homework, newlyAddedStudentIds);
                }
            });
        }
    }

    /**
     * 解析作业目标学生列表
     * 未指定学生时，默认分配给系统中的所有学生
     *
     * @param studentIds 指定的学生ID列表
     * @return 最终要分配的学生ID列表
     */
    private List<Long> resolveTargetStudentIds(List<Long> studentIds) {
        if (studentIds != null && !studentIds.isEmpty()) {
            return studentIds;
        }
        return userRepository.findByRoleIgnoreCase("STUDENT").stream()
            .map(User::getId)
            .collect(Collectors.toList());
    }
}
