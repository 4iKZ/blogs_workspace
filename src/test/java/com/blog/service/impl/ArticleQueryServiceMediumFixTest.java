package com.blog.service.impl;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.dto.ArticleDTO;
import com.blog.entity.Article;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.CategoryMapper;
import com.blog.mapper.UserFavoriteMapper;
import com.blog.mapper.UserFollowMapper;
import com.blog.mapper.UserLikeMapper;
import com.blog.mapper.UserMapper;
import com.blog.service.ArticleRankService;
import com.blog.utils.RedisCacheUtils;
import com.blog.utils.RedisUtils;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Medium bug #6 / #8 的聚焦回归测试：
 * #8 popular 快捷路径必须在分页钳位之后执行；
 * #6 推荐缓存键必须按用户隔离。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ArticleQueryServiceMediumFixTest {

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
    private UserFollowMapper userFollowMapper;
    @Mock
    private RedisUtils redisUtils;
    @Mock
    private RedisCacheUtils redisCacheUtils;
    @Mock
    private ArticleRankService articleRankService;

    @InjectMocks
    private ArticleQueryServiceImpl articleQueryService;

    @BeforeEach
    void setUp() {
        RequestContextHolder.resetRequestAttributes();
        SecurityContextHolder.clearContext();

        ArticleDtoAssembler articleDtoAssembler = new ArticleDtoAssembler();
        ReflectionTestUtils.setField(articleDtoAssembler, "userMapper", userMapper);
        ReflectionTestUtils.setField(articleDtoAssembler, "categoryMapper", categoryMapper);
        ReflectionTestUtils.setField(articleDtoAssembler, "userLikeMapper", userLikeMapper);
        ReflectionTestUtils.setField(articleDtoAssembler, "userFavoriteMapper", userFavoriteMapper);
        ReflectionTestUtils.setField(articleDtoAssembler, "redisCacheUtils", redisCacheUtils);
        ReflectionTestUtils.setField(articleQueryService, "articleDtoAssembler", articleDtoAssembler);
    }

    private void setUserId(Long userId) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getAttribute("userId")).thenReturn(userId);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(req));
    }

    @Test
    @DisplayName("#8 popular 快捷路径收到的是钳位后的 page/size")
    void popularSort_clampsPageSizeBeforeShortcut() {
        PageResult<ArticleDTO> hotPage = PageResult.of(Collections.emptyList(), 0L, 1, 100);
        when(articleRankService.getHotArticlesPage(1, 100, "week"))
                .thenReturn(Result.success(hotPage));

        Result<PageResult<ArticleDTO>> result =
                articleQueryService.getArticleList(-5, 100000, null, null, null, null, null, "popular");

        assertThat(result.isSuccess()).isTrue();
        ArgumentCaptor<Integer> pageCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> sizeCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(articleRankService).getHotArticlesPage(pageCaptor.capture(), sizeCaptor.capture(), anyString());
        assertThat(pageCaptor.getValue()).isEqualTo(1);
        assertThat(sizeCaptor.getValue()).isEqualTo(100);
    }

    @Test
    @DisplayName("#6 不同用户的推荐缓存键互相隔离")
    void recommendedArticles_cacheKey_isolatedPerUser() {
        when(articleMapper.selectList(any())).thenReturn(Collections.emptyList());

        setUserId(100L);
        articleQueryService.getRecommendedArticles(10);
        setUserId(200L);
        articleQueryService.getRecommendedArticles(10);

        verify(redisUtils).get("recommended:articles:10:user:100");
        verify(redisUtils).get("recommended:articles:10:user:200");
    }

    @Test
    @DisplayName("#6 同一用户命中自己的推荐缓存，不查库")
    void recommendedArticles_cacheHit_sameUser() {
        ArticleDTO cached = new ArticleDTO();
        cached.setId(1L);
        cached.setTitle("用户100的推荐");
        setUserId(100L);
        when(redisUtils.get("recommended:articles:10:user:100"))
                .thenReturn(Collections.singletonList(cached));

        Result<List<ArticleDTO>> result = articleQueryService.getRecommendedArticles(10);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).hasSize(1);
        assertThat(result.getData().get(0).getTitle()).isEqualTo("用户100的推荐");
        verify(articleMapper, never()).selectList(any());
    }
}
