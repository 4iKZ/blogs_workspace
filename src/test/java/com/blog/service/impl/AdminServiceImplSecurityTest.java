package com.blog.service.impl;

import com.blog.entity.User;
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

@ExtendWith(MockitoExtension.class)
class AdminServiceImplSecurityTest {

    @Mock
    private UserMapper userMapper;

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

    private static User activeUser() {
        User user = new User();
        user.setId(7L);
        user.setStatus(User.STATUS_ACTIVE);
        return user;
    }
}
