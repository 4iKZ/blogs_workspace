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
import org.springframework.security.test.context.support.WithMockUser;
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
                registerDTO.setPassword("Password123!");
                registerDTO.setConfirmPassword("Password123!");
                registerDTO.setEmail("test@example.com");
                registerDTO.setNickname("测试用户");
                registerDTO.setEmailCode("123456");

                mockMvc.perform(post("/api/user/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        public void testRegisterUserWithDuplicateUsername() throws Exception {
                UserRegisterDTO registerDTO1 = new UserRegisterDTO();
                registerDTO1.setUsername("duplicateuser");
                registerDTO1.setPassword("Password123!");
                registerDTO1.setConfirmPassword("Password123!");
                registerDTO1.setEmail("test1@example.com");
                registerDTO1.setNickname("测试用户1");
                registerDTO1.setEmailCode("123456");

                mockMvc.perform(post("/api/user/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerDTO1)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));

                UserRegisterDTO registerDTO2 = new UserRegisterDTO();
                registerDTO2.setUsername("duplicateuser");
                registerDTO2.setPassword("Password456!");
                registerDTO2.setConfirmPassword("Password456!");
                registerDTO2.setEmail("test2@example.com");
                registerDTO2.setNickname("测试用户2");
                registerDTO2.setEmailCode("123456");

                mockMvc.perform(post("/api/user/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerDTO2)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        public void testLoginUser() throws Exception {
                UserRegisterDTO registerDTO = new UserRegisterDTO();
                registerDTO.setUsername("loginuser");
                registerDTO.setPassword("Password123!");
                registerDTO.setConfirmPassword("Password123!");
                registerDTO.setEmail("login@example.com");
                registerDTO.setNickname("登录用户");
                registerDTO.setEmailCode("123456");

                mockMvc.perform(post("/api/user/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerDTO)));

                UserLoginDTO loginDTO = new UserLoginDTO();
                loginDTO.setUsername("loginuser");
                loginDTO.setPassword("Password123!");

                mockMvc.perform(post("/api/user/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.username").value("loginuser"));
        }

        @Test
        public void testLoginUserWithWrongPassword() throws Exception {
                UserRegisterDTO registerDTO = new UserRegisterDTO();
                registerDTO.setUsername("wrongpassuser");
                registerDTO.setPassword("Password123!");
                registerDTO.setConfirmPassword("Password123!");
                registerDTO.setEmail("wrongpass@example.com");
                registerDTO.setNickname("密码错误用户");
                registerDTO.setEmailCode("123456");

                mockMvc.perform(post("/api/user/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerDTO)));

                UserLoginDTO loginDTO = new UserLoginDTO();
                loginDTO.setUsername("wrongpassuser");
                loginDTO.setPassword("WrongPassword123!");

                mockMvc.perform(post("/api/user/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        public void testGetUserById() throws Exception {
                UserRegisterDTO registerDTO = new UserRegisterDTO();
                registerDTO.setUsername("getuser");
                registerDTO.setPassword("Password123!");
                registerDTO.setConfirmPassword("Password123!");
                registerDTO.setEmail("getuser@example.com");
                registerDTO.setNickname("获取用户");
                registerDTO.setEmailCode("123456");

                String response = mockMvc.perform(post("/api/user/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerDTO)))
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                Long userId = objectMapper.readTree(response).path("data").asLong();

                mockMvc.perform(get("/api/user/{userId}", userId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.username").value("getuser"));
        }

        @Test
        @WithMockUser(roles = "user")
        public void testUpdateUser() throws Exception {
                UserUpdateDTO updateDTO = new UserUpdateDTO();
                updateDTO.setNickname("更新后的昵称");
                updateDTO.setEmail("updated@example.com");
                updateDTO.setAvatar("/avatar/new.jpg");

                mockMvc.perform(put("/api/user/info")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateDTO)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithMockUser(roles = "user")
        public void testDeleteUser() throws Exception {
                mockMvc.perform(delete("/api/user/info"))
                                .andExpect(status().isOk());
        }

        @Test
        public void testGetPublicUserInfo() throws Exception {
                UserRegisterDTO registerDTO = new UserRegisterDTO();
                registerDTO.setUsername("publicuser");
                registerDTO.setPassword("Password123!");
                registerDTO.setConfirmPassword("Password123!");
                registerDTO.setEmail("public@example.com");
                registerDTO.setNickname("Public User");
                registerDTO.setEmailCode("123456");

                mockMvc.perform(post("/api/user/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(registerDTO)))
                                .andExpect(status().isOk());

                UserLoginDTO loginDTO = new UserLoginDTO();
                loginDTO.setUsername("publicuser");
                loginDTO.setPassword("Password123!");

                String loginResponse = mockMvc.perform(post("/api/user/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(loginDTO)))
                                .andExpect(status().isOk())
                                .andReturn()
                                .getResponse()
                                .getContentAsString();

                Long userId = objectMapper.readTree(loginResponse).path("data").path("id").asLong();

                mockMvc.perform(get("/api/user/{userId}", userId))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.success").value(true))
                                .andExpect(jsonPath("$.data.username").value("publicuser"))
                                .andExpect(jsonPath("$.data.nickname").value("Public User"))
                                .andExpect(jsonPath("$.data.password").doesNotExist());
        }
}
