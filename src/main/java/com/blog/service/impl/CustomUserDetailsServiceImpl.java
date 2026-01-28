package com.blog.service.impl;

import com.blog.entity.User;
import com.blog.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * 自定义UserDetailsService实现
 * 从数据库获取用户信息，构建UserDetails对象
 */
@Service
public class CustomUserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 从数据库查询用户
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }
        
        // 构建UserDetails对象
        // 将Integer类型的role转换为String类型的角色名
        String roleName;
        switch (user.getRole()) {
            case 2:
            case 3:
                roleName = "admin";
                break;
            default:
                roleName = "user";
        }
        return org.springframework.security.core.userdetails.User.builder()
            .username(user.getUsername())
            .password(user.getPassword())
            .roles(roleName)
            .build();
    }
}
