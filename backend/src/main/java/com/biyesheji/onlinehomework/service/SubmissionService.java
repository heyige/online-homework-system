package com.biyesheji.onlinehomework.service;

import com.biyesheji.onlinehomework.dto.SubmissionDTO;
import com.biyesheji.onlinehomework.enums.MessageType;
import com.biyesheji.onlinehomework.model.Homework;
import com.biyesheji.onlinehomework.model.Message;
import com.biyesheji.onlinehomework.model.Submission;
import com.biyesheji.onlinehomework.model.User;
import com.biyesheji.onlinehomework.repository.HomeworkRepository;
import com.biyesheji.onlinehomework.repository.MessageRepository;
import com.biyesheji.onlinehomework.repository.SubmissionRepository;
import com.biyesheji.onlinehomework.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 作业提交业务逻辑服务类
 *
 * 描述：处理作业提交相关的所有业务逻辑，包括提交作业、批改作业、查询提交、查重检测等
 *
 * @Service: 标记为 Spring 业务逻辑组件
 * @Transactional: 开启事务管理，所有方法均在事务中执行
 */
@Service
@Transactional
public class SubmissionService {

    /**
     * 提交数据访问接口
     * @Autowired: 自动注入 Spring 容器中的 SubmissionRepository Bean
     */
    @Autowired
    private SubmissionRepository submissionRepository;

    /**
     * 作业数据访问接口
     */
    @Autowired
    private HomeworkRepository homeworkRepository;

    /**
     * 消息数据访问接口
     */
    @Autowired
    private MessageRepository messageRepository;

    /**
     * 用户数据访问接口
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * 创建作业提交记录
     *
     * @param submission 提交实体（包含作业ID、学生ID、提交内容、附件等）
     * @return 创建成功的提交实体
     */
    @CacheEvict(value = "homeworkByStudent", key = "#submission.studentId")
    public Submission createSubmission(Submission submission) {
        // 设置初始状态为"已提交"
        submission.setStatus("submitted");
        return submissionRepository.save(submission);
    }

    /**
     * 根据提交ID查询提交
     *
     * @param id 提交ID
     * @return 包含提交的 Optional 对象
     */
    public Optional<Submission> findById(Long id) {
        return submissionRepository.findById(id);
    }

    /**
     * 根据作业ID查询该作业的所有提交
     *
     * @param homeworkId 作业ID
     * @return 该作业的所有提交列表
     */
    public List<Submission> findByHomeworkId(Long homeworkId) {
        return submissionRepository.findByHomeworkId(homeworkId);
    }

    /**
     * 根据学生ID查询该学生的所有提交
     *
     * @param studentId 学生用户ID
     * @return 该学生的所有提交列表
     */
    public List<Submission> findByStudentId(Long studentId) {
        return submissionRepository.findByStudentId(studentId);
    }

    /**
     * 根据作业ID和学生ID查询特定提交
     * 如果有多个提交，返回最新的一次
     *
     * @param homeworkId 作业ID
     * @param studentId 学生用户ID
     * @return 最新的提交记录，如果没有则返回 null
     */
    public Submission findByHomeworkIdAndStudentId(Long homeworkId, Long studentId) {
        List<Submission> submissions = submissionRepository.findByHomeworkIdAndStudentId(homeworkId, studentId);
        if (submissions.isEmpty()) {
            return null;
        }
        // 返回最新的提交（根据提交时间排序）
        return submissions.stream()
                .max((s1, s2) -> s1.getSubmittedAt().compareTo(s2.getSubmittedAt()))
                .orElse(null);
    }

    /**
     * 根据ID查询提交（包含作业和学生信息）
     * 用于在批改页面展示完整的提交信息
     *
     * @param id 提交ID
     * @return 包含作业和学生信息的 DTO
     */
    public SubmissionDTO findByIdWithHomeworkInfo(Long id) {
        Optional<Submission> submissionOpt = submissionRepository.findById(id);
        if (submissionOpt.isPresent()) {
            // 获取作业信息
            Homework homework = homeworkRepository.findById(submissionOpt.get().getHomeworkId()).orElse(null);
            // 获取学生信息
            User student = userRepository.findById(submissionOpt.get().getStudentId()).orElse(null);
            String studentName = student != null ? student.getName() : null;
            return new SubmissionDTO(submissionOpt.get(), homework, studentName);
        }
        return null;
    }

