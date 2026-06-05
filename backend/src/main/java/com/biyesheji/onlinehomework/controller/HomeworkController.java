package com.biyesheji.onlinehomework.controller;

import com.biyesheji.onlinehomework.dto.ApiResponse;
import com.biyesheji.onlinehomework.dto.StudentHomeworkDTO;
import com.biyesheji.onlinehomework.exception.ResourceNotFoundException;
import com.biyesheji.onlinehomework.model.Homework;
import com.biyesheji.onlinehomework.model.HomeworkStudent;
import com.biyesheji.onlinehomework.service.HomeworkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 作业管理控制器
 *
 * 描述：处理作业相关的所有接口，包括作业的创建、查询、修改、删除、分配学生等
 * 教师可以管理自己的作业，管理员可以管理所有作业
 *
 * @Tag: Swagger API 文档注解
 * @RestController: 标记为 REST 控制器
 * @RequestMapping: 类级基础路径，所有接口以 /homework 开头
 * @SecurityRequirement: 需要 JWT 认证
 */
@Tag(name = "作业管理", description = "作业发布、编辑、删除等相关接口")
@RestController
@RequestMapping("/homework")
@SecurityRequirement(name = "bearerAuth")
public class HomeworkController {

    /**
     * 作业服务
     * 处理作业相关的业务逻辑
     * @Autowired: 自动注入 Spring 容器中的 HomeworkService Bean
     */
    @Autowired
    private HomeworkService homeworkService;

    /**
     * 创建作业接口
     * 教师和管理员可调用
     *
     * 功能描述：
     * 1. 接收作业信息（标题、描述、截止时间、课程名等）
     * 2. 获取当前教师的用户ID
     * 3. 处理各种格式的日期字段
     * 4. 分配作业给学生（可选）
     * 5. 创建作业并发送通知
     *
     * @param request 包含作业信息的 Map 对象
     * @return 创建成功的作业信息
     */
    @Operation(summary = "创建作业", description = "教师创建新的作业")
    @PostMapping
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Homework>> createHomework(@Valid @RequestBody Map<String, Object> request) {
        // 获取当前登录用户信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> principal = (Map<String, Object>) authentication.getPrincipal();
        String username = (String) principal.get("username");

        // 提取作业信息
        Homework homework = new Homework();
        homework.setTitle((String) request.get("title"));
        homework.setCourseName((String) request.get("courseName"));
        homework.setDescription((String) request.get("description"));

        // 处理日期字段 - 支持多种格式
        Object deadlineObj = request.get("deadline");
        if (deadlineObj != null) {
            if (deadlineObj instanceof String) {
                try {
                    String dateStr = (String) deadlineObj;
                    if (dateStr.contains("T")) {
                        // 处理 ISO 格式：2026-04-13T12:00:00 或 2026-04-13T12:00:00.000Z
                        java.time.LocalDateTime localDateTime;
                        if (dateStr.contains("Z")) {
                            java.time.ZonedDateTime zonedDateTime = java.time.ZonedDateTime.parse(dateStr);
                            localDateTime = zonedDateTime.toLocalDateTime();
                        } else {
                            localDateTime = java.time.LocalDateTime.parse(dateStr);
                        }
                        homework.setDeadline(java.sql.Timestamp.valueOf(localDateTime));
                    } else {
                        homework.setDeadline(new Date(dateStr));
                    }
                } catch (Exception e) {
                    System.err.println("日期解析错误: " + e.getMessage());
                }
            } else if (deadlineObj instanceof Date) {
                homework.setDeadline((Date) deadlineObj);
            } else if (deadlineObj instanceof Map) {
                Map<?, ?> dateMap = (Map<?, ?>) deadlineObj;
                Object timestampObj = dateMap.get("timestamp");
                if (timestampObj instanceof Number) {
                    homework.setDeadline(new Date(((Number) timestampObj).longValue()));
                }
            } else if (deadlineObj instanceof Long) {
                homework.setDeadline(new Date((Long) deadlineObj));
            } else if (deadlineObj instanceof Integer) {
                homework.setDeadline(new Date(((Integer) deadlineObj).longValue()));
            }
        }

        // 处理数字字段
        Object maxScoreObj = request.get("maxScore");
        if (maxScoreObj != null) {
            if (maxScoreObj instanceof Number) {
                homework.setMaxScore(((Number) maxScoreObj).doubleValue());
            } else if (maxScoreObj instanceof String) {
                try {
                    homework.setMaxScore(Double.parseDouble((String) maxScoreObj));
                } catch (Exception e) {
                    // 忽略解析错误
                }
            }
        }

        // 处理布尔字段
        Object allowModificationObj = request.get("allowModification");
        if (allowModificationObj != null) {
            if (allowModificationObj instanceof Boolean) {
                homework.setAllowModification((Boolean) allowModificationObj);
            } else if (allowModificationObj instanceof String) {
                homework.setAllowModification(Boolean.parseBoolean((String) allowModificationObj));
            }
        }

        // 处理状态字段
        Object statusObj = request.get("status");
        if (statusObj != null) {
            homework.setStatus((String) statusObj);
        } else {
            homework.setStatus("published");
        }

        // 设置教师ID
        homework.setTeacherId(homeworkService.findByUsername(username).getId());

        // 提取学生ID列表
        List<Long> studentIds = null;
        Object studentIdsObj = request.get("studentIds");
        if (studentIdsObj instanceof List) {
            List<?> ids = (List<?>) studentIdsObj;
            studentIds = ids.stream()
                .map(id -> {
                    if (id instanceof Integer) {
                        return ((Integer) id).longValue();
                    } else if (id instanceof Long) {
                        return (Long) id;
                    } else if (id instanceof String) {
                        try {
                            return Long.parseLong((String) id);
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    }
                    return null;
                })
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toList());
        }

        // 创建作业
        Homework createdHomework = homeworkService.createHomework(homework, studentIds);

        return ResponseEntity.ok(ApiResponse.success("作业创建成功", createdHomework));
    }

