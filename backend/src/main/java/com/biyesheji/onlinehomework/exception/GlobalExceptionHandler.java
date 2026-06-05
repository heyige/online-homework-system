package com.biyesheji.onlinehomework.exception;

import com.biyesheji.onlinehomework.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 统一处理应用中所有 Controller 抛出的异常，返回统一的 JSON 响应格式
 * 使用 @RestControllerAdvice 注解，实现对所有 @RestController 的全局异常拦截
 *
 * @author毕业设计
 * @version1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理资源未找到异常
     * 当请求的资源（如用户、作业等）在数据库中不存在时抛出
     *
     * @param ex ResourceNotFoundException 异常对象，包含资源类型和查找条件
     * @return HTTP 404 响应，body 中包含错误信息
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        // 记录错误日志，便于后端调试和问题排查
        log.error("资源未找到：{}", ex.getMessage());

        // 返回 404 Not Found 状态码，响应体包含错误信息
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(404, ex.getMessage()));
    }

    /**
     * 处理业务异常
     * 当业务逻辑检查失败时抛出，如参数校验失败、状态不正确等
     *
     * @param ex BusinessException 异常对象，包含错误码和错误消息
     * @return HTTP 400 响应，body 中包含错误信息
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(BusinessException ex) {
        // 记录业务异常日志
        log.error("业务异常：{}", ex.getMessage());

        // 返回 400 Bad Request 状态码，使用异常中的错误码
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getCode(), ex.getMessage()));
    }

    /**
     * 处理参数验证异常
     * 当使用 @Valid 或 @Validated 注解进行参数校验失败时抛出
     * 通常用于 @RequestBody 注解的 JSON 参数校验
     *
     * @param ex MethodArgumentNotValidException 异常对象，包含所有校验失败的信息
     * @return HTTP 400 响应，body 中包含所有参数错误信息
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        // 记录参数验证失败日志
        log.error("参数验证失败：{}", ex.getMessage());

        // 创建一个 Map 用于存储所有字段级别的错误信息
        // key 为字段名，value 为错误消息
        Map<String, String> errors = new HashMap<>();

        // 遍历所有校验失败的结果，提取字段名和错误消息
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            // 将通用 error 转换为 FieldError 以获取字段名
            String fieldName = ((FieldError) error).getField();
            // 获取字段的校验失败消息（如 @NotBlank(message="不能为空")）
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        // 返回 400 Bad Request 状态码，响应体包含所有参数错误
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(400, "参数验证失败"));
    }

    /**
     * 处理凭证错误异常
     * 当用户名或密码错误时抛出，用于登录认证失败
     *
     * @param ex BadCredentialsException 异常对象，通常由 Spring Security 抛出
     * @return HTTP 401 响应，body 中包含通用错误消息（不暴露具体原因，防止攻击）
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentialsException(BadCredentialsException ex) {
        // 记录凭证错误日志
        log.error("凭证错误：{}", ex.getMessage());

        // 返回 401 Unauthorized 状态码
        // 注意：这里使用通用消息，不告诉攻击者是用户名错还是密码错，提高安全性
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(401, "用户名或密码错误"));
    }

    /**
     * 处理认证异常
     * 当 Spring Security 其他认证相关异常发生时处理
     *
     * @param ex AuthenticationException 异常对象，包含认证失败的具体原因
     * @return HTTP 401 响应，body 中包含错误信息
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthenticationException(AuthenticationException ex) {
        // 记录认证异常日志
        log.error("认证异常：{}", ex.getMessage());

        // 返回 401 Unauthorized 状态码
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.error(401, "认证失败：" + ex.getMessage()));
    }

    /**
     * 处理所有未捕获的异常
     * 作为最后的异常处理防线，捕获所有上述处理器无法处理的异常
     * 防止敏感异常信息暴露给客户端
     *
     * @param ex Exception 异常对象
     * @return HTTP 500 响应，body 中包含通用错误消息
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGlobalException(Exception ex) {
        // 记录完整的异常堆栈信息，便于后端问题排查
        log.error("系统异常：", ex);

        // 返回 500 Internal Server Error 状态码
        // 使用通用消息，避免向客户端暴露系统内部细节
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("系统错误：" + ex.getMessage()));
    }
}