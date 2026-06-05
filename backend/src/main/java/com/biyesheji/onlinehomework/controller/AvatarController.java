package com.biyesheji.onlinehomework.controller;

import com.biyesheji.onlinehomework.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Tag(name = "头像管理", description = "头像上传与访问")
@RestController
@SecurityRequirement(name = "bearerAuth")
public class AvatarController {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png");
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024L;

    private final Path uploadDir;

    public AvatarController() {
        this.uploadDir = Paths.get(System.getProperty("user.dir"), "uploads", "avatars");
        try {
            Files.createDirectories(uploadDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create avatar upload directory", e);
        }
    }

    @Operation(summary = "上传头像", description = "上传头像文件，返回可访问的 URL 路径")
    @PostMapping("/users/me/avatar")
    public ResponseEntity<ApiResponse<String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("请选择头像文件"));
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            return ResponseEntity.badRequest().body(ApiResponse.error("只支持 JPG、PNG 格式的头像"));
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity.badRequest().body(ApiResponse.error("头像文件大小不能超过 2MB"));
        }

        String extension = contentType.toLowerCase(Locale.ROOT).contains("png") ? ".png" : ".jpg";
        String filename = UUID.randomUUID() + extension;

        try {
            Path target = uploadDir.resolve(filename).normalize();
            if (!target.startsWith(uploadDir)) {
                return ResponseEntity.badRequest().body(ApiResponse.error("非法文件名"));
            }
            file.transferTo(target.toFile());
            return ResponseEntity.ok(ApiResponse.success("上传成功", "/public/avatars/" + filename));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(ApiResponse.error("头像上传失败"));
        }
    }

    @Operation(summary = "获取头像", description = "公开访问已上传的头像文件")
    @GetMapping("/public/avatars/{filename}")
    public ResponseEntity<Resource> getAvatar(@PathVariable String filename) throws IOException {
        if (filename == null || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }

        Path filePath = uploadDir.resolve(filename).normalize();
        if (!filePath.startsWith(uploadDir) || !Files.exists(filePath)) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new UrlResource(filePath.toUri());
        String contentType = Files.probeContentType(filePath);
        if (contentType == null) {
            contentType = filename.endsWith(".png") ? MediaType.IMAGE_PNG_VALUE : MediaType.IMAGE_JPEG_VALUE;
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=86400")
                .body(resource);
    }
}