    /**
     * 获取作业详情接口
     *
     * @param id 作业ID
     * @return 作业详细信息
     */
    @Operation(summary = "获取作业详情", description = "根据作业 ID 获取详细信息")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Homework>> getHomeworkById(@PathVariable Long id) {
        Homework homework = homeworkService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("作业", "id", id));
        homeworkService.enrichWithTeacherName(homework);
        return ResponseEntity.ok(ApiResponse.success(homework));
    }

    /**
     * 获取教师的所有作业接口
     * 教师和管理员可调用
     *
     * @param courseName 课程名称（可选，用于筛选）
     * @param status 作业状态（可选，用于筛选）
     * @return 该教师发布的所有作业列表
     */
    @Operation(summary = "获取教师的所有作业", description = "获取当前教师发布的所有作业")
    @GetMapping("/teacher/all")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<Homework>>> getHomeworkByTeacher(
            @RequestParam(required = false) String courseName,
            @RequestParam(required = false) String status) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> principal = (Map<String, Object>) authentication.getPrincipal();
        String username = (String) principal.get("username");
        Long teacherId = homeworkService.findByUsername(username).getId();

        List<Homework> homeworkList = homeworkService.findByTeacherId(teacherId);

        // 应用筛选条件
        if (courseName != null && !courseName.isEmpty()) {
            homeworkList = homeworkList.stream()
                    .filter(h -> h.getCourseName() != null && h.getCourseName().contains(courseName))
                    .collect(java.util.stream.Collectors.toList());
        }

        if (status != null && !status.isEmpty() && !"all".equals(status)) {
            homeworkList = homeworkList.stream()
                    .filter(h -> h.getStatus() != null && h.getStatus().equals(status))
                    .collect(java.util.stream.Collectors.toList());
        }

        return ResponseEntity.ok(ApiResponse.success(homeworkList));
    }

    /**
     * 获取学生的作业列表接口
     * 仅学生可调用
     * 返回包含提交状态的作业列表
     *
     * @return 该学生被分配的所有作业（包含提交状态）
     */
    @Operation(summary = "获取学生的作业列表", description = "获取当前学生的所有作业（包含提交状态）")
    @GetMapping("/student/all")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<StudentHomeworkDTO>>> getHomeworkByStudent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> principal = (Map<String, Object>) authentication.getPrincipal();
        String username = (String) principal.get("username");
        Long studentId = homeworkService.findByUsername(username).getId();

        List<StudentHomeworkDTO> homeworkList = homeworkService.findHomeworkByStudentIdWithStatus(studentId);
        return ResponseEntity.ok(ApiResponse.success(homeworkList));
    }

    /**
     * 获取所有作业接口
     * 仅管理员可调用
     *
     * @return 所有作业列表
     */
    @Operation(summary = "获取所有作业", description = "获取所有已发布的作业（管理员）")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Homework>>> getAllHomework() {
        List<Homework> homeworkList = homeworkService.getAllHomework();
        return ResponseEntity.ok(ApiResponse.success(homeworkList));
    }

    /**
     * 按状态查询作业接口
     *
     * @param status 作业状态
     * @return 符合状态的作业列表
     */
    @Operation(summary = "按状态查询作业", description = "根据作业状态查询作业列表")
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<Homework>>> getHomeworkByStatus(@PathVariable String status) {
        List<Homework> homeworkList = homeworkService.findByStatus(status);
        return ResponseEntity.ok(ApiResponse.success(homeworkList));
    }

    /**
     * 更新作业接口
     * 教师和管理员可调用
     *
     * @param id 要更新的作业ID
     * @param homework 包含更新信息的作业对象
     * @return 更新后的作业信息
     */
    @Operation(summary = "更新作业", description = "更新作业信息")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Homework>> updateHomework(@PathVariable Long id,
                                                                 @RequestBody Homework homework) {
        Homework existingHomework = homeworkService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("作业", "id", id));

        // 只更新非空字段
        if (homework.getTitle() != null) {
            existingHomework.setTitle(homework.getTitle());
        }
        if (homework.getCourseName() != null) {
            existingHomework.setCourseName(homework.getCourseName());
        }
        if (homework.getDescription() != null) {
            existingHomework.setDescription(homework.getDescription());
        }
        if (homework.getDeadline() != null) {
            existingHomework.setDeadline(homework.getDeadline());
        }
        if (homework.getMaxScore() != null) {
            existingHomework.setMaxScore(homework.getMaxScore());
        }
        if (homework.getStatus() != null) {
            existingHomework.setStatus(homework.getStatus());
        }
        if (homework.getAllowModification() != null) {
            existingHomework.setAllowModification(homework.getAllowModification());
        }

        Homework updatedHomework = homeworkService.updateHomework(existingHomework);
        return ResponseEntity.ok(ApiResponse.success("作业更新成功", updatedHomework));
    }

    /**
     * 删除作业接口
     * 教师和管理员可调用
     *
     * @param id 要删除的作业ID
     * @return 删除成功响应
     */
    @Operation(summary = "删除作业", description = "删除指定作业")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> deleteHomework(@PathVariable Long id) {
        homeworkService.deleteHomework(id);
        return ResponseEntity.ok(ApiResponse.success("作业删除成功", null));
    }

    /**
     * 获取作业的学生ID列表接口
     *
     * @param id 作业ID
     * @return 被分配该作业的学生ID列表
     */
    @Operation(summary = "获取作业的学生 ID 列表", description = "获取作业分配给的所有学生 ID")
    @GetMapping("/{id}/students")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<Long>>> getStudentIdsByHomeworkId(@PathVariable Long id) {
        List<Long> studentIds = homeworkService.getStudentIdsByHomeworkId(id);
        return ResponseEntity.ok(ApiResponse.success(studentIds));
    }

    /**
     * 更新作业学生列表接口
     * 修改作业分配的学生名单
     *
     * @param id 作业ID
     * @param studentIds 新的学生ID列表
     * @return 更新成功响应
     */
    @Operation(summary = "更新作业学生列表", description = "更新作业分配的学生")
    @PutMapping("/{id}/students")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<String>> updateHomeworkStudents(@PathVariable Long id,
                                                                       @RequestBody List<Long> studentIds) {
        homeworkService.updateHomeworkStudents(id, studentIds);
        return ResponseEntity.ok(ApiResponse.success("学生列表更新成功", null));
    }

    /**
     * 获取有过提交的作业接口
     * 只返回有学生提交的作业
     *
     * @return 有学生提交的作业列表
     */
    @Operation(summary = "获取有过提交的作业", description = "获取教师发布的有学生提交的作业")
    @GetMapping("/teacher/with-submissions")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<Homework>>> getHomeworkWithSubmissions() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> principal = (Map<String, Object>) authentication.getPrincipal();
        String username = (String) principal.get("username");
        Long teacherId = homeworkService.findByUsername(username).getId();

        List<Homework> homeworkList = homeworkService.findHomeworkWithSubmissionsByTeacherId(teacherId);
        return ResponseEntity.ok(ApiResponse.success(homeworkList));
    }

    /**
     * 获取过期作业接口
     *
     * @return 已过截止日期的作业列表
     */
    @Operation(summary = "获取过期作业", description = "获取已过截止日期的作业")
    @GetMapping("/expired")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<Homework>>> getExpiredHomework() {
        List<Homework> homeworkList = homeworkService.findExpiredHomework();
        return ResponseEntity.ok(ApiResponse.success(homeworkList));
    }
}
