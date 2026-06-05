package com.biyesheji.onlinehomework.util;

import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;

public final class SubmissionAttachmentValidator {

    private static final long MAX_FILE_SIZE = 100L * 1024 * 1024;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".pdf", ".doc", ".docx", ".zip", ".rar",
            ".jpg", ".jpeg", ".png", ".gif", ".webp"
    );

    private SubmissionAttachmentValidator() {
    }

    public static String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return "文件大小不能超过100MB";
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "文件缺少有效扩展名";
        }

        String fileExtension = originalFilename.substring(originalFilename.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(fileExtension)) {
            return "只允许上传 PDF、Word、压缩包或图片文件";
        }

        return null;
    }
}
