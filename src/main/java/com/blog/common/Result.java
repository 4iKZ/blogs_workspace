package com.blog.common;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * 统一响应结果类
 */
@Schema(description = "统一响应结果")
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "响应码")
    @JsonProperty("code")
    private Integer code;

    @Schema(description = "响应消息")
    @JsonProperty("message")
    private String message;

    @Schema(description = "响应数据")
    @JsonProperty("data")
    private T data;

    @Schema(description = "时间戳")
    @JsonProperty("timestamp")
    private Long timestamp;

    public Result() {
        this.timestamp = System.currentTimeMillis();
    }

    public Result(Integer code, String message) {
        this();
        this.code = code;
        this.message = message;
    }

    public Result(Integer code, String message, T data) {
        this(code, message);
        this.data = data;
    }

    public static <T> Result<T> success() {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage());
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), ResultCode.SUCCESS.getMessage(), data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(ResultCode.SUCCESS.getCode(), message, data);
    }

    public static <T> Result<T> error() {
        return new Result<>(ResultCode.ERROR.getCode(), ResultCode.ERROR.getMessage());
    }

    public static <T> Result<T> error(String message) {
        return new Result<>(ResultCode.ERROR.getCode(), message);
    }

    public static <T> Result<T> error(Integer code, String message) {
        return new Result<>(code, message);
    }

    public static <T> Result<T> error(ResultCode resultCode) {
        return new Result<>(resultCode.getCode(), resultCode.getMessage());
    }

    public static <T> Result<T> error(ResultCode resultCode, String message) {
        return new Result<>(resultCode.getCode(), message);
    }

    // Getters and Setters
    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
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

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    /**
     * 判断操作是否成功
     * @return 如果响应码为200则返回true，否则返回false
     */
    public boolean isSuccess() {
        return this.code != null && this.code.equals(ResultCode.SUCCESS.getCode());
    }

    @Override
    public String toString() {
        return "Result{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", data=" + data +
                ", timestamp=" + timestamp +
                '}';
    }
}