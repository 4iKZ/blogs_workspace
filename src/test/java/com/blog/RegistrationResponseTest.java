package com.blog;

import com.blog.common.Result;
import com.blog.controller.UserController;
import com.blog.dto.UserRegisterDTO;
import com.blog.service.TOSService;
import com.blog.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
public class RegistrationResponseTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private TOSService tosService;

    @Test
    public void testRegisterResponseStructure() throws Exception {
        // Mock successful registration
        Mockito.when(userService.register(any(UserRegisterDTO.class)))
                .thenReturn(Result.success("注册成功"));

        String requestBody = "{\"username\":\"testUser\",\"password\":\"password123\",\"confirmPassword\":\"password123\",\"email\":\"test@example.com\",\"captcha\":\"1234\",\"captchaKey\":\"key\"}";

        mockMvc.perform(post("/api/user/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andDo(print())
                .andExpect(status().isOk());
    }
}
