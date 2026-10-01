package com.blog.service.impl;

import com.blog.common.Result;
import com.blog.dto.CategoryCreateDTO;
import com.blog.entity.Category;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CategoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceRenameDuplicateTest {

    @Mock
    private CategoryMapper categoryMapper;

    @Mock
    private ArticleMapper articleMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    @DisplayName("重命名分类 - 名称冲突应捕获唯一键异常并返回友好错误，而非 500")
    void updateCategory_duplicateKeyException_shouldReturnError() {
        Category existing = new Category();
        existing.setId(1L);
        existing.setName("Old Name");
        when(categoryMapper.selectById(1L)).thenReturn(existing);

        CategoryCreateDTO dto = new CategoryCreateDTO();
        dto.setName("Taken Name");
        when(categoryMapper.updateById(any(Category.class)))
                .thenThrow(new org.springframework.dao.DuplicateKeyException("uk_name_parent"));

        Result<Void> result = categoryService.updateCategory(1L, dto);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getMessage()).contains("分类名称已存在");
    }
}
