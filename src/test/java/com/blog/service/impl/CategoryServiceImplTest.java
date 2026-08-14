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
        when(categoryMapper.selectCount(any())).thenReturn(0L);

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
    @DisplayName("添加分类 - 分类名已存在应返回友好错误")
    void addCategory_duplicateName_shouldReturnError() {
        CategoryCreateDTO dto = new CategoryCreateDTO();
        dto.setName("New Category");
        when(categoryMapper.selectCount(any())).thenReturn(1L);

        Result<Long> result = categoryService.addCategory(dto);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("分类名称已存在");
        verify(categoryMapper, never()).insert(any());
    }

    @Test
    @DisplayName("添加分类 - 并发重复插入应捕获唯一键异常并返回友好错误")
    void addCategory_duplicateKeyException_shouldReturnError() {
        CategoryCreateDTO dto = new CategoryCreateDTO();
        dto.setName("New Category");
        when(categoryMapper.selectCount(any())).thenReturn(0L);
        when(categoryMapper.insert(any(Category.class)))
                .thenThrow(new org.springframework.dao.DuplicateKeyException("uk_name_parent"));

        Result<Long> result = categoryService.addCategory(dto);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("分类名称已存在");
    }

    @Test
    @DisplayName("更新分类 - 未填写的字段不应覆盖原值")
    void updateCategory_nullFields_shouldNotOverwrite() {
        Category existing = new Category();
        existing.setId(1L);
        existing.setName("Old Name");
        existing.setDescription("Old Description");
        existing.setSortOrder(5);
        when(categoryMapper.selectById(1L)).thenReturn(existing);

        CategoryCreateDTO dto = new CategoryCreateDTO();
        dto.setName("New Name");
        // description 和 sortOrder 均未填写（null）
        when(categoryMapper.updateById(any())).thenReturn(1);

        Result<Void> result = categoryService.updateCategory(1L, dto);

        assertThat(result.isSuccess()).isTrue();
        org.mockito.ArgumentCaptor<Category> captor = org.mockito.ArgumentCaptor.forClass(Category.class);
        verify(categoryMapper).updateById(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("New Name");
        assertThat(captor.getValue().getDescription()).isEqualTo("Old Description");
        assertThat(captor.getValue().getSortOrder()).isEqualTo(5);
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
