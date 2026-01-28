package com.blog.controller;

import com.blog.dto.UserLoginDTO;
import com.blog.dto.UserRegisterDTO;
import com.blog.dto.UserUpdateDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 用户控制器测试类
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class UserControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Test
        public void testRegisterUser() throws Exception {
                UserRegisterDTO registerDTO = new UserRegisterDTO();
                registerDTO.setUsername("testuser");
                registerDTO.setPassword("password123");
                registerDTO.setEmail("test@example.com");
                registerDTO.setNickname("测试用户");

                mockMvc.perform(post("/api/users/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.username").value("testuser"))
                                .andExpect(jsonPath("$.data.email").value("test@example.com"));
        }

        @Test
        public void testRegisterUserWithDuplicateUsername() throws Exception {
                // 先注册一个用户
                UserRegisterDTO registerDTO1 = new UserRegisterDTO();
                registerDTO1.setUsername("duplicateuser");
                registerDTO1.setPassword("password123");
                registerDTO1.setEmail("test1@example.com");
                registerDTO1.setNickname("测试用户1");

                mockMvc.perform(post("/api/users/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerDTO1)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));

                // 尝试用相同的用户名注册
                UserRegisterDTO registerDTO2 = new UserRegisterDTO();
                registerDTO2.setUsername("duplicateuser");
                registerDTO2.setPassword("password456");
                registerDTO2.setEmail("test2@example.com");
                registerDTO2.setNickname("测试用户2");

                mockMvc.perform(post("/api/users/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerDTO2)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("用户名已存在"));
        }

        @Test
        public void testLoginUser() throws Exception {
                // 先注册用户
                UserRegisterDTO registerDTO = new UserRegisterDTO();
                registerDTO.setUsername("loginuser");
                registerDTO.setPassword("password123");
                registerDTO.setEmail("login@example.com");
                registerDTO.setNickname("登录用户");

                mockMvc.perform(post("/api/users/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerDTO)));

                // 测试登录
                UserLoginDTO loginDTO = new UserLoginDTO();
                loginDTO.setUsername("loginuser");
                loginDTO.setPassword("password123");

                mockMvc.perform(post("/api/users/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.username").value("loginuser"));
        }

        @Test
        public void testLoginUserWithWrongPassword() throws Exception {
                // 先注册用户
                UserRegisterDTO registerDTO = new UserRegisterDTO();
                registerDTO.setUsername("wrongpassuser");
                registerDTO.setPassword("password123");
                registerDTO.setEmail("wrongpass@example.com");
                registerDTO.setNickname("密码错误用户");

                mockMvc.perform(post("/api/users/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerDTO)));

                // 测试错误密码登录
                UserLoginDTO loginDTO = new UserLoginDTO();
                loginDTO.setUsername("wrongpassuser");
                loginDTO.setPassword("wrongpassword");

                mockMvc.perform(post("/api/users/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(false))
                                .andExpect(jsonPath("$.message").value("密码错误"));
        }

        @Test
        public void testGetUserById() throws Exception {
                // 先注册用户
                UserRegisterDTO registerDTO = new UserRegisterDTO();
                registerDTO.setUsername("getuser");
                registerDTO.setPassword("password123");
                registerDTO.setEmail("getuser@example.com");
                registerDTO.setNickname("获取用户");

                String response = mockMvc.perform(post("/api/users/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerDTO)))
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                Long userId = objectMapper.readTree(response).path("data").path("userId").asLong();

                // 测试获取用户信息
                mockMvc.perform(get("/api/users/{userId}", userId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.username").value("getuser"))
                                .andExpect(jsonPath("$.data.email").value("getuser@example.com"));
        }

        @Test
        public void testUpdateUser() throws Exception {
                // 先注册用户
                UserRegisterDTO registerDTO = new UserRegisterDTO();
                registerDTO.setUsername("updateuser");
                registerDTO.setPassword("password123");
                registerDTO.setEmail("update@example.com");
                registerDTO.setNickname("更新用户");

                String response = mockMvc.perform(post("/api/users/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerDTO)))
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                Long userId = objectMapper.readTree(response).path("data").path("userId").asLong();

                // 更新用户信息
                UserUpdateDTO updateDTO = new UserUpdateDTO();
                updateDTO.setNickname("更新后的昵称");
                updateDTO.setEmail("updated@example.com");
                updateDTO.setAvatar("/avatar/new.jpg");

                mockMvc.perform(put("/api/users/{userId}", userId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.nickname").value("更新后的昵称"))
                                .andExpect(jsonPath("$.data.email").value("updated@example.com"))
                                .andExpect(jsonPath("$.data.avatar").value("/avatar/new.jpg"));
        }

        @Test
        public void testDeleteUser() throws Exception {
                // 先注册用户
                UserRegisterDTO registerDTO = new UserRegisterDTO();
                registerDTO.setUsername("deleteuser");
                registerDTO.setPassword("password123");
                registerDTO.setEmail("delete@example.com");
                registerDTO.setNickname("删除用户");

                String response = mockMvc.perform(post("/api/users/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerDTO)))
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                Long userId = objectMapper.readTree(response).path("data").path("userId").asLong();

                // 删除用户
                mockMvc.perform(delete("/api/users/{userId}", userId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));

                // 验证用户已被删除
                mockMvc.perform(get("/api/users/{userId}", userId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        public void testGetPublicUserInfo() throws Exception {
                // 1. 注册用户
                UserRegisterDTO registerDTO = new UserRegisterDTO();
                registerDTO.setUsername("publicuser");
                registerDTO.setPassword("password123");
                registerDTO.setEmail("public@example.com");
                registerDTO.setNickname("Public User");

                mockMvc.perform(post("/api/user/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerDTO)))
                                .andExpect(status().isOk());

                // 2. 登录获取ID
                UserLoginDTO loginDTO = new UserLoginDTO();
                loginDTO.setUsername("publicuser");
                loginDTO.setPassword("password123");

                String loginResponse = mockMvc.perform(post("/api/user/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginDTO)))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                Long userId = objectMapper.readTree(loginResponse).path("data").path("id").asLong();

                // 3. 获取公开信息
                mockMvc.perform(get("/api/user/{userId}", userId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.username").value("publicuser"))
                                .andExpect(jsonPath("$.data.nickname").value("Public User"))
                                // 验证敏感信息不存在或为null
                                .andExpect(jsonPath("$.data.email").isEmpty())
                                .andExpect(jsonPath("$.data.phone").isEmpty())
                                .andExpect(jsonPath("$.data.password").doesNotExist())
                                .andExpect(jsonPath("$.data.accessToken").isEmpty())
                                .andExpect(jsonPath("$.data.refreshToken").isEmpty());
        }
}