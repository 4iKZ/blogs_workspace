package com.blog.service.impl;

import com.blog.dto.ArticleDTO;
import com.blog.entity.Article;
import com.blog.entity.User;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CategoryMapper;
import com.blog.mapper.UserFavoriteMapper;
import com.blog.mapper.UserLikeMapper;
import com.blog.mapper.UserMapper;
import com.blog.utils.RedisCacheUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("文章DTO组装组件测试")
class ArticleDtoAssemblerUnitTest {

    @Mock
    private ArticleMapper articleMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private UserLikeMapper userLikeMapper;
    @Mock
    private UserFavoriteMapper userFavoriteMapper;
    @Mock
    private RedisCacheUtils redisCacheUtils;

    @InjectMocks
    private ArticleDtoAssembler articleDtoAssembler;

    @BeforeEach
    void setUp() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("空输入")
    void emptyInput() {
        assertThat(articleDtoAssembler.batchConvertToDTO(null)).isEmpty();
    }

    @Test
    @DisplayName("批量加载DTO映射 - null或空输入应返回空Map")
    void batchConvertToDTOMap_emptyInput_shouldReturnEmptyMap() {
        assertThat(articleDtoAssembler.batchConvertToDTOMap(null)).isEmpty();
        assertThat(articleDtoAssembler.batchConvertToDTOMap(Collections.emptyList())).isEmpty();
    }

    @Test
    @DisplayName("批量加载DTO映射 - 单批应返回按ID索引的Map")
    void batchConvertToDTOMap_singleBatch_shouldReturnMapKeyedById() {
        Article article = createArticle(1L, "文章", Article.STATUS_PUBLISHED, 2L);
        when(articleMapper.selectBatchIds(anyList())).thenReturn(List.of(article));

        Map<Long, ArticleDTO> result = articleDtoAssembler.batchConvertToDTOMap(List.of(1L));

        assertThat(result).containsOnlyKeys(1L);
        assertThat(result.get(1L).getTitle()).isEqualTo("文章");
    }

    @Test
    @DisplayName("批量加载DTO映射 - 超过500个ID应分批查询")
    void batchConvertToDTOMap_overBatchSize_shouldChunkQueries() {
        List<Long> ids = new ArrayList<>();
        for (long i = 1; i <= 501; i++) {
            ids.add(i);
        }
        when(articleMapper.selectBatchIds(anyList())).thenAnswer(invocation -> {
            List<Long> batch = invocation.getArgument(0);
            List<Article> articles = new ArrayList<>();
            for (Long id : batch) {
                articles.add(createArticle(id, "文章" + id, Article.STATUS_PUBLISHED, 2L));
            }
            return articles;
        });

        Map<Long, ArticleDTO> result = articleDtoAssembler.batchConvertToDTOMap(ids);

        assertThat(result).hasSize(501);
        verify(articleMapper, times(2)).selectBatchIds(anyList());
    }

    @Test
    @DisplayName("批量加载DTO映射 - 重复ID应去重")
    void batchConvertToDTOMap_duplicateIds_shouldDeduplicate() {
        when(articleMapper.selectBatchIds(anyList())).thenReturn(List.of(
                createArticle(1L, "文章1", Article.STATUS_PUBLISHED, 2L),
                createArticle(2L, "文章2", Article.STATUS_PUBLISHED, 2L)));

        Map<Long, ArticleDTO> result = articleDtoAssembler.batchConvertToDTOMap(List.of(1L, 1L, 2L, 2L));

        assertThat(result).hasSize(2);
        ArgumentCaptor<List<Long>> captor = ArgumentCaptor.forClass(List.class);
        verify(articleMapper).selectBatchIds(captor.capture());
        assertThat(captor.getValue()).containsExactly(1L, 2L);
    }

    @Test
    @DisplayName("批量加载DTO映射 - DTO的ID为空应过滤")
    void batchConvertToDTOMap_nullDtoId_shouldFilter() {
        when(articleMapper.selectBatchIds(anyList())).thenReturn(List.of(
                createArticle(1L, "文章", Article.STATUS_PUBLISHED, 2L),
                createArticle(null, "无ID文章", Article.STATUS_PUBLISHED, 2L)));

        Map<Long, ArticleDTO> result = articleDtoAssembler.batchConvertToDTOMap(List.of(1L, 2L));

        assertThat(result).containsOnlyKeys(1L);
    }

    @Test
    @DisplayName("异常分类查询 - 不应抛异常")
    void categoryQueryException() {
        Article article = createArticle(1L, "文章", Article.STATUS_PUBLISHED, 2L);
        when(userMapper.selectBatchIds(any())).thenReturn(List.of(createUser(2L, "作者")));
        when(categoryMapper.selectBatchIds(any())).thenThrow(new RuntimeException());
        setUserId(1L);

        assertThat(articleDtoAssembler.batchConvertToDTO(List.of(article))).hasSize(1);
    }

    private void setUserId(Long userId) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getAttribute("userId")).thenReturn(userId);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
    }

    private Article createArticle(Long id, String title, int status, Long authorId) {
        Article article = new Article();
        article.setId(id);
        article.setTitle(title);
        article.setStatus(status);
        article.setAuthorId(authorId);
        article.setViewCount(0);
        article.setCategoryId(11L);
        return article;
    }

    private User createUser(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setNickname(username);
        return user;
    }
}
