package com.biyesheji.onlinehomework.service;

import com.biyesheji.onlinehomework.dto.PageDTO;
import com.biyesheji.onlinehomework.enums.MessageType;
import com.biyesheji.onlinehomework.model.Homework;
import com.biyesheji.onlinehomework.model.Message;
import com.biyesheji.onlinehomework.model.ProfileRequest;
import com.biyesheji.onlinehomework.model.Submission;
import com.biyesheji.onlinehomework.model.User;
import com.biyesheji.onlinehomework.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 用户业务逻辑服务类
 *
 * 描述：处理用户相关的所有业务逻辑，包括用户注册、查询、修改、删除、密码修改、验证码等
 *
 * @Service: 标记为 Spring 业务逻辑组件
 * @Transactional: 开启事务管理，所有方法均在事务中执行
 */
@Service
@Transactional
public class UserService {

    /**
     * 用户数据访问接口
     * @Autowired: 自动注入 Spring 容器中的 UserRepository Bean
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * 密码加密器
     * 用于密码加密和验证（BCrypt 算法）
     */
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 作业数据访问接口
     */
    @Autowired
    private HomeworkRepository homeworkRepository;

    /**
     * 作业-学生关联数据访问接口
     */
    @Autowired
    private HomeworkStudentRepository homeworkStudentRepository;

    /**
     * 提交数据访问接口
     */
    @Autowired
    private SubmissionRepository submissionRepository;

    /**
     * 消息数据访问接口
     */
    @Autowired
    private MessageRepository messageRepository;

    /**
     * 个人信息申请数据访问接口
     */
    @Autowired
    private ProfileRequestRepository profileRequestRepository;

    /**
     * 验证码存储 Map
     * Key: 邮箱或手机号
     * Value: 验证码和过期时间
     */
    private final Map<String, VerificationCodeEntry> verificationCodes = new HashMap<>();

    /**
     * 创建新用户
     *
     * 功能描述：
     * 1. 检查用户名、邮箱、手机号是否已存在
     * 2. 对密码进行 BCrypt 加密
     * 3. 如果是学生角色且没有学号，自动生成学号
     * 4. 保存用户到数据库
     *
     * @param user 用户实体（包含用户名、密码、姓名、角色等信息）
     * @return 创建成功的用户实体（密码已加密）
     * @throws RuntimeException 如果用户名、邮箱或手机号已存在
     */
    public User createUser(User user) {
        // 检查用户名是否已存在
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new RuntimeException("用户名已存在");
        }
        // 检查邮箱是否已存在（仅当邮箱不为空时检查）
        if (user.getEmail() != null && !user.getEmail().isEmpty() && userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("邮箱已被使用");
        }
        // 检查手机号是否已存在（仅当手机号不为空时检查）
        if (user.getPhone() != null && !user.getPhone().isEmpty() && userRepository.existsByPhone(user.getPhone())) {
            throw new RuntimeException("手机号已被使用");
        }

        // 对密码进行 BCrypt 加密
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 如果是学生角色且没有学号，自动生成学号（格式：年份 + 6位随机数）
        if ("student".equalsIgnoreCase(user.getRole()) && (user.getStudentId() == null || user.getStudentId().isEmpty())) {
            int year = java.time.Year.now().getValue();
            int randomPart = (int) (Math.random() * 900000) + 100000;
            user.setStudentId(year + "" + randomPart);
        }

