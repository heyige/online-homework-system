package com.biyesheji.onlinehomework.controller;

import com.biyesheji.onlinehomework.dto.ApiResponse;
import com.biyesheji.onlinehomework.dto.SubmissionDTO;
import com.biyesheji.onlinehomework.exception.ResourceNotFoundException;
import com.biyesheji.onlinehomework.model.Homework;
import com.biyesheji.onlinehomework.model.Submission;
import com.biyesheji.onlinehomework.model.User;
import com.biyesheji.onlinehomework.repository.UserRepository;
import com.biyesheji.onlinehomework.service.HomeworkService;
import com.biyesheji.onlinehomework.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.biyesheji.onlinehomework.util.SubmissionAttachmentValidator;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "提交管理", description = "作业提交、批改相关接口")
@RestController
@RequestMapping("/submissions")
@SecurityRequirement(name = "bearerAuth")
public class SubmissionController {
    
    @Autowired
    private SubmissionService submissionService;
    
    @Autowired
    private HomeworkService homeworkService;
    
    @Autowired
    private UserRepository userRepository;
    
    private final String uploadDir;
    
    public SubmissionController() {
        // Use absolute path based on the application's base directory
        // Ensure the directory exists
        this.uploadDir = System.getProperty("user.dir") + "/uploads/submissions/";
        Path uploadPath = Paths.get(uploadDir);
        try {
            Files.createDirectories(uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create upload directory", e);
        }
    }
    
    private String saveSubmissionFile(Submission submission, MultipartFile file) throws IOException {
        String validationError = SubmissionAttachmentValidator.validate(file);
        if (validationError != null) {
            return validationError;
        }

        if (submission.getFilePath() != null) {
            Path oldFilePath = Paths.get(submission.getFilePath());
            try {
                Files.deleteIfExists(oldFilePath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        String storedFileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();
        Path filePath = Paths.get(uploadDir, storedFileName);
        Files.createDirectories(filePath.getParent());
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        submission.setFilePath(filePath.toString());
        submission.setFileName(file.getOriginalFilename());
        return null;
    }
    
    @Operation(summary = "提交作业", description = "学生提交作业")
    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Submission>> submitHomework(@RequestParam Long homeworkId,
                                                                   @RequestParam(required = false) String content,
                                                                   @RequestParam(required = false) MultipartFile file) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> principal = (Map<String, Object>) authentication.getPrincipal();
        String username = (String) principal.get("username");
        Long studentId = homeworkService.findByUsername(username).getId();
        
        Homework homework = homeworkService.findById(homeworkId)
                .orElseThrow(() -> new ResourceNotFoundException("作业", "id", homeworkId));
        
        if (homework.getDeadline() != null && new Date().after(homework.getDeadline())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("已超过提交截止时间"));
        }
        
        Submission existingSubmission = submissionService.findByHomeworkIdAndStudentId(homeworkId, studentId);
        
        if (existingSubmission != null) {
            if ("graded".equals(existingSubmission.getStatus())) {
                return ResponseEntity.badRequest().body(ApiResponse.error("已批改的作业不能修改"));
            }
            if (!Boolean.TRUE.equals(homework.getAllowModification())) {
                return ResponseEntity.badRequest().body(ApiResponse.error("该作业不允许修改重新提交"));
            }
        }
        
        Submission submission;
        if (existingSubmission != null) {
            // 更新现有提交
            submission = existingSubmission;
            submission.setContent(content);
            
            // 如果有新文件，删除旧文件并上传新文件
            if (file != null && !file.isEmpty()) {
                try {
                    String validationError = saveSubmissionFile(submission, file);
                    if (validationError != null) {
                        return ResponseEntity.badRequest().body(ApiResponse.error(validationError));
                    }
                } catch (IOException e) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("文件上传失败"));
                }
            }
            
            // 更新提交
            Submission savedSubmission = submissionService.updateSubmission(submission);
            return ResponseEntity.ok(ApiResponse.success("提交成功", savedSubmission));
        } else {
            // 创建新提交
            submission = new Submission();
            submission.setHomeworkId(homeworkId);
            submission.setStudentId(studentId);
            submission.setContent(content);
            
            if (file != null && !file.isEmpty()) {
                try {
                    String validationError = saveSubmissionFile(submission, file);
                    if (validationError != null) {
                        return ResponseEntity.badRequest().body(ApiResponse.error(validationError));
                    }
                } catch (IOException e) {
                    return ResponseEntity.badRequest().body(ApiResponse.error("文件上传失败"));
                }
            }
            
            Submission savedSubmission = submissionService.createSubmission(submission);
            return ResponseEntity.ok(ApiResponse.success("提交成功", savedSubmission));
        }
    }
    
