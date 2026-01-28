package com.blog.service;

import com.blog.dto.UserDTO;
import com.blog.dto.UserLoginDTO;
import com.blog.dto.UserRegisterDTO;
import com.blog.dto.UserUpdateDTO;
import com.blog.dto.ChangePasswordDTO;
import com.blog.common.Result;
import com.blog.entity.User;
import com.blog.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 用户服务测试类
 */
@SpringBootTest
@Transactional
public class UserServiceTest {

    @Autowired
    private UserService userService;

    @MockBean
    private CaptchaService captchaService;

    @BeforeEach
    void setUp() {
        when(captchaService.verifyCaptcha(any(), any())).thenReturn(true);
    }

    @Test
    public void testRegisterUser() {
        UserRegisterDTO registerDTO = new UserRegisterDTO();
        registerDTO.setUsername("testuser");
        registerDTO.setPassword("password123");
        registerDTO.setConfirmPassword("password123");
        registerDTO.setEmail("test@example.com");
        registerDTO.setNickname("测试用户");

        Result<String> result = userService.register(registerDTO);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("注册成功", result.getData());
    }

    @Test
    public void testRegisterUserWithExistingUsername() {
        // 先注册一个用户
        UserRegisterDTO registerDTO1 = new UserRegisterDTO();
        registerDTO1.setUsername("duplicateuser");
        registerDTO1.setPassword("password123");
        registerDTO1.setConfirmPassword("password123");
        registerDTO1.setEmail("test1@example.com");
        registerDTO1.setNickname("测试用户1");
        userService.register(registerDTO1);

        // 尝试用相同的用户名注册
        UserRegisterDTO registerDTO2 = new UserRegisterDTO();
        registerDTO2.setUsername("duplicateuser");
        registerDTO2.setPassword("password456");
        registerDTO2.setConfirmPassword("password456");
        registerDTO2.setEmail("test2@example.com");
        registerDTO2.setNickname("测试用户2");

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.register(registerDTO2));
        assertEquals("用户名已存在", ex.getMessage());
    }

    @Test
    public void testLoginUser() {
        // 先注册用户
        UserRegisterDTO registerDTO = new UserRegisterDTO();
        registerDTO.setUsername("loginuser");
        registerDTO.setPassword("password123");
        registerDTO.setConfirmPassword("password123");
        registerDTO.setEmail("login@example.com");
        registerDTO.setNickname("登录用户");
        userService.register(registerDTO);

        // 测试登录
        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setUsername("loginuser");
        loginDTO.setPassword("password123");

        Result<UserDTO> result = userService.login(loginDTO);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("loginuser", result.getData().getUsername());
    }

    @Test
    public void testLoginUserWithWrongPassword() {
        // 先注册用户
        UserRegisterDTO registerDTO = new UserRegisterDTO();
        registerDTO.setUsername("wrongpassuser");
        registerDTO.setPassword("password123");
        registerDTO.setConfirmPassword("password123");
        registerDTO.setEmail("wrongpass@example.com");
        registerDTO.setNickname("密码错误用户");
        userService.register(registerDTO);

        // 测试错误密码登录
        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setUsername("wrongpassuser");
        loginDTO.setPassword("wrongpassword");

        BusinessException ex = assertThrows(BusinessException.class, () -> userService.login(loginDTO));
        assertEquals("密码错误", ex.getMessage());
    }

    @Test
    public void testGetUserById() {
        // 先注册用户
        UserRegisterDTO registerDTO = new UserRegisterDTO();
        registerDTO.setUsername("getuser");
        registerDTO.setPassword("password123");
        registerDTO.setConfirmPassword("password123");
        registerDTO.setEmail("getuser@example.com");
        registerDTO.setNickname("获取用户");
        userService.register(registerDTO);

        // 获取用户ID（需要先获取用户信息）
        User user = userService.getUserByUsername("getuser");
        Long userId = user.getId();

        // 测试获取用户信息
        Result<UserDTO> result = userService.getUserInfo(userId);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("getuser", result.getData().getUsername());
        assertEquals("getuser@example.com", result.getData().getEmail());
    }

    @Test
    public void testUpdateUser() {
        // 先注册用户
        UserRegisterDTO registerDTO = new UserRegisterDTO();
        registerDTO.setUsername("updateuser");
        registerDTO.setPassword("password123");
        registerDTO.setConfirmPassword("password123");
        registerDTO.setEmail("update@example.com");
        registerDTO.setNickname("更新用户");
        userService.register(registerDTO);

        // 获取用户ID
        User user = userService.getUserByUsername("updateuser");
        Long userId = user.getId();

        // 更新用户信息
        UserUpdateDTO updateDTO = new UserUpdateDTO();
        updateDTO.setNickname("更新后的昵称");
        updateDTO.setEmail("updated@example.com");
        updateDTO.setAvatar("/avatar/new.jpg");

        Result<Void> result = userService.updateUserInfo(userId, updateDTO);

        assertTrue(result.isSuccess());

        // 验证更新后的用户信息
        Result<UserDTO> updatedUserResult = userService.getUserInfo(userId);
        assertTrue(updatedUserResult.isSuccess());
        assertEquals("更新后的昵称", updatedUserResult.getData().getNickname());
        assertEquals("updated@example.com", updatedUserResult.getData().getEmail());
        assertEquals("/avatar/new.jpg", updatedUserResult.getData().getAvatar());
    }

    @Test
    public void testUpdatePassword() {
        // 先注册用户
        UserRegisterDTO registerDTO = new UserRegisterDTO();
        registerDTO.setUsername("updatepassuser");
        registerDTO.setPassword("oldpassword");
        registerDTO.setConfirmPassword("oldpassword");
        registerDTO.setEmail("updatepass@example.com");
        registerDTO.setNickname("更新密码用户");
        userService.register(registerDTO);

        // 获取用户ID
        User user = userService.getUserByUsername("updatepassuser");
        Long userId = user.getId();

        // 更新密码
        ChangePasswordDTO changePasswordDTO = new ChangePasswordDTO();
        changePasswordDTO.setOldPassword("oldpassword");
        changePasswordDTO.setNewPassword("newpassword123");

        Result<Void> result = userService.changePassword(userId, changePasswordDTO);

        assertTrue(result.isSuccess());

        // 验证新密码可以登录
        UserLoginDTO loginDTO = new UserLoginDTO();
        loginDTO.setUsername("updatepassuser");
        loginDTO.setPassword("newpassword123");

        Result<UserDTO> loginResult = userService.login(loginDTO);
        assertTrue(loginResult.isSuccess());
    }

    @Test
    public void testUpdatePasswordWithWrongOldPassword() {
        // 先注册用户
        UserRegisterDTO registerDTO = new UserRegisterDTO();
        registerDTO.setUsername("wrongoldpass");
        registerDTO.setPassword("password123");
        registerDTO.setConfirmPassword("password123");
        registerDTO.setEmail("wrongold@example.com");
        registerDTO.setNickname("旧密码错误用户");
        userService.register(registerDTO);

        // 获取用户ID
        User user = userService.getUserByUsername("wrongoldpass");
        Long userId = user.getId();

        // 尝试用错误的旧密码更新
        ChangePasswordDTO changePasswordDTO = new ChangePasswordDTO();
        changePasswordDTO.setOldPassword("wrongoldpassword");
        changePasswordDTO.setNewPassword("newpassword123");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> userService.changePassword(userId, changePasswordDTO));
        assertEquals("原密码错误", ex.getMessage());
    }

    @Test
    public void testDeleteUser() {
        // 先注册用户
        UserRegisterDTO registerDTO = new UserRegisterDTO();
        registerDTO.setUsername("deleteuser");
        registerDTO.setPassword("password123");
        registerDTO.setConfirmPassword("password123");
        registerDTO.setEmail("delete@example.com");
        registerDTO.setNickname("删除用户");
        userService.register(registerDTO);

        // 获取用户ID
        User user = userService.getUserByUsername("deleteuser");
        Long userId = user.getId();

        // 删除用户
        Result<Void> result = userService.deleteUser(userId);

        assertTrue(result.isSuccess());

        // 验证用户已被删除
        BusinessException ex = assertThrows(BusinessException.class, () -> userService.getUserInfo(userId));
        assertEquals("用户不存在", ex.getMessage());
    }

    @Test
    public void testFollowUnfollowFollow() {
        // 1. 注册两个用户
        UserRegisterDTO reg1 = new UserRegisterDTO();
        reg1.setUsername("follower");
        reg1.setPassword("password123");
        reg1.setConfirmPassword("password123");
        reg1.setEmail("follower@test.com");
        userService.register(reg1);
        Long followerId = userService.getUserByUsername("follower").getId();

        UserRegisterDTO reg2 = new UserRegisterDTO();
        reg2.setUsername("following");
        reg2.setPassword("password123");
        reg2.setConfirmPassword("password123");
        reg2.setEmail("following@test.com");
        userService.register(reg2);
        Long followingId = userService.getUserByUsername("following").getId();

        // 2. 关注
        Result<Void> followRes1 = userService.follow(followerId, followingId);
        assertTrue(followRes1.isSuccess());
        assertTrue(userService.isFollowing(followerId, followingId).getData());

        // 3. 取消关注
        Result<Void> unfollowRes = userService.unfollow(followerId, followingId);
        assertTrue(unfollowRes.isSuccess());
        assertFalse(userService.isFollowing(followerId, followingId).getData());

        // 4. 再次关注（修复前这里会报错 500）
        Result<Void> followRes2 = userService.follow(followerId, followingId);
        assertTrue(followRes2.isSuccess());
        assertTrue(userService.isFollowing(followerId, followingId).getData());
    }

    @Test
    public void testFollowCountBug() {
        // 1. 注册两个用户
        UserRegisterDTO reg1 = new UserRegisterDTO();
        reg1.setUsername("fan");
        reg1.setPassword("Password123!");
        reg1.setConfirmPassword("Password123!");
        reg1.setEmail("fan@test.com");
        userService.register(reg1);
        Long fanId = userService.getUserByUsername("fan").getId();

        UserRegisterDTO reg2 = new UserRegisterDTO();
        reg2.setUsername("star");
        reg2.setPassword("Password123!");
        reg2.setConfirmPassword("Password123!");
        reg2.setEmail("star@test.com");
        userService.register(reg2);
        Long starId = userService.getUserByUsername("star").getId();

        // 初始粉丝数
        int initialFollowers = userService.getUserById(starId).getFollowerCount();
        assertEquals(0, initialFollowers);

        // 2. 第一次关注
        userService.follow(fanId, starId);
        assertEquals(1, userService.getUserById(starId).getFollowerCount());

        // 3. 取消关注
        userService.unfollow(fanId, starId);
        assertEquals(0, userService.getUserById(starId).getFollowerCount());

        // 4. 再次关注 (这里就是 bug 发生的地方)
        // 如果 updateById 失败（因为 MP 加上了 deleted=0 条件），记录仍然是 deleted=1
        // 但是代码继续执行了 incrementFollowerCount，导致粉丝数 +1
        userService.follow(fanId, starId);
        assertEquals(1, userService.getUserById(starId).getFollowerCount());

        // 5. 再次取消关注 (如果步骤4没真正恢复，这里会报错 "未关注该用户")
        // 如果步骤4确实恢复了，这里应该成功并减1
        try {
            userService.unfollow(fanId, starId);
        } catch (BusinessException e) {
            // 如果抛出 "未关注该用户"，说明步骤4恢复失败
            fail("Should be able to unfollow: " + e.getMessage());
        }
        assertEquals(0, userService.getUserById(starId).getFollowerCount());

        // 6. 再次关注 - 检查是否会重复增加
        // 如果之前的逻辑有问题，这里可能会导致计数异常
        userService.follow(fanId, starId);
        assertEquals(1, userService.getUserById(starId).getFollowerCount());
    }
}