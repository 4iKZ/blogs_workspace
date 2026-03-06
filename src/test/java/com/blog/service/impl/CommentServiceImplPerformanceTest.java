package com.blog.service.impl;

import com.blog.common.Result;
import com.blog.dto.CommentDTO;
import com.blog.entity.Comment;
import com.blog.mapper.CommentMapper;
import com.blog.utils.RedisCacheUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 验证性能优化后的 CommentServiceImpl 行为：
 * 1. getArticleCommentCount 使用 countCommentsByArticleId（COUNT SQL），不加载全量数据
 * 2. getHotComments 使用 selectHotCommentsByArticleId（数据库排序+LIMIT），不在内存排序
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CommentServiceImpl 性能优化测试")
class CommentServiceImplPerformanceTest {

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private RedisCacheUtils redisCacheUtils;

    @InjectMocks
    private CommentServiceImpl commentService;

    // -----------------------------------------------------------------------
    // getArticleCommentCount
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getArticleCommentCount 使用 COUNT SQL，不调用 selectCommentsByArticleId")
    void testGetArticleCommentCount_UsesCountQuery_NotFullLoad() {
        Long articleId = 1L;
        when(redisCacheUtils.getCache(anyString())).thenReturn(null);
        when(commentMapper.countCommentsByArticleId(articleId, 2)).thenReturn(5);

        Result<Integer> result = commentService.getArticleCommentCount(articleId);

        assertTrue(result.isSuccess());
        assertEquals(5, result.getData());

        // 验证使用了 COUNT 查询
        verify(commentMapper).countCommentsByArticleId(articleId, 2);
        // 确保没有调用加载全量数据的旧方法
        verify(commentMapper, never()).selectCommentsByArticleId(eq(articleId), any());
    }

    @Test
    @DisplayName("getArticleCommentCount 缓存命中时直接返回，不查询数据库")
    void testGetArticleCommentCount_CacheHit_NoDatabaseQuery() {
        Long articleId = 2L;
        when(redisCacheUtils.getCache(anyString())).thenReturn(3);

        Result<Integer> result = commentService.getArticleCommentCount(articleId);

        assertTrue(result.isSuccess());
        assertEquals(3, result.getData());
        verify(commentMapper, never()).countCommentsByArticleId(any(), any());
        verify(commentMapper, never()).selectCommentsByArticleId(any(), any());
    }

    // -----------------------------------------------------------------------
    // getHotComments
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("getHotComments 使用数据库排序查询，不调用 selectCommentsByArticleId")
    void testGetHotComments_UsesDatabaseOrderedQuery_NotFullLoad() {
        Long articleId = 10L;
        int limit = 3;

        Comment c1 = new Comment();
        c1.setId(1L);
        c1.setContent("热门评论1");
        c1.setLikeCount(100);
        c1.setParentId(0L);
        c1.setUserId(1L);

        Comment c2 = new Comment();
        c2.setId(2L);
        c2.setContent("热门评论2");
        c2.setLikeCount(50);
        c2.setParentId(0L);
        c2.setUserId(2L);

        when(redisCacheUtils.getCache(anyString())).thenReturn(null);
        when(commentMapper.selectHotCommentsByArticleId(articleId, 2, limit)).thenReturn(List.of(c1, c2));

        Result<List<CommentDTO>> result = commentService.getHotComments(articleId, limit);

        assertTrue(result.isSuccess());
        assertEquals(2, result.getData().size());

        // 验证使用了带 ORDER BY + LIMIT 的查询
        verify(commentMapper).selectHotCommentsByArticleId(articleId, 2, limit);
        // 确保没有调用加载全量数据的旧方法
        verify(commentMapper, never()).selectCommentsByArticleId(eq(articleId), any());
    }

    @Test
    @DisplayName("getHotComments 缓存命中时直接返回，不查询数据库")
    void testGetHotComments_CacheHit_NoDatabaseQuery() {
        Long articleId = 11L;
        List<CommentDTO> cached = List.of(new CommentDTO());
        when(redisCacheUtils.getCache(anyString())).thenReturn(cached);

        Result<List<CommentDTO>> result = commentService.getHotComments(articleId, 5);

        assertTrue(result.isSuccess());
        verify(commentMapper, never()).selectHotCommentsByArticleId(any(), any(), any());
        verify(commentMapper, never()).selectCommentsByArticleId(any(), any());
    }
}
