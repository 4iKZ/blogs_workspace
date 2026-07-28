package com.blog.service.impl;

import com.blog.entity.User;
import com.blog.entity.Article;
import com.blog.mapper.ArticleMapper;
import com.blog.mapper.UserFollowMapper;
import com.blog.mapper.UserMapper;
import com.blog.service.AuthSessionRevocationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class AdminServiceImplSecurityTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private UserFollowMapper userFollowMapper;

    @Mock
    private AuthSessionRevocationService authSessionRevocationService;

    @InjectMocks
    private AdminServiceImpl service;

    @Test
    void updateUserStatus_disabled_shouldRevokeRefreshToken() {
        User user = activeUser();
        when(userMapper.selectById(7L)).thenReturn(user);
        when(authSessionRevocationService.updateStatusAndRevoke(7L, User.STATUS_DISABLED)).thenReturn(true);

        service.updateUserStatus(7L, User.STATUS_DISABLED);

        verify(authSessionRevocationService).updateStatusAndRevoke(7L, User.STATUS_DISABLED);
    }

    @Test
    void deleteUser_shouldRevokeRefreshToken() {
        when(userMapper.selectById(7L)).thenReturn(activeUser());
        when(userFollowMapper.selectList(any())).thenReturn(List.of());
        when(authSessionRevocationService.incrementVersionAndRevoke(7L)).thenReturn(true);
        when(userMapper.deleteById(7L)).thenReturn(1);

        service.deleteUser(7L);

        verify(authSessionRevocationService).incrementVersionAndRevoke(7L);
    }

    @Test
    void updateArticleStatus_mustNotPublishWithoutModerationDecision() {
        Article article = new Article();
        article.setId(9L);
        article.setStatus(Article.STATUS_DRAFT);
        var result = service.updateArticleStatus(9L, Article.STATUS_PUBLISHED);

        assertThat(result.isSuccess()).isFalse();
        verify(articleMapper, never()).updateById(any());
    }

    private static User activeUser() {
        User user = new User();
        user.setId(7L);
        user.setStatus(User.STATUS_ACTIVE);
        return user;
    }
}
