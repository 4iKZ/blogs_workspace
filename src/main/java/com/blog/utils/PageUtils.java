package com.blog.utils;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.dto.PageDTO;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页工具类
 * 封装分页相关的通用逻辑
 */
public class PageUtils {

    /**
     * 默认页码
     */
    public static final Integer DEFAULT_PAGE = 1;

    /**
     * 默认每页大小
     */
    public static final Integer DEFAULT_SIZE = 10;

    /**
     * 创建分页对象
     * @param page 页码
     * @param size 每页大小
     * @param <T> 实体类型
     * @return Page对象
     */
    public static <T> Page<T> createPage(Integer page, Integer size) {
        return new Page<>(getValidPage(page), getValidSize(size));
    }

    /**
     * 获取有效的页码
     * @param page 页码
     * @return 有效的页码
     */
    public static Integer getValidPage(Integer page) {
        return page == null || page < 1 ? DEFAULT_PAGE : page;
    }

    /**
     * 获取有效的每页大小
     * @param size 每页大小
     * @return 有效的每页大小
     */
    public static Integer getValidSize(Integer size) {
        return size == null || size < 1 ? DEFAULT_SIZE : size;
    }

    /**
     * 转换分页结果
     * @param pageResult IPage结果
     * @param converter 转换函数
     * @param <T> 实体类型
     * @param <R> DTO类型
     * @return 转换后的分页结果
     */
    public static <T, R> PageDTO<R> convertPageResult(IPage<T> pageResult, Function<T, R> converter) {
        PageDTO<R> pageDTO = new PageDTO<>();
        pageDTO.setTotal(pageResult.getTotal());
        pageDTO.setCurrent((int) pageResult.getCurrent());
        pageDTO.setSize((int) pageResult.getSize());
        pageDTO.setPages((int) pageResult.getPages());
        pageDTO.setRecords(convertList(pageResult.getRecords(), converter));
        return pageDTO;
    }

    /**
     * 转换列表
     * @param list 原始列表
     * @param converter 转换函数
     * @param <T> 原始类型
     * @param <R> 目标类型
     * @return 转换后的列表
     */
    public static <T, R> List<R> convertList(List<T> list, Function<T, R> converter) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        return list.stream().map(converter).collect(Collectors.toList());
    }

    /**
     * 计算偏移量
     * @param page 页码
     * @param size 每页大小
     * @return 偏移量
     */
    public static Integer calculateOffset(Integer page, Integer size) {
        return (getValidPage(page) - 1) * getValidSize(size);
    }
}
