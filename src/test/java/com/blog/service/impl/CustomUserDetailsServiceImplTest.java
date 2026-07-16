package com.blog.service.impl;

import com.blog.entity.ArticleModerationLog;
import com.blog.entity.User;
import com.blog.mapper.ArticleModerationLogMapper;
import com.blog.mapper.UserMapper;
import com.blog.dto.ModerationResult;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CustomUserDetailsServiceImplTest {

    private final CustomUserDetailsServiceImpl service = new CustomUserDetailsServiceImpl();

    @Test
    void loadUserByUsername_userNotFound_shouldThrow() {
        UserMapper mapper = mock(UserMapper.class);
        when(mapper.selectByUsername(anyString())).thenReturn(null);
        setField(service, "userMapper", mapper);

        assertThatThrownBy(() -> service.loadUserByUsername("nobody"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("用户不存在");
    }

    @Test
    void loadUserByUsername_roleUser_shouldMapToUser() {
        UserMapper mapper = mock(UserMapper.class);
        User user = new User();
        user.setUsername("alice");
        user.setPassword("encoded");
        user.setRole(1);
        when(mapper.selectByUsername("alice")).thenReturn(user);
        setField(service, "userMapper", mapper);

        var userDetails = service.loadUserByUsername("alice");

        assertThat(userDetails.getUsername()).isEqualTo("alice");
        assertThat(userDetails.getPassword()).isEqualTo("encoded");
        assertThat(userDetails.getAuthorities()).extracting(a -> a.getAuthority()).containsExactly("ROLE_user");
    }

    @Test
    void loadUserByUsername_roleAdmin_shouldMapToAdmin() {
        UserMapper mapper = mock(UserMapper.class);
        User user = new User();
        user.setUsername("admin");
        user.setPassword("encoded");
        user.setRole(2);
        when(mapper.selectByUsername("admin")).thenReturn(user);
        setField(service, "userMapper", mapper);

        var userDetails = service.loadUserByUsername("admin");

        assertThat(userDetails.getAuthorities()).extracting(a -> a.getAuthority()).containsExactly("ROLE_admin");
    }

    @Test
    void loadUserByUsername_roleSuperAdmin_shouldMapToAdmin() {
        UserMapper mapper = mock(UserMapper.class);
        User user = new User();
        user.setUsername("super");
        user.setPassword("encoded");
        user.setRole(3);
        when(mapper.selectByUsername("super")).thenReturn(user);
        setField(service, "userMapper", mapper);

        var userDetails = service.loadUserByUsername("super");

        assertThat(userDetails.getAuthorities()).extracting(a -> a.getAuthority()).containsExactly("ROLE_admin");
    }

    private static void setField(CustomUserDetailsServiceImpl target, String fieldName, Object value) {
        try {
            var field = CustomUserDetailsServiceImpl.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
