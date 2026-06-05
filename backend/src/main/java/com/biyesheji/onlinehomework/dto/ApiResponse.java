package com.biyesheji.onlinehomework.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 统一 API 响应封装类
 *
 * 描述：所有 Controller 接口返回的响应都会封装为此格式
 * 包含状态码、消息和数据部分，便于前端统一处理
 *
 * @param <T> 响应数据的类型
 *
 * 使用示例：
 * - 成功响应: ApiResponse.success(data)
 * - 成功响应: ApiResponse.success("操作成功", data)
 * - 错误响应: ApiResponse.error("用户名不存在")
 * - 错误响应: ApiResponse.error(400, "参数错误")
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResponse<T> {

    /**
     * 状态码
     * 200: 成功
     * 400: 请求参数错误
     * 401: 未授权/登录失效
     * 403: 拒绝访问/权限不足
     * 404: 资源不存在
     * 500: 服务器内部错误
     */
    private int code;

    /**
     * 响应消息
     * 用于描述请求结果或错误原因
     */
    private String message;

    /**
     * 响应数据
     * 泛型，可以是任意类型（User、List、Map 等）
     */
    private T data;

    /**
     * 创建成功响应（使用默认消息）
     *
     * @param data 响应数据
     * @param <T> 数据类型
     * @return ApiResponse 实例，code=200, message="操作成功"
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "操作成功", data);
    }

    /**
     * 创建成功响应（自定义消息）
     *
     * @param message 自定义消息
     * @param data 响应数据
     * @param <T> 数据类型
     * @return ApiResponse 实例，code=200
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }

    /**
     * 创建错误响应（默认错误码 500）
     *
     * @param message 错误消息
     * @param <T> 数据类型
     * @return ApiResponse 实例，code=500, data=null
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(500, message, null);
    }

    /**
     * 创建错误响应（自定义错误码）
     *
     * @param code 错误状态码
     * @param message 错误消息
     * @param <T> 数据类型
     * @return ApiResponse 实例，data=null
     */
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
}
