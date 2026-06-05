package com.biyesheji.onlinehomework.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 业务异常类
 * 用于处理业务逻辑中的错误情况，如参数校验失败、状态不正确、业务规则违反等
 * 继承自 RuntimeException，表示非检查型异常，不需要强制捕获
 *
 * @author毕业设计
 * @version1.0.0
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BusinessException extends RuntimeException {

    /**
     * 错误码，用于前端区分不同类型的业务错误
     * 例如：400 表示参数错误，401 表示未授权，403 表示禁止访问等
     */
    private int code;

    /**
     * 错误消息，描述具体的业务错误信息
     * 会直接展示给用户
     */
    private String message;

    /**
     * 构造函数，仅包含错误消息
     * 默认错误码为 500（服务器内部错误）
     *
     * @param message 错误消息
     */
    public BusinessException(String message) {
        super(message);
        this.code = 500;
        this.message = message;
    }

    /**
     * 构造函数，包含错误码和错误消息
     *
     * @param code 错误码
     * @param message 错误消息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }
}