    @Operation(summary = "获取作业提交", description = "根据 ID 获取提交详情")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SubmissionDTO>> getSubmissionById(@PathVariable Long id) {
        SubmissionDTO submission = submissionService.findByIdWithHomeworkInfo(id);
        if (submission == null) {
            throw new ResourceNotFoundException("提交", "id", id);
        }
        return ResponseEntity.ok(ApiResponse.success(submission));
    }

    @Operation(summary = "获取学生的所有提交", description = "获取当前学生的所有提交记录")
    @GetMapping("/student/all")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<SubmissionDTO>>> getSubmissionsByStudent() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> principal = (Map<String, Object>) authentication.getPrincipal();
        String username = (String) principal.get("username");
        Long studentId = homeworkService.findByUsername(username).getId();

        List<SubmissionDTO> submissions = submissionService.findByStudentIdWithHomeworkInfo(studentId);
        return ResponseEntity.ok(ApiResponse.success(submissions));
    }
    
    @Operation(summary = "获取作业的所有提交", description = "获取指定作业的所有提交")
    @GetMapping("/homework/{homeworkId}")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<SubmissionDTO>>> getSubmissionsByHomework(@PathVariable Long homeworkId) {
        List<Submission> submissions = submissionService.findByHomeworkId(homeworkId);
        Homework homework = homeworkService.findById(homeworkId).orElse(null);
        List<SubmissionDTO> dtos = submissions.stream().map(submission -> {
            User student = null;
            try {
                student = userRepository.findById(submission.getStudentId()).orElse(null);
            } catch (Exception e) {
            }
            String studentName = student != null ? student.getName() : null;
            return new SubmissionDTO(submission, homework, studentName);
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }
    
    @Operation(summary = "学生获取指定作业的提交", description = "学生获取自己在指定作业的提交记录")
    @GetMapping("/homework/{homeworkId}/student")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<SubmissionDTO>> getSubmissionByHomeworkForStudent(@PathVariable Long homeworkId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> principal = (Map<String, Object>) authentication.getPrincipal();
        String username = (String) principal.get("username");
        Long studentId = homeworkService.findByUsername(username).getId();

        SubmissionDTO submission = submissionService.findByHomeworkIdAndStudentIdWithHomeworkInfo(homeworkId, studentId);
        return ResponseEntity.ok(ApiResponse.success(submission));
    }
    
    @Operation(summary = "教师查看所有提交", description = "教师查看自己作业的所有提交记录")
    @GetMapping("/teacher/all")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<SubmissionDTO>>> getAllSubmissionsForTeacher() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> principal = (Map<String, Object>) authentication.getPrincipal();
        String username = (String) principal.get("username");
        Long teacherId = homeworkService.findByUsername(username).getId();
        
        // 获取教师的所有作业
        List<Homework> homeworkList = homeworkService.findByTeacherId(teacherId);
        List<Long> homeworkIds = homeworkList.stream().map(Homework::getId).toList();
        
        // 获取这些作业的所有提交
        List<Submission> submissions = new java.util.ArrayList<>();
        for (Long homeworkId : homeworkIds) {
            submissions.addAll(submissionService.findByHomeworkId(homeworkId));
        }
        
        // 转换为 SubmissionDTO 列表，添加作业信息和学生信息
        List<SubmissionDTO> dtos = submissions.stream().map(submission -> {
            Homework homework = null;
            try {
                homework = homeworkService.findById(submission.getHomeworkId()).orElse(null);
            } catch (Exception e) {
            }
            User student = null;
            try {
                student = userRepository.findById(submission.getStudentId()).orElse(null);
            } catch (Exception e) {
            }
            String studentName = student != null ? student.getName() : null;
            return new SubmissionDTO(submission, homework, studentName);
        }).collect(Collectors.toList());
        
        return ResponseEntity.ok(ApiResponse.success(dtos));
    }
    
    @Operation(summary = "批改作业", description = "教师批改学生作业")
    @PutMapping("/{id}/grade")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Submission>> gradeSubmission(@PathVariable Long id,
                                                                    @RequestParam Double score,
                                                                    @RequestParam(required = false) String feedback) {
        Submission submission = submissionService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("提交", "id", id));
        
        Homework homework = homeworkService.findById(submission.getHomeworkId())
                .orElseThrow(() -> new ResourceNotFoundException("作业", "id", submission.getHomeworkId()));
        
        if (homework.getMaxScore() != null && score > homework.getMaxScore()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("评分不能超过作业满分"));
        }
        
        Submission gradedSubmission = submissionService.gradeSubmission(id, score, feedback);
        return ResponseEntity.ok(ApiResponse.success("批改成功", gradedSubmission));
    }
    
    @Operation(summary = "更新提交", description = "更新作业提交内容")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Submission>> updateSubmission(@PathVariable Long id,
                                                                     @RequestParam(required = false) String content,
                                                                     @RequestParam(required = false) MultipartFile file,
                                                                     @RequestParam(required = false) Boolean deleteFile) {
        Submission submission = submissionService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("提交", "id", id));
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> principal = (Map<String, Object>) authentication.getPrincipal();
        String username = (String) principal.get("username");
        Long studentId = homeworkService.findByUsername(username).getId();
        
        if (!submission.getStudentId().equals(studentId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("无权修改他人的提交"));
        }
        
        if ("graded".equals(submission.getStatus())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("已批改的作业不能修改"));
        }
        
        if (content != null) {
            submission.setContent(content);
        }
        
        // 处理删除附件请求
        if (Boolean.TRUE.equals(deleteFile)) {
            // 删除旧文件
            if (submission.getFilePath() != null) {
                Path oldFilePath = Paths.get(submission.getFilePath());
                try {
                    Files.deleteIfExists(oldFilePath);
                } catch (IOException e) {
                    // 旧文件删除失败不影响更新
                    e.printStackTrace();
                }
            }
            submission.setFilePath(null);
            submission.setFileName(null);
        }
        
        if (file != null && !file.isEmpty()) {
            try {
                String validationError = saveSubmissionFile(submission, file);
                if (validationError != null) {
                    return ResponseEntity.badRequest().body(ApiResponse.error(validationError));
                }
            } catch (IOException e) {
                return ResponseEntity.badRequest().body(ApiResponse.error("文件上传失败"));
            }
        }

        Submission updatedSubmission = submissionService.updateSubmission(submission);
        return ResponseEntity.ok(ApiResponse.success("更新成功", updatedSubmission));
    }

    @Operation(summary = "删除提交", description = "删除作业提交")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<String>> deleteSubmission(@PathVariable Long id) {
        Submission submission = submissionService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("提交", "id", id));
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> principal = (Map<String, Object>) authentication.getPrincipal();
        String username = (String) principal.get("username");
        Long studentId = homeworkService.findByUsername(username).getId();
        
        if (!submission.getStudentId().equals(studentId)) {
            return ResponseEntity.status(403).body(ApiResponse.error("无权删除他人的提交"));
        }

        if ("graded".equals(submission.getStatus())) {
            return ResponseEntity.badRequest().body(ApiResponse.error("已批改的作业不能删除"));
        }
        
        submissionService.deleteSubmission(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功", null));
    }
    
    @Operation(summary = "作业查重", description = "检测作业抄袭情况")
    @PostMapping("/homework/{homeworkId}/plagiarism")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<Submission>>> checkPlagiarism(@PathVariable Long homeworkId) {
        List<Submission> submissions = submissionService.checkPlagiarism(homeworkId);
        return ResponseEntity.ok(ApiResponse.success(submissions));
    }

    @Operation(summary = "下载附件", description = "下载提交的作业附件")
    @GetMapping("/attachment/{id}")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER', 'ADMIN')")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable Long id) {
        Submission submission = submissionService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("提交", "id", id));

        if (submission.getFilePath() == null || submission.getFilePath().isEmpty()) {
            throw new ResourceNotFoundException("附件", "id", id);
        }

        try {
            Path filePath = Paths.get(submission.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String fileName = submission.getFileName();
                if (fileName == null || fileName.isEmpty()) {
                    fileName = filePath.getFileName().toString();
                }

                // 正确编码文件名，确保保留后缀名
                try {
                    fileName = java.net.URLEncoder.encode(fileName, "UTF-8").replace("+", " ");
                } catch (java.io.UnsupportedEncodingException e) {
                    e.printStackTrace();
                }

                // 确保文件名包含后缀
                if (!fileName.contains(".")) {
                    // 从文件路径中提取后缀
                    String path = submission.getFilePath();
                    if (path != null && path.contains(".")) {
                        int lastDotIndex = path.lastIndexOf('.');
                        if (lastDotIndex > 0) {
                            String extension = path.substring(lastDotIndex);
                            fileName += extension;
                        }
                    }
                }

                // 根据文件扩展名设置正确的Content-Type
                MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;
                if (fileName != null) {
                    String fileExtension = fileName.toLowerCase();
                    if (fileExtension.endsWith(".pdf")) {
                        contentType = MediaType.APPLICATION_PDF;
                    } else if (fileExtension.endsWith(".doc")) {
                        contentType = MediaType.valueOf("application/msword");
                    } else if (fileExtension.endsWith(".docx")) {
                        contentType = MediaType.valueOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
                    } else if (fileExtension.endsWith(".zip")) {
                        contentType = MediaType.valueOf("application/zip");
                    } else if (fileExtension.endsWith(".rar")) {
                        contentType = MediaType.valueOf("application/x-rar-compressed");
                    } else if (fileExtension.endsWith(".jpg") || fileExtension.endsWith(".jpeg")) {
                        contentType = MediaType.IMAGE_JPEG;
                    } else if (fileExtension.endsWith(".png")) {
                        contentType = MediaType.IMAGE_PNG;
                    } else if (fileExtension.endsWith(".gif")) {
                        contentType = MediaType.IMAGE_GIF;
                    } else if (fileExtension.endsWith(".webp")) {
                        contentType = MediaType.valueOf("image/webp");
                    } else if (fileExtension.endsWith(".txt")) {
                        contentType = MediaType.TEXT_PLAIN;
                    } else if (fileExtension.endsWith(".json")) {
                        contentType = MediaType.APPLICATION_JSON;
                    }
                }

                return ResponseEntity.ok()
                        .contentType(contentType)
                        .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
                        .body(resource);
            } else {
                throw new ResourceNotFoundException("文件", "id", id);
            }
        } catch (IOException e) {
            throw new RuntimeException("文件读取失败");
        }
    }

    @Operation(summary = "获取作业成绩统计", description = "获取指定作业的成绩统计信息（平均分、最高分、最低分等）")
    @GetMapping("/homework/{homeworkId}/statistics")
    @PreAuthorize("hasAnyRole('TEACHER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHomeworkStatistics(@PathVariable Long homeworkId) {
        Homework homework = homeworkService.findById(homeworkId)
                .orElseThrow(() -> new ResourceNotFoundException("作业", "id", homeworkId));

        List<Long> studentIds = homeworkService.getStudentIdsByHomeworkId(homeworkId);
        List<Submission> submissions = submissionService.findByHomeworkId(homeworkId);

        Map<Long, Submission> latestSubmissionByStudent = new HashMap<>();
        for (Submission submission : submissions) {
            Submission existing = latestSubmissionByStudent.get(submission.getStudentId());
            if (existing == null) {
                latestSubmissionByStudent.put(submission.getStudentId(), submission);
                continue;
            }
            if (submission.getSubmittedAt() != null && existing.getSubmittedAt() != null
                    && submission.getSubmittedAt().after(existing.getSubmittedAt())) {
                latestSubmissionByStudent.put(submission.getStudentId(), submission);
            }
        }

        int assignedCount = studentIds.size();
        int submittedCount = 0;
        int gradedCount = 0;
        int pendingGradeCount = 0;
        List<Map<String, Object>> studentDetails = new ArrayList<>();

        for (Long studentId : studentIds) {
            User student = userRepository.findById(studentId).orElse(null);
            String studentName = student != null
                    ? (student.getName() != null && !student.getName().isBlank() ? student.getName() : student.getUsername())
                    : "未知学生";
            Submission submission = latestSubmissionByStudent.get(studentId);

            Map<String, Object> row = new HashMap<>();
            row.put("studentId", studentId);
            row.put("studentName", studentName);

            if (submission == null) {
                row.put("status", "未提交");
                row.put("statusCode", "unsubmitted");
            } else if ("graded".equals(submission.getStatus()) || submission.getScore() != null) {
                row.put("status", "已批改");
                row.put("statusCode", "graded");
                row.put("score", submission.getScore());
                row.put("submittedAt", submission.getSubmittedAt());
                row.put("submissionId", submission.getId());
                submittedCount++;
                gradedCount++;
            } else {
                row.put("status", "待批改");
                row.put("statusCode", "submitted");
                row.put("submittedAt", submission.getSubmittedAt());
                row.put("submissionId", submission.getId());
                submittedCount++;
                pendingGradeCount++;
            }
            studentDetails.add(row);
        }

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("homeworkTitle", homework.getTitle());
        statistics.put("courseName", homework.getCourseName());
        statistics.put("deadline", homework.getDeadline());
        statistics.put("status", homework.getStatus());
        statistics.put("homeworkMaxScore", homework.getMaxScore());
        statistics.put("assignedStudentCount", assignedCount);
        statistics.put("totalSubmissions", submittedCount);
        statistics.put("submittedCount", submittedCount);
        statistics.put("unsubmittedCount", Math.max(0, assignedCount - submittedCount));
        statistics.put("gradedSubmissions", gradedCount);
        statistics.put("gradedCount", gradedCount);
        statistics.put("pendingGradeCount", pendingGradeCount);
        statistics.put("completionRate", assignedCount > 0
                ? Math.round(submittedCount * 1000.0 / assignedCount) / 10.0
                : 0.0);
        statistics.put("gradeRate", submittedCount > 0
                ? Math.round(gradedCount * 1000.0 / submittedCount) / 10.0
                : 0.0);
        statistics.put("studentDetails", studentDetails);

        if (gradedCount > 0) {
            double averageScore = latestSubmissionByStudent.values().stream()
                    .filter(s -> s.getScore() != null)
                    .mapToDouble(Submission::getScore)
                    .average()
                    .orElse(0.0);

            double maxScore = latestSubmissionByStudent.values().stream()
                    .filter(s -> s.getScore() != null)
                    .mapToDouble(Submission::getScore)
                    .max()
                    .orElse(0.0);

            double minScore = latestSubmissionByStudent.values().stream()
                    .filter(s -> s.getScore() != null)
                    .mapToDouble(Submission::getScore)
                    .min()
                    .orElse(0.0);

            statistics.put("averageScore", Math.round(averageScore * 100.0) / 100.0);
            statistics.put("maxScore", maxScore);
            statistics.put("minScore", minScore);
        } else {
            statistics.put("averageScore", 0.0);
            statistics.put("maxScore", 0.0);
            statistics.put("minScore", 0.0);
        }

        return ResponseEntity.ok(ApiResponse.success(statistics));
    }
}
