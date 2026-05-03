package com.warehouse.dto;

import java.io.Serializable;

/**
 * API统一响应包装类
 * 用于统一REST API的响应格式
 * 包含成功标识、消息和数据三个核心字段
 * 
 * @param <T> 响应数据的类型
 */
public class ApiResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 操作是否成功
     */
    private boolean success;
    
    /**
     * 响应消息
     * 成功时为"操作成功"或自定义成功消息
     * 失败时为错误描述信息
     */
    private String message;
    
    /**
     * 响应数据
     * 成功时返回业务数据
     * 失败时为null
     */
    private T data;

    /**
     * 默认构造函数
     */
    public ApiResponse() {
    }

    /**
     * 全参数构造函数
     * @param success 是否成功
     * @param message 响应消息
     * @param data 响应数据
     */
    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

    /**
     * 创建成功响应（无自定义消息）
     * 默认消息为"操作成功"
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 成功的ApiResponse对象
     */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, "操作成功", data);
    }

    /**
     * 创建成功响应（带自定义消息）
     * @param message 自定义成功消息
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 成功的ApiResponse对象
     */
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data);
    }

    /**
     * 创建失败响应
     * @param message 错误描述信息
     * @param <T> 数据类型
     * @return 失败的ApiResponse对象
     */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}