        return userRepository.save(user);
    }

    /**
     * 根据用户ID查询用户
     *
     * @param id 用户ID
     * @return 用户实体
     * @throws RuntimeException 如果用户不存在
     * @Cacheable: 缓存结果，key 为用户ID，查询后会自动缓存
     */
    @Cacheable(value = "userById", key = "#id", unless = "#result == null")
    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在：" + id));
    }

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户实体
     * @throws RuntimeException 如果用户不存在
     * @Cacheable: 缓存结果，key 为用户名
     */
    @Cacheable(value = "userByUsername", key = "#username", unless = "#result == null")
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("用户不存在：" + username));
    }

    /**
     * 根据邮箱查询用户
     *
     * @param email 电子邮箱
     * @return 包含用户的 Optional 对象
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号码
     * @return 包含用户的 Optional 对象
     */
    public Optional<User> findByPhone(String phone) {
        return userRepository.findByPhone(phone);
    }

    /**
     * 根据角色查询用户列表（不区分大小写）
     *
     * @param role 用户角色 (ADMIN/TEACHER/STUDENT)
     * @return 符合角色的用户列表
     */
    public List<User> findByRole(String role) {
        return userRepository.findByRoleIgnoreCase(role);
    }

    /**
     * 获取所有用户
     *
     * @return 所有用户的列表
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * 分页查询用户
     *
     * @param page 页码（从1开始）
     * @param size 每页记录数
     * @param username 用户名（模糊查询，可为null）
     * @param name 姓名（模糊查询，可为null）
     * @param role 角色（精确查询，可为null）
     * @return 分页结果，包含用户列表和分页信息
     */
    public PageDTO<User> getUsersByPage(int page, int size, String username, String name, String role) {
        // 创建分页参数：页码从0开始，倒序排列
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "id"));
        // 执行多条件分页查询
        Page<User> userPage = userRepository.findByFilters(username, name, role, pageable);
        // 转换为自定义的分页 DTO
        return new PageDTO<>(
            userPage.getContent(),        // 当前页数据
            userPage.getTotalElements(), // 总记录数
            userPage.getTotalPages(),    // 总页数
            userPage.getNumber() + 1,    // 当前页码（从1开始）
            userPage.getSize()           // 每页大小
        );
    }

    /**
     * 更新用户信息
     *
     * @param user 包含更新信息的用户实体
     * @return 更新后的用户实体
     */
    @Caching(evict = {
            @CacheEvict(value = "userById", key = "#user.id"),
            @CacheEvict(value = "userByUsername", allEntries = true)
    })
    public User updateUser(User user) {
        Optional<User> existingUserOptional = userRepository.findById(user.getId());
        if (existingUserOptional.isPresent()) {
            User existingUser = existingUserOptional.get();

            // 只更新非空字段，保留原有数据
            if (user.getRole() != null) {
                existingUser.setRole(user.getRole());
            }
            if (user.getDepartment() != null) {
                existingUser.setDepartment(user.getDepartment());
            }
            if (user.getMajor() != null) {
                existingUser.setMajor(user.getMajor());
            }
            if (user.getPhone() != null) {
                existingUser.setPhone(user.getPhone());
            }
            if (user.getName() != null) {
                existingUser.setName(user.getName());
            }
            if (user.getEmail() != null) {
                existingUser.setEmail(user.getEmail());
            }
            if (user.getStudentId() != null) {
                existingUser.setStudentId(user.getStudentId());
            }
            if (user.getAvatar() != null) {
                existingUser.setAvatar(user.getAvatar().isBlank() ? null : user.getAvatar());
            }

            return userRepository.save(existingUser);
        }
        return null;
    }

    /**
     * 批准个人信息修改申请后，同步更新用户资料并清除缓存。
     */
    @CacheEvict(value = {"userById", "userByUsername"}, allEntries = true)
    public User applyProfileApproval(ProfileRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("用户不存在：" + request.getUserId()));

        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setDepartment(request.getDepartment());
        user.setMajor(request.getMajor());
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar().isBlank() ? null : request.getAvatar());
        }

        return userRepository.save(user);
    }

    /**
     * 删除用户
     * 根据用户角色执行不同的删除逻辑：
     * - 教师：删除其发布的所有作业及作业相关数据
     * - 学生：删除其所有提交、作业关联、消息等
     * - 管理员：删除其发送的消息
     *
     * @param id 要删除的用户ID
     * @throws RuntimeException 如果用户不存在或删除失败
     * @CacheEvict: 清除缓存
     */
    @CacheEvict(value = {"userById", "userByUsername"}, allEntries = true)
    public void deleteUser(Long id) {
        Optional<User> userOptional = userRepository.findById(id);
        if (userOptional.isEmpty()) {
            throw new RuntimeException("用户不存在：" + id);
        }

        User user = userOptional.get();
        String role = user.getRole();

        try {
            // 根据角色执行不同的删除逻辑
            if ("TEACHER".equals(role) || "teacher".equals(role)) {
                deleteTeacherAndRelatedData(user);
            } else if ("STUDENT".equals(role) || "student".equals(role)) {
                deleteStudentAndRelatedData(user);
            } else if ("ADMIN".equals(role) || "admin".equals(role)) {
                deleteAdminRelatedData(user);
            }

            // 最后删除用户本身
            userRepository.deleteById(id);
        } catch (Exception e) {
            throw new RuntimeException("删除用户失败：" + e.getMessage(), e);
        }
    }

    /**
     * 删除教师及其相关数据
     * 包括：教师发布的作业、作业的提交、作业与学生的关联、相关消息
     *
     * @param teacher 教师用户实体
     */
    private void deleteTeacherAndRelatedData(User teacher) {
        // 获取该教师发布的所有作业
        List<Homework> teacherHomeworks = homeworkRepository.findByTeacherId(teacher.getId());

        for (Homework homework : teacherHomeworks) {
            Long homeworkId = homework.getId();

            // 删除作业相关的所有提交
            List<Submission> submissions = submissionRepository.findByHomeworkId(homeworkId);
            for (Submission submission : submissions) {
                submissionRepository.deleteById(submission.getId());
            }

            // 删除作业与学生的关联
            homeworkStudentRepository.deleteByHomeworkId(homeworkId);

            // 删除作业相关的消息
            messageRepository.deleteByNotificationId(homeworkId.toString());

            // 删除作业本身
            homeworkRepository.deleteById(homeworkId);
        }

        // 删除教师发送的消息
        messageRepository.deleteBySenderId(teacher.getId());
    }

    /**
     * 删除学生及其相关数据
     * 包括：学生的所有提交、作业关联、收到的消息、个人信息申请
     *
     * @param student 学生用户实体
     */
    private void deleteStudentAndRelatedData(User student) {
        Long studentId = student.getId();

        // 删除学生的所有提交
        List<Submission> submissions = submissionRepository.findByStudentId(studentId);
        for (Submission submission : submissions) {
            submissionRepository.deleteById(submission.getId());
        }

        // 删除学生与作业的关联
        homeworkStudentRepository.deleteByStudentId(studentId);

        // 删除学生收到的消息
        messageRepository.deleteByReceiverId(studentId);

        // 删除学生的个人信息修改申请
        profileRequestRepository.deleteByUserId(studentId);
    }

    /**
     * 删除管理员相关数据
     * 包括：管理员发送的消息
     *
     * @param admin 管理员用户实体
     */
    private void deleteAdminRelatedData(User admin) {
        messageRepository.deleteBySenderId(admin.getId());
    }

    /**
     * 修改密码
     *
     * @param userId 用户ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 成功返回 "success"，失败返回错误信息
     */
    public String changePassword(Long userId, String oldPassword, String newPassword) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            return "用户不存在";
        }

        User user = userOptional.get();

        // 验证旧密码是否正确
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return "旧密码不正确";
        }

        // 检查新旧密码是否相同
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            return "新旧密码不能一致";
        }

        // 加密并保存新密码
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        return "success";
    }

    /**
     * 生成验证码
     * 用于忘记密码功能
     *
     * @param identifier 标识符（邮箱或手机号）
     * @return 生成的6位数字验证码
     */
    public String generateVerificationCode(String identifier) {
        // 生成6位随机数字验证码
        String code = String.format("%06d", (int) (Math.random() * 999999));
        // 设置过期时间：5分钟后
        long expiryTime = System.currentTimeMillis() + 5 * 60 * 1000;
        // 存储验证码和过期时间
        verificationCodes.put(identifier, new VerificationCodeEntry(code, expiryTime));
        // 打印验证码（生产环境应移除或改为日志记录）
        System.out.println("Verification code for " + identifier + ": " + code);
        return code;
    }

    /**
     * 验证验证码
     *
     * @param identifier 标识符（邮箱或手机号）
     * @param code 用户输入的验证码
     * @return true 验证成功，false 验证失败
     */
    public boolean verifyCode(String identifier, String code) {
        VerificationCodeEntry entry = verificationCodes.get(identifier);
        if (entry == null) {
            return false;
        }

        // 检查验证码是否过期
        if (System.currentTimeMillis() > entry.getExpiryTime()) {
            verificationCodes.remove(identifier);
            return false;
        }

        // 验证验证码是否匹配
        boolean isValid = code != null && code.equals(entry.getCode());
        // 验证成功后删除验证码（一次性使用）
        if (isValid) {
            verificationCodes.remove(identifier);
        }

        return isValid;
    }

    /**
     * 重置密码
     * 通过验证码验证后重置密码
     *
     * @param identifier 标识符（邮箱或手机号）
     * @param newPassword 新密码
     * @return true 重置成功，false 重置失败
     */
    public boolean resetPassword(String identifier, String newPassword) {
        Optional<User> userOptional;
        // 根据标识符类型选择查询方式
        if (identifier.contains("@")) {
            userOptional = findByEmail(identifier);
        } else {
            userOptional = findByPhone(identifier);
        }

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            // 加密并保存新密码
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return true;
        }
        return false;
    }

    /**
     * 验证码条目内部类
     * 用于存储验证码和过期时间
     */
    private static class VerificationCodeEntry {
        private final String code;       // 验证码
        private final long expiryTime;   // 过期时间（时间戳）

        public VerificationCodeEntry(String code, long expiryTime) {
            this.code = code;
            this.expiryTime = expiryTime;
        }

        public String getCode() {
            return code;
        }

        public long getExpiryTime() {
            return expiryTime;
        }
    }
}