    /**
     * 根据学生ID查询提交列表（包含作业信息）
     *
     * @param studentId 学生用户ID
     * @return 包含作业信息的提交 DTO 列表
     */
    public List<SubmissionDTO> findByStudentIdWithHomeworkInfo(Long studentId) {
        List<Submission> submissions = submissionRepository.findByStudentId(studentId);
        return submissions.stream().map(submission -> {
            // 获取作业信息
            Homework homework = homeworkRepository.findById(submission.getHomeworkId()).orElse(null);
            // 获取学生信息
            User student = userRepository.findById(submission.getStudentId()).orElse(null);
            String studentName = student != null ? student.getName() : null;
            return new SubmissionDTO(submission, homework, studentName);
        }).collect(Collectors.toList());
    }

    /**
     * 根据作业ID和学生ID查询提交（包含作业信息）
     *
     * @param homeworkId 作业ID
     * @param studentId 学生用户ID
     * @return 包含作业信息的提交 DTO
     */
    public SubmissionDTO findByHomeworkIdAndStudentIdWithHomeworkInfo(Long homeworkId, Long studentId) {
        Submission submission = findByHomeworkIdAndStudentId(homeworkId, studentId);
        if (submission != null) {
            Homework homework = homeworkRepository.findById(homeworkId).orElse(null);
            User student = userRepository.findById(studentId).orElse(null);
            String studentName = student != null ? student.getName() : null;
            return new SubmissionDTO(submission, homework, studentName);
        }
        return null;
    }

    /**
     * 批改作业提交
     *
     * 业务流程：
     * 1. 查询要批改的提交
     * 2. 设置成绩、批语、批改状态和批改时间
     * 3. 保存批改结果
     * 4. 发送批改通知给学生
     *
     * @param id 提交ID
     * @param score 成绩分数
     * @param feedback 教师批语（可选）
     * @return 批改后的提交实体
     */
    @CacheEvict(value = "homeworkByStudent", key = "#result?.studentId")
    public Submission gradeSubmission(Long id, Double score, String feedback) {
        Optional<Submission> optionalSubmission = submissionRepository.findById(id);
        if (optionalSubmission.isPresent()) {
            Submission submission = optionalSubmission.get();
            // 设置成绩
            submission.setScore(score);
            // 设置批语（可选，空则清空）
            if (feedback != null && !feedback.isBlank()) {
                submission.setFeedback(feedback);
            } else {
                submission.setFeedback(null);
            }
            // 设置状态为"已批改"
            submission.setStatus("graded");
            // 设置批改时间
            submission.setGradedAt(new Date());
            // 保存批改结果
            Submission gradedSubmission = submissionRepository.save(submission);

            // 发送批改通知给学生
            sendGradeNotification(gradedSubmission);

            return gradedSubmission;
        }
        return null;
    }

    /**
     * 发送作业批改通知
     * 当教师批改作业后，通知学生查看成绩
     *
     * @param submission 批改后的提交实体
     */
    private void sendGradeNotification(Submission submission) {
        // 获取作业信息
        Homework homework = homeworkRepository.findById(submission.getHomeworkId())
            .orElse(null);

        if (homework == null) {
            return;
        }

        // 创建通知消息
        Message message = new Message();
        message.setTitle("作业批改");
        message.setContent("你的作业已被批改，得分：" + submission.getScore());
        message.setType(MessageType.SUBMISSION_GRADED.getCode());
        message.setReceiverId(submission.getStudentId());
        message.setSenderId(homework.getTeacherId());
        message.setNotificationId(homework.getId().toString());
        message.setStatus("active");
        message.setIsRead(false);

        // 保存消息
        messageRepository.save(message);
    }

    /**
     * 更新提交记录
     * 用于学生修改已提交的作业（如果作业允许修改）
     *
     * @param submission 包含更新信息的提交实体
     * @return 更新后的提交实体
     */
    @CacheEvict(value = "homeworkByStudent", key = "#submission.studentId")
    public Submission updateSubmission(Submission submission) {
        return submissionRepository.save(submission);
    }

    /**
     * 删除提交记录
     *
     * @param id 要删除的提交ID
     */
    public void deleteSubmission(Long id) {
        submissionRepository.deleteById(id);
    }

    /**
     * 根据作业ID和提交状态查询提交列表
     *
     * @param homeworkId 作业ID
     * @param status 提交状态 (submitted/graded)
     * @return 符合条件的所有提交
     */
    public List<Submission> findByHomeworkIdAndStatus(Long homeworkId, String status) {
        return submissionRepository.findByHomeworkIdAndStatus(homeworkId, status);
    }

