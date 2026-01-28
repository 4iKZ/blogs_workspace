package com.blog.utils;

import com.blog.common.Result;
import com.blog.common.ResultCode;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 业务逻辑工具类
 * 封装通用的业务逻辑
 */
@Slf4j
public class BusinessUtils {

    /**
     * 检查ID是否有效
     * @param id ID值
     * @return ID是否有效
     */
    public static boolean isValidId(Long id) {
        return id != null && id > 0;
    }

    /**
     * 检查对象是否存在
     * @param object 要检查的对象
     * @param errorMessage 错误信息
     * @param <T> 对象类型
     * @return 如果对象存在，返回该对象；否则抛出异常
     */
    public static <T> T checkExist(T object, String errorMessage) {
        if (object == null) {
            throw new RuntimeException(errorMessage);
        }
        return object;
    }

    /**
     * Check if ID exists
     * @param id ID value
     * @param findByIdFunction Function to find object by ID
     * @param errorMessage Error message
     * @param <T> Object type
     * @return If object exists, return the object; otherwise throw exception
     */
    public static <T> T checkIdExist(Long id, IdFunction<T> findByIdFunction, String errorMessage) {
        if (!isValidId(id)) {
            log.warn("Invalid ID provided: {}", id);
            throw new RuntimeException("无效的ID: " + (id == null ? "null" : id) + ". 请确保提供有效的ID值");
        }
        T object = findByIdFunction.apply(id);
        return checkExist(object, errorMessage);
    }

    /**
     * 设置更新时间
     * @param object 要设置更新时间的对象
     * @param <T> 对象类型
     * @return 更新后的对象
     */
    public static <T> T setUpdateTime(T object) {
        if (object != null) {
            try {
                // 使用反射设置updateTime字段
                java.lang.reflect.Method method = object.getClass().getMethod("setUpdateTime", LocalDateTime.class);
                method.invoke(object, LocalDateTime.now());
            } catch (Exception e) {
                // 如果反射失败，忽略异常
                log.warn("Failed to set update time for object: {}", object.getClass().getName(), e);
            }
        }
        return object;
    }

    /**
     * 成功结果快捷创建
     * @param data 响应数据
     * @param <T> 数据类型
     * @return Result对象
     */
    public static <T> Result<T> success(T data) {
        return Result.success(data);
    }

    /**
     * 成功结果快捷创建（无数据）
     * @return Result对象
     */
    public static Result<Void> success() {
        return Result.success();
    }

    /**
     * 错误结果快捷创建
     * @param message 错误信息
     * @param <T> 数据类型
     * @return Result对象
     */
    public static <T> Result<T> error(String message) {
        return Result.error(message);
    }

    /**
     * 错误结果快捷创建
     * @param code 错误码
     * @param message 错误信息
     * @param <T> 数据类型
     * @return Result对象
     */
    public static <T> Result<T> error(Integer code, String message) {
        return Result.error(code, message);
    }

    /**
     * 错误结果快捷创建
     * @param resultCode ResultCode枚举
     * @param <T> 数据类型
     * @return Result对象
     */
    public static <T> Result<T> error(ResultCode resultCode) {
        return Result.error(resultCode);
    }

    /**
     * 检查状态值是否有效
     * @param status 状态值
     * @param validStatuses 有效的状态值数组
     * @return 状态值是否有效
     */
    public static boolean isValidStatus(Integer status, Integer... validStatuses) {
        if (status == null) {
            return false;
        }
        for (Integer validStatus : validStatuses) {
            if (Objects.equals(status, validStatus)) {
                return true;
            }
        }
        return false;
    }

    /**
     * ID函数接口
     * 用于根据ID查找对象
     * @param <T> 对象类型
     */
    @FunctionalInterface
    public interface IdFunction<T> {
        T apply(Long id);
    }

    /**
     * 可更新接口
     * 用于统一设置更新时间
     */
    public interface Updatable {
        void setUpdateTime(LocalDateTime updateTime);
    }
}
