package com.blog.utils;

import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * DTO转换工具类
 * 封装实体与DTO之间的转换逻辑
 */
public class DTOConverter {

    /**
     * 转换单个对象
     * @param source 源对象
     * @param targetClass 目标类
     * @param <S> 源类型
     * @param <T> 目标类型
     * @return 转换后的目标对象
     */
    public static <S, T> T convert(S source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("DTO转换失败: " + e.getMessage(), e);
        }
    }

    /**
     * 转换单个对象，支持自定义转换逻辑
     * @param source 源对象
     * @param targetClass 目标类
     * @param customizer 自定义转换逻辑
     * @param <S> 源类型
     * @param <T> 目标类型
     * @return 转换后的目标对象
     */
    public static <S, T> T convert(S source, Class<T> targetClass, Consumer<T> customizer) {
        T target = convert(source, targetClass);
        if (target != null && customizer != null) {
            customizer.accept(target);
        }
        return target;
    }

    /**
     * 转换单个对象，支持双向自定义转换逻辑
     * @param source 源对象
     * @param targetClass 目标类
     * @param customizer 自定义转换逻辑，同时提供源对象和目标对象
     * @param <S> 源类型
     * @param <T> 目标类型
     * @return 转换后的目标对象
     */
    public static <S, T> T convert(S source, Class<T> targetClass, BiConsumer<S, T> customizer) {
        T target = convert(source, targetClass);
        if (target != null && customizer != null) {
            customizer.accept(source, target);
        }
        return target;
    }

    /**
     * 转换列表
     * @param sources 源对象列表
     * @param targetClass 目标类
     * @param <S> 源类型
     * @param <T> 目标类型
     * @return 转换后的目标对象列表
     */
    public static <S, T> List<T> convertList(List<S> sources, Class<T> targetClass) {
        if (sources == null || sources.isEmpty()) {
            return List.of();
        }
        return sources.stream()
                .map(source -> convert(source, targetClass))
                .collect(Collectors.toList());
    }

    /**
     * 转换列表，支持自定义转换逻辑
     * @param sources 源对象列表
     * @param targetClass 目标类
     * @param customizer 自定义转换逻辑
     * @param <S> 源类型
     * @param <T> 目标类型
     * @return 转换后的目标对象列表
     */
    public static <S, T> List<T> convertList(List<S> sources, Class<T> targetClass, Consumer<T> customizer) {
        if (sources == null || sources.isEmpty()) {
            return List.of();
        }
        return sources.stream()
                .map(source -> convert(source, targetClass, customizer))
                .collect(Collectors.toList());
    }

    /**
     * 转换列表，支持双向自定义转换逻辑
     * @param sources 源对象列表
     * @param targetClass 目标类
     * @param customizer 自定义转换逻辑，同时提供源对象和目标对象
     * @param <S> 源类型
     * @param <T> 目标类型
     * @return 转换后的目标对象列表
     */
    public static <S, T> List<T> convertList(List<S> sources, Class<T> targetClass, BiConsumer<S, T> customizer) {
        if (sources == null || sources.isEmpty()) {
            return List.of();
        }
        return sources.stream()
                .map(source -> convert(source, targetClass, customizer))
                .collect(Collectors.toList());
    }

    /**
     * 双向Consumer接口，用于同时接收源对象和目标对象
     * @param <S> 源类型
     * @param <T> 目标类型
     */
    @FunctionalInterface
    public interface BiConsumer<S, T> {
        /**
         * 对给定的源对象和目标对象执行操作
         * @param source 源对象
         * @param target 目标对象
         */
        void accept(S source, T target);
    }
}
