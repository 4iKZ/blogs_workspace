package com.blog.service.impl;

import com.blog.common.Result;
import com.blog.dto.CategoryCreateDTO;
import com.blog.dto.CategoryDTO;
import com.blog.entity.Article;
import com.blog.entity.Category;
import com.blog.mapper.ArticleMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.blog.mapper.CategoryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    @DisplayName("获取分类列表 - 非管理员只返回启用分类")
    void getCategoryList_nonAdmin_shouldReturnOnlyActiveCategories() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Test Category");
        when(categoryMapper.selectAllActiveCategories()).thenReturn(Collections.singletonList(category));

        Result<List<CategoryDTO>> result = categoryService.getCategoryList();

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getName()).isEqualTo("Test Category");
        verify(categoryMapper).selectAllActiveCategories();
        verify(categoryMapper, never()).selectList(any());
    }

    @Test
    @DisplayName("获取分类列表 - 管理员返回全部分类（含禁用）")
    void getCategoryList_admin_shouldReturnAllCategories() {
        Category active = new Category();
        active.setId(1L);
        active.setName("Active");
        Category disabled = new Category();
        disabled.setId(2L);
        disabled.setName("Disabled");
        when(categoryMapper.selectList(any())).thenReturn(List.of(active, disabled));

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin", null,
                        List.of(new SimpleGrantedAuthority("ROLE_admin"))));
        try {
            Result<List<CategoryDTO>> result = categoryService.getCategoryList();

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).hasSize(2);
            verify(categoryMapper).selectList(any());
            verify(categoryMapper, never()).selectAllActiveCategories();
        } finally {
            SecurityContextHolder.clearContext();
        }
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

    @Test
    @DisplayName("分类列表 - 文章数来自已发布文章的分组统计，无文章的分类为 0")
    void getCategoryList_shouldFillPublishedArticleCounts() {
        Category withArticles = new Category();
        withArticles.setId(1L);
        withArticles.setName("有文章");
        Category empty = new Category();
        empty.setId(2L);
        empty.setName("空分类");
        when(categoryMapper.selectAllActiveCategories()).thenReturn(List.of(withArticles, empty));
        when(articleMapper.selectMaps(any())).thenReturn(List.of(countRow(1L, 3L)));

        Result<List<CategoryDTO>> result = categoryService.getCategoryList();

        assertThat(result.getData()).hasSize(2);
        assertThat(result.getData().get(0).getArticleCount()).isEqualTo(3L);
        assertThat(result.getData().get(1).getArticleCount()).isEqualTo(0L);
    }

    @Test
    @DisplayName("分类列表统计只计入已发布文章（status = 2），草稿与已下线文章不参与计数")
    void getCategoryList_countQueryFiltersPublishedStatus() {
        when(categoryMapper.selectAllActiveCategories()).thenReturn(List.of());
        ArgumentCaptor<QueryWrapper<Article>> captor = ArgumentCaptor.forClass(QueryWrapper.class);

        categoryService.getCategoryList();

        verify(articleMapper).selectMaps(captor.capture());
        assertThat(captor.getValue().getSqlSegment()).contains("status = #{").contains("GROUP BY category_id");
        assertThat(captor.getValue().getParamNameValuePairs()).containsValue(Article.STATUS_PUBLISHED);
    }

    @Test
    @DisplayName("分类详情与文章数量接口 - 只统计已发布文章")
    void getCategoryById_andArticleCount_shouldUsePublishedCount() {
        Category category = new Category();
        category.setId(7L);
        category.setName("数据库");
        when(categoryMapper.selectById(7L)).thenReturn(category);
        when(articleMapper.selectCount(any())).thenReturn(4L);

        Result<CategoryDTO> detail = categoryService.getCategoryById(7L);
        Result<Integer> count = categoryService.getCategoryArticleCount(7L);

        assertThat(detail.getData().getArticleCount()).isEqualTo(4L);
        assertThat(count.getData()).isEqualTo(4);
    }

    private static Map<String, Object> countRow(long categoryId, long articleCount) {
        Map<String, Object> row = new HashMap<>();
        row.put("category_id", categoryId);
        row.put("article_count", articleCount);
        return row;
    }
}