    /**
     * 作业查重检测
     *
     * 业务流程：
     * 1. 获取该作业的所有提交
     * 2. 对每对提交进行相似度计算
     * 3. 将相似度结果保存到数据库
     * 4. 返回更新后的提交列表
     *
     * 查重算法：使用 TF-IDF 思想，通过词频向量计算余弦相似度
     *
     * @param homeworkId 作业ID
     * @return 更新了相似度分数的提交列表
     */
    public List<Submission> checkPlagiarism(Long homeworkId) {
        // 获取该作业的所有提交
        List<Submission> submissions = submissionRepository.findByHomeworkId(homeworkId);

        // 对每对提交计算相似度
        for (int i = 0; i < submissions.size(); i++) {
            Submission submission1 = submissions.get(i);
            double maxSimilarity = 0.0;  // 与其他提交的最大相似度

            for (int j = 0; j < submissions.size(); j++) {
                if (i != j) {  // 不与自己比较
                    Submission submission2 = submissions.get(j);
                    // 计算两篇提交的相似度
                    double similarity = calculateSimilarity(submission1.getContent(), submission2.getContent());
                    // 更新最大相似度
                    if (similarity > maxSimilarity) {
                        maxSimilarity = similarity;
                    }
                }
            }

            // 保存最大相似度分数
            submission1.setSimilarityScore(maxSimilarity);
            submissionRepository.save(submission1);
        }

        return submissions;
    }

    /**
     * 计算两篇文本的相似度
     * 使用余弦相似度算法
     *
     * @param text1 第一篇文本
     * @param text2 第二篇文本
     * @return 相似度分数（0.0 - 1.0），1.0 表示完全相同
     */
    private double calculateSimilarity(String text1, String text2) {
        // 处理空值情况
        if (text1 == null || text2 == null || text1.isEmpty() || text2.isEmpty()) {
            return 0.0;
        }

        // 分词
        List<String> tokens1 = tokenize(text1);
        List<String> tokens2 = tokenize(text2);

        // 处理空词列表
        if (tokens1.isEmpty() || tokens2.isEmpty()) {
            return 0.0;
        }

        // 构建词频向量
        Map<String, Integer> vector1 = getTermFrequencyFromTokens(tokens1);
        Map<String, Integer> vector2 = getTermFrequencyFromTokens(tokens2);

        // 计算点积
        double dotProduct = 0.0;
        for (String term : vector1.keySet()) {
            if (vector2.containsKey(term)) {
                dotProduct += vector1.get(term) * vector2.get(term);
            }
        }

        // 计算向量模长
        double magnitude1 = 0.0;
        for (int count : vector1.values()) {
            magnitude1 += count * count;
        }
        magnitude1 = Math.sqrt(magnitude1);

        double magnitude2 = 0.0;
        for (int count : vector2.values()) {
            magnitude2 += count * count;
        }
        magnitude2 = Math.sqrt(magnitude2);

        // 避免除零错误
        if (magnitude1 == 0 || magnitude2 == 0) {
            return 0.0;
        }

        // 计算余弦相似度
        return dotProduct / (magnitude1 * magnitude2);
    }

    /**
     * 文本分词
     * 支持中英文混合文本
     *
     * @param text 输入文本
     * @return 分词后的词列表
     */
    private List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();

        // 转换为小写并替换特殊字符为空格
        text = text.toLowerCase().replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", " ");

        // 检测是否包含中文
        if (text.matches(".*[\\u4e00-\\u9fa5].*")) {
            // 中文文本处理 - 使用二元分词
            for (int i = 0; i < text.length(); i++) {
                if (i < text.length() - 1) {
                    String bigram = text.substring(i, i + 2);
                    // 只保留由中文字符组成的二元组
                    if (!bigram.contains(" ") && !bigram.matches(".*[^\\u4e00-\\u9fa5a-zA-Z0-9].*")) {
                        tokens.add(bigram);
                    }
                }
            }
            // 额外添加单词级别的分词
            String[] words = text.split("\\s+");
            for (String word : words) {
                if (!word.isEmpty()) {
                    tokens.add(word);
                }
            }
        } else {
            // 英文文本处理 - 使用单词和双字母组合
            String[] words = text.split("\\s+");
            for (String word : words) {
                if (!word.isEmpty()) {
                    // 添加双字母组合特征（用于捕捉局部相似性）
                    if (word.length() >= 2) {
                        for (int i = 0; i < word.length() - 1; i++) {
                            tokens.add(word.substring(i, i + 2));
                        }
                    }
                    tokens.add(word);
                }
            }
        }

        return tokens;
    }

    /**
     * 从分词结果计算词频
     *
     * @param tokens 分词列表
     * @return 词频 Map，key 为词语，value 为出现次数
     */
    private Map<String, Integer> getTermFrequencyFromTokens(List<String> tokens) {
        Map<String, Integer> termFrequency = new HashMap<>();
        for (String token : tokens) {
            if (!token.isEmpty()) {
                termFrequency.put(token, termFrequency.getOrDefault(token, 0) + 1);
            }
        }
        return termFrequency;
    }
}
