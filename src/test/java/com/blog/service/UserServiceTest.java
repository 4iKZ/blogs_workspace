package com.blog.service;

import com.blog.dto.UserDTO;
import com.blog.dto.UserLoginDTO;
import com.blog.dto.UserRegisterDTO;
import com.blog.dto.UserUpdateDTO;
import com.blog.dto.ChangePasswordDTO;
import com.blog.common.Result;
import com.blog.entity.User;
import com.blog.exception.BusinessException;
import com.blog.utils.RedisUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import org.junit.jupiter.api.Disabled;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 用户服务测试类
 */
@SpringBootTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class UserServiceTest {

    @Autowired
    private UserService userService;

    @MockBean
    private CaptchaService captchaService;

    @MockBean
    private RedisUtils redisUtils;

    @MockBean
    private com.blog.utils.RedisDistributedLock redisDistributedLock;

    private static final String TEST_EMAIL_CODE = "123456";

    @BeforeEach
    void setUp() {
        when(captchaService.verifyCaptcha(any(), any())).thenReturn(true);
        when(redisUtils.get(anyString())).thenReturn(TEST_EMAIL_CODE);
        when(redisUtils.set(anyString(), any(), anyLong(), any())).thenReturn(true);
        when(redisDistributedLock.tryLock(anyString(), anyLong(), any())).thenReturn("lock-value");
        when(redisDistributedLock.tryLock(anyString(), anyLong(), any(), anyLong(), any())).thenReturn("lock-value");
        doNothing().when(redisDistributedLock).releaseLock(anyString(), anyString());
    }

    private UserRegisterDTO createRegisterDTO(String username, String email, String nickname) {
        UserRegisterDTO dto = new UserRegisterDTO();
        dto.setUsername(username);
        dto.setPassword("Password123!");
        dto.setConfirmPassword("Password123!");
        dto.setEmail(email);
        dto.setNickname(nickname);
        dto.setEmailCode(TEST_EMAIL_CODE);
        return dto;
    }

    @Test
    public void testRegisterUser() {
        Result<String> result = userService.register(
                createRegisterDTO("testuser", "test@example.com", "测试用户"));
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
    }

    @Test
    public void testRegisterUserWithExistingUsername() {
        userService.register(createRegisterDTO("duplicateuser", "test1@example.com", "测试用户1"));
        BusinessException ex = assertThrows(BusinessException.class, () ->
                userService.register(createRegisterDTO("duplicateuser", "test2@example.com", "测试用户2")));
        assertEquals("用户名已存在", ex.getMessage());
    }

    @Test
    public void testLoginUser() {
        userService.register(createRegisterDTO("loginuser", "login@example.com", "登录用户"));
        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setUsername("loginuser");
        loginDTO.setPassword("Password123!");
        Result<UserDTO> result = userService.login(loginDTO);
        assertTrue(result.isSuccess());
        assertEquals("loginuser", result.getData().getUsername());
    }

    @Test
    public void testLoginUserWithWrongPassword() {
        userService.register(createRegisterDTO("wrongpassuser", "wrongpass@example.com", "密码错误用户"));
        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setUsername("wrongpassuser");
        loginDTO.setPassword("WrongPassword123!");
        BusinessException ex = assertThrows(BusinessException.class, () -> userService.login(loginDTO));
        assertEquals("密码错误", ex.getMessage());
    }

    @Test
    public void testGetUserById() {
        userService.register(createRegisterDTO("getuser", "getuser@example.com", "获取用户"));
        User user = userService.getUserByUsername("getuser");
        Result<UserDTO> result = userService.getUserInfo(user.getId());
        assertTrue(result.isSuccess());
        assertEquals("getuser", result.getData().getUsername());
        assertEquals("getuser@example.com", result.getData().getEmail());
    }

    @Test
    public void testUpdateUser() {
        userService.register(createRegisterDTO("updateuser", "update@example.com", "更新用户"));
        User user = userService.getUserByUsername("updateuser");
        UserUpdateDTO updateDTO = new UserUpdateDTO();
        updateDTO.setNickname("更新后的昵称");
        updateDTO.setEmail("updated@example.com");
        updateDTO.setAvatar("/avatar/new.jpg");
        assertTrue(userService.updateUserInfo(user.getId(), updateDTO).isSuccess());
        Result<UserDTO> updated = userService.getUserInfo(user.getId());
        assertEquals("更新后的昵称", updated.getData().getNickname());
        assertEquals("updated@example.com", updated.getData().getEmail());
    }

    @Test
    public void testUpdatePassword() {
        userService.register(createRegisterDTO("updatepassuser", "updatepass@example.com", "更新密码用户"));
        User user = userService.getUserByUsername("updatepassuser");
        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("Password123!");
        dto.setNewPassword("NewPassword123!");
        assertTrue(userService.changePassword(user.getId(), dto, null).isSuccess());
        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setUsername("updatepassuser");
        loginDTO.setPassword("NewPassword123!");
        assertTrue(userService.login(loginDTO).isSuccess());
    }

    @Test
    public void testUpdatePasswordWithWrongOldPassword() {
        userService.register(createRegisterDTO("wrongoldpass", "wrongold@example.com", "旧密码错误用户"));
        User user = userService.getUserByUsername("wrongoldpass");
        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setOldPassword("WrongOldPassword123!");
        dto.setNewPassword("NewPassword123!");
        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.changePassword(user.getId(), dto, null));
        assertEquals("原密码错误", ex.getMessage());
    }

    @Test
    public void testDeleteUser() {
        userService.register(createRegisterDTO("deleteuser", "delete@example.com", "删除用户"));
        User user = userService.getUserByUsername("deleteuser");
        assertTrue(userService.deleteUser(user.getId()).isSuccess());
        BusinessException ex = assertThrows(BusinessException.class, () -> userService.getUserInfo(user.getId()));
        assertEquals("用户不存在", ex.getMessage());
    }

    @Test
    public void testFollowUnfollowFollow() {
        userService.register(createRegisterDTO("follower", "follower@test.com", "follower"));
        userService.register(createRegisterDTO("following", "following@test.com", "following"));
        Long followerId = userService.getUserByUsername("follower").getId();
        Long followingId = userService.getUserByUsername("following").getId();
        assertTrue(userService.follow(followerId, followingId).isSuccess());
        assertTrue(userService.isFollowing(followerId, followingId).getData());
        assertTrue(userService.unfollow(followerId, followingId).isSuccess());
        assertFalse(userService.isFollowing(followerId, followingId).getData());
        assertTrue(userService.follow(followerId, followingId).isSuccess());
        assertTrue(userService.isFollowing(followerId, followingId).getData());
    }

    @Test
    @Disabled("Follow count uses TransactionSynchronization which requires full transaction management")
    public void testFollowCountBug() {
        userService.register(createRegisterDTO("fan", "fan@test.com", "fan"));
        userService.register(createRegisterDTO("star", "star@test.com", "star"));
        Long fanId = userService.getUserByUsername("fan").getId();
        Long starId = userService.getUserByUsername("star").getId();
        assertEquals(0, userService.getUserById(starId).getFollowerCount());
        userService.follow(fanId, starId);
        assertEquals(1, userService.getUserById(starId).getFollowerCount());
        userService.unfollow(fanId, starId);
        assertEquals(0, userService.getUserById(starId).getFollowerCount());
        userService.follow(fanId, starId);
        assertEquals(1, userService.getUserById(starId).getFollowerCount());
        try {
            userService.unfollow(fanId, starId);
        } catch (BusinessException e) {
            fail("Should be able to unfollow: " + e.getMessage());
        }
        assertEquals(0, userService.getUserById(starId).getFollowerCount());
        userService.follow(fanId, starId);
        assertEquals(1, userService.getUserById(starId).getFollowerCount());
    }
}