package com.biyesheji.onlinehomework.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 资源未找到异常类
 * 当请求的资源（如用户、作业、课程等）在数据库中不存在时抛出
 * 继承自 RuntimeException，表示非检查型异常，不需要强制捕获
 *
 * @author毕业设计
 * @version1.0.0
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ResourceNotFoundException extends RuntimeException {

    /**
     * 资源名称
     * 例如：User、Homework、Course 等实体类名
     */
    private String resourceName;

    /**
     * 查找条件字段名
     * 例如：id、username、email 等
     */
    private String fieldName;

    /**
     * 查找条件的值
     * 例如：具体的用户ID、用户名等
     */
    private Object fieldValue;

    /**
     * 构造函数
     * 接收资源名称、字段名和字段值，自动生成友好的错误消息
     *
     * @param resourceName 资源名称（如 "用户"、"作业"）
     * @param fieldName 查找字段名（如 "id"、"username"）
     * @param fieldValue 字段值（如具体的ID值或用户名）
     *
     * @example new ResourceNotFoundException("用户", "id", 123)
     *          生成的错误消息为："用户 不存在：id = '123'"
     */
    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        // 调用父类构造函数，设置异常消息
        super(String.format("%s 不存在：%s = '%s'", resourceName, fieldName, fieldValue));

        // 保存各个属性，便于日志记录和后续处理
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }
}