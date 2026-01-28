package com.blog.service.impl;
import com.blog.service.FileUploadService;
import com.blog.common.Result;
import com.blog.dto.CategoryCreateDTO;
import com.blog.dto.CategoryDTO;
import com.blog.entity.Category;
import com.blog.entity.Article;
import com.blog.mapper.CategoryMapper;
import com.blog.mapper.ArticleMapper;
import com.blog.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 分类服务实现类
 */
@Service
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private ArticleMapper articleMapper;

    @Override
    public Result<List<CategoryDTO>> getCategoryList() {
        log.info("获取所有分类");
        List<Category> categories = categoryMapper.selectList(null);
        List<CategoryDTO> categoryDTOs = categories.stream()
                .map(category -> {
                    CategoryDTO dto = new CategoryDTO();
                    BeanUtils.copyProperties(category, dto);
                    return dto;
                })
                .collect(Collectors.toList());
        return Result.success(categoryDTOs);
    }

    @Override
    public Result<CategoryDTO> getCategoryById(Long categoryId) {
        log.info("根据ID获取分类：{}", categoryId);
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            return Result.error("分类不存在");
        }
        CategoryDTO dto = new CategoryDTO();
        BeanUtils.copyProperties(category, dto);
        return Result.success(dto);
    }

    @Override
    public Result<Long> addCategory(CategoryCreateDTO categoryCreateDTO) {
        log.info("创建分类：{}", categoryCreateDTO.getName());
        // 检查分类名是否已存在
        // TODO: 实现分类名唯一性检查
        
        Category category = new Category();
        BeanUtils.copyProperties(categoryCreateDTO, category);
        
        category.setCreateTime(LocalDateTime.now());
        category.setUpdateTime(LocalDateTime.now());
        
        int result = categoryMapper.insert(category);
        if (result > 0) {
            return Result.success(category.getId());
        }
        return Result.error("创建分类失败");
    }

    @Override
    public Result<Void> updateCategory(Long categoryId, CategoryCreateDTO categoryCreateDTO) {
        log.info("更新分类：{}", categoryId);
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            return Result.error("分类不存在");
        }
        
        BeanUtils.copyProperties(categoryCreateDTO, category);
        
        category.setUpdateTime(LocalDateTime.now());
        
        int result = categoryMapper.updateById(category);
        if (result > 0) {
            return Result.success();
        }
        return Result.error("更新分类失败");
    }

    @Override
    public Result<Void> deleteCategory(Long categoryId) {
        log.info("删除分类：{}", categoryId);
        Category category = categoryMapper.selectById(categoryId);
        if (category == null) {
            return Result.error("分类不存在");
        }

        // 检查分类下是否有文章
        Long articleCount = articleMapper.selectCount(
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getCategoryId, categoryId)
                        .eq(Article::getDeleted, 0)
        );
        if (articleCount > 0) {
            return Result.error("该分类下还有文章，无法删除");
        }

        int result = categoryMapper.deleteById(categoryId);
        if (result > 0) {
            return Result.success();
        }
        return Result.error("删除分类失败");
    }

    @Override
    public Result<Integer> getCategoryArticleCount(Long categoryId) {
        log.info("获取分类下的文章数量：{}", categoryId);
        // TODO: 实现获取分类下的文章数量逻辑
        return Result.success(0);
    }
}