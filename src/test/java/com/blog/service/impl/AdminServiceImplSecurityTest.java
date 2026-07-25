package com.blog.service.impl;

import com.blog.entity.User;
import com.blog.mapper.UserFollowMapper;
import com.blog.mapper.UserMapper;
import com.blog.utils.RedisUtils;
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
    private RedisUtils redisUtils;

    @InjectMocks
    private AdminServiceImpl service;

    @Test
    void updateUserStatus_disabled_shouldRevokeRefreshToken() {
        User user = activeUser();
        when(userMapper.selectById(7L)).thenReturn(user);
        when(userMapper.updateById(user)).thenReturn(1);

        service.updateUserStatus(7L, User.STATUS_DISABLED);

        verify(redisUtils).delete("auth:refresh:user:7");
    }

    @Test
    void deleteUser_shouldRevokeRefreshToken() {
        when(userMapper.selectById(7L)).thenReturn(activeUser());
        when(userFollowMapper.selectList(any())).thenReturn(List.of());
        when(userMapper.deleteById(7L)).thenReturn(1);

        service.deleteUser(7L);

        verify(redisUtils).delete("auth:refresh:user:7");
    }

    private static User activeUser() {
        User user = new User();
        user.setId(7L);
        user.setStatus(User.STATUS_ACTIVE);
        return user;
    }
}
