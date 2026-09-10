package com.blog.service.impl;

import com.blog.dto.ArticleDTO;
import com.blog.entity.Article;
import com.blog.entity.User;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("文章DTO组装组件测试")
class ArticleDtoAssemblerUnitTest {

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
