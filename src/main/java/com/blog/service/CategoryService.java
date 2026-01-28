package com.blog.service;

import com.blog.common.Result;
import com.blog.dto.CategoryCreateDTO;
import com.blog.dto.CategoryDTO;

import java.util.List;

/**
 * 分类服务接口
 */
public interface CategoryService {

    /**
     * 获取分类列表
     */
    Result<List<CategoryDTO>> getCategoryList();

    /**
     * 获取分类详情
     */
    Result<CategoryDTO> getCategoryById(Long categoryId);

    /**
     * 添加分类
     */
    Result<Long> addCategory(CategoryCreateDTO categoryCreateDTO);

    /**
     * 编辑分类
     */
    Result<Void> updateCategory(Long categoryId, CategoryCreateDTO categoryCreateDTO);

    /**
     * 删除分类
     */
    Result<Void> deleteCategory(Long categoryId);

    /**
     * 获取分类下的文章数量
     */
    Result<Integer> getCategoryArticleCount(Long categoryId);
}