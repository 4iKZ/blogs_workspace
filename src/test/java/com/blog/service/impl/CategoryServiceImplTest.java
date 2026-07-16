package com.blog.service.impl;

import com.blog.common.Result;
import com.blog.dto.CategoryCreateDTO;
import com.blog.dto.CategoryDTO;
import com.blog.entity.Article;
import com.blog.entity.Category;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CategoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private ArticleMapper articleMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    @DisplayName("获取分类列表 - 应返回所有分类")
    void getCategoryList_shouldReturnAllCategories() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Test Category");
        when(categoryMapper.selectList(any())).thenReturn(Collections.singletonList(category));

        Result<List<CategoryDTO>> result = categoryService.getCategoryList();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getName()).isEqualTo("Test Category");
    }

    @Test
    @DisplayName("获取分类详情 - 分类不存在应返回错误")
    void getCategoryById_notFound_shouldReturnError() {
        when(categoryMapper.selectById(anyLong())).thenReturn(null);

        Result<CategoryDTO> result = categoryService.getCategoryById(999L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).isEqualTo("分类不存在");
    }

    @Test
    @DisplayName("添加分类 - 成功应返回分类ID")
    void addCategory_shouldReturnCategoryId() {
        CategoryCreateDTO dto = new CategoryCreateDTO();
        dto.setName("New Category");

        Category category = new Category();
        category.setId(1L);
        when(categoryMapper.insert(any(Category.class))).thenAnswer(invocation -> {
            Category arg = invocation.getArgument(0);
            arg.setId(1L);
            return 1;
        });

        Result<Long> result = categoryService.addCategory(dto);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(1L);
    }

    @Test
    @DisplayName("删除分类 - 分类下有文章应返回错误")
    void deleteCategory_hasArticles_shouldReturnError() {
        Category category = new Category();
        category.setId(1L);
        when(categoryMapper.selectById(1L)).thenReturn(category);
        when(articleMapper.selectCount(any())).thenReturn(5L);

        Result<Void> result = categoryService.deleteCategory(1L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("无法删除");
    }
}
