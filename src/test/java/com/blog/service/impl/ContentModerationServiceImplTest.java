package com.blog.service.impl;

import com.blog.common.Result;
import com.blog.dto.ModerationResult;
import com.blog.service.ContentModerationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AI内容审核服务测试")
public class ContentModerationServiceImplTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private ContentModerationServiceImpl service;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "baseUrl", "http://test-ai");
        ReflectionTestUtils.setField(service, "apiKey", "test-key");
        ReflectionTestUtils.setField(service, "model", "deepseek-chat");
        ReflectionTestUtils.setField(service, "objectMapper", objectMapper);
    }

    @Nested
    @DisplayName("moderateArticle 测试")
    class ModerateArticleTests {

        @Test
        @DisplayName("null标题和内容应默认使用空字符串")
        void nullTitleAndContent_shouldUseEmptyString() {
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(new ResponseEntity<>("{\"choices\":[]}", HttpStatus.OK));

            Result<ModerationResult> result = service.moderateArticle(null, null);

            assertThat(result.isSuccess()).isTrue();
            verify(restTemplate).postForEntity(eq("http://test-ai/chat/completions"), any(HttpEntity.class), eq(String.class));
        }

        @Test
        @DisplayName("内容超过审核上限时应拒绝，不能只审核前缀")
        void contentTooLong_shouldBeRejectedWithoutCallingAi() {
            String longContent = "z".repeat(5000);

            Result<ModerationResult> result = service.moderateArticle("title", longContent);

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("超过审核上限");
            verifyNoInteractions(restTemplate);
        }

        @Test
        @DisplayName("内容刚好4000字符时不截断")
        void contentExactly4000_shouldNotTruncate() {
            String content = "z".repeat(4000);
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(new ResponseEntity<>("{\"choices\":[]}", HttpStatus.OK));

            Result<ModerationResult> result = service.moderateArticle("title", content);

            assertThat(result.isSuccess()).isTrue();
            ArgumentCaptor<HttpEntity<?>> captor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(restTemplate).postForEntity(anyString(), captor.capture(), eq(String.class));
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
            List<Map<String, Object>> messages = (List<Map<String, Object>>) body.get("messages");
            String promptContent = (String) messages.get(0).get("content");
            long zCount = promptContent.chars().filter(ch -> ch == 'z').count();
            assertThat(zCount).isEqualTo(4000);
        }
    }

    @Nested
    @DisplayName("moderateComment 测试")
    class ModerateCommentTests {

        @Test
        @DisplayName("null内容应默认使用空字符串")
        void nullContent_shouldUseEmptyString() {
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(new ResponseEntity<>("{\"choices\":[]}", HttpStatus.OK));

            Result<ModerationResult> result = service.moderateComment(null);

            assertThat(result.isSuccess()).isTrue();
            verify(restTemplate).postForEntity(eq("http://test-ai/chat/completions"), any(HttpEntity.class), eq(String.class));
        }

        @Test
        @DisplayName("内容超长时应截断到1000字符")
        void contentTooLong_shouldTruncateTo1000() {
            String longContent = "z".repeat(2000);
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(new ResponseEntity<>("{\"choices\":[]}", HttpStatus.OK));

            Result<ModerationResult> result = service.moderateComment(longContent);

            assertThat(result.isSuccess()).isTrue();
            ArgumentCaptor<HttpEntity<?>> captor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(restTemplate).postForEntity(anyString(), captor.capture(), eq(String.class));
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
            List<Map<String, Object>> messages = (List<Map<String, Object>>) body.get("messages");
            String promptContent = (String) messages.get(0).get("content");
            long zCount = promptContent.chars().filter(ch -> ch == 'z').count();
            assertThat(zCount).isEqualTo(1000);
        }

        @Test
        @DisplayName("内容刚好1000字符时不截断")
        void contentExactly1000_shouldNotTruncate() {
            String content = "z".repeat(1000);
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(new ResponseEntity<>("{\"choices\":[]}", HttpStatus.OK));

            Result<ModerationResult> result = service.moderateComment(content);

            assertThat(result.isSuccess()).isTrue();
            ArgumentCaptor<HttpEntity<?>> captor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(restTemplate).postForEntity(anyString(), captor.capture(), eq(String.class));
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
            List<Map<String, Object>> messages = (List<Map<String, Object>>) body.get("messages");
            String promptContent = (String) messages.get(0).get("content");
            long zCount = promptContent.chars().filter(ch -> ch == 'z').count();
            assertThat(zCount).isEqualTo(1000);
        }
    }

    @Nested
    @DisplayName("moderate 测试")
    class ModerateTests {

        @Test
        @DisplayName("内容超长时应截断到2000字符")
        void contentTooLong_shouldTruncateTo2000() {
            String longContent = "z".repeat(3000);
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(new ResponseEntity<>("{\"choices\":[]}", HttpStatus.OK));

            Result<ModerationResult> result = service.moderate(longContent);

            assertThat(result.isSuccess()).isTrue();
            ArgumentCaptor<HttpEntity<?>> captor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(restTemplate).postForEntity(anyString(), captor.capture(), eq(String.class));
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
            List<Map<String, Object>> messages = (List<Map<String, Object>>) body.get("messages");
            String promptContent = (String) messages.get(0).get("content");
            long zCount = promptContent.chars().filter(ch -> ch == 'z').count();
            assertThat(zCount).isEqualTo(2000);
        }

        @Test
        @DisplayName("内容刚好2000字符时不截断")
        void contentExactly2000_shouldNotTruncate() {
            String content = "z".repeat(2000);
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(new ResponseEntity<>("{\"choices\":[]}", HttpStatus.OK));

            Result<ModerationResult> result = service.moderate(content);

            assertThat(result.isSuccess()).isTrue();
            ArgumentCaptor<HttpEntity<?>> captor = ArgumentCaptor.forClass(HttpEntity.class);
            verify(restTemplate).postForEntity(anyString(), captor.capture(), eq(String.class));
            @SuppressWarnings("unchecked")
            Map<String, Object> body = (Map<String, Object>) captor.getValue().getBody();
            List<Map<String, Object>> messages = (List<Map<String, Object>>) body.get("messages");
            String promptContent = (String) messages.get(0).get("content");
            long zCount = promptContent.chars().filter(ch -> ch == 'z').count();
            assertThat(zCount).isEqualTo(2000);
        }

        @Test
        @DisplayName("HTTP 200 且响应正常时应返回成功")
        void httpOk_shouldReturnSuccess() throws Exception {
            String innerJson = objectMapper.writeValueAsString(ModerationResult.pass());
            String responseJson = objectMapper.writeValueAsString(Map.of(
                    "choices", List.of(Map.of("message", Map.of("content", innerJson)))
            ));

            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(responseJson, HttpStatus.OK));

            Result<ModerationResult> result = service.moderate("正常内容");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isNotNull();
            assertThat(result.getData().isPassed()).isTrue();
            assertThat(result.getData().getViolationType()).isEqualTo("none");
            assertThat(result.getData().getConfidence()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("HTTP 非 200 时应返回错误")
        void httpNotOk_shouldReturnError() {
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(new ResponseEntity<>("error", HttpStatus.INTERNAL_SERVER_ERROR));

            Result<ModerationResult> result = service.moderate("内容");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("AI审核请求失败");
        }

        @Test
        @DisplayName("RestTemplate 抛出异常时应返回错误")
        void restTemplateException_shouldReturnError() {
            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenThrow(new RuntimeException("connection failed"));

            Result<ModerationResult> result = service.moderate("内容");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("AI审核服务异常");
        }

        @Test
        @DisplayName("响应包含 markdown 代码块时应 stripping 后解析")
        void responseWithMarkdownJson_shouldStripAndParse() throws Exception {
            String innerJson = objectMapper.writeValueAsString(
                    new ModerationResult(true, "none", List.of(), 1.0, null)
            );
            String wrappedJson = "```json\n" + innerJson + "\n```";
            String responseJson = objectMapper.writeValueAsString(Map.of(
                    "choices", List.of(Map.of("message", Map.of("content", wrappedJson)))
            ));

            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(responseJson, HttpStatus.OK));

            Result<ModerationResult> result = service.moderate("内容");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().isPassed()).isTrue();
        }

        @Test
        @DisplayName("响应 choices 为空数组时应回退解析根节点")
        void emptyChoicesArray_shouldFallbackToRoot() throws Exception {
            String responseJson = objectMapper.writeValueAsString(Map.of("choices", List.of()));

            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(responseJson, HttpStatus.OK));

            Result<ModerationResult> result = service.moderate("内容");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().isPassed()).isFalse();
        }

        @Test
        @DisplayName("响应 message.content 为空时应返回失败")
        void emptyMessageContent_shouldReturnFailed() throws Exception {
            String responseJson = objectMapper.writeValueAsString(Map.of(
                    "choices", List.of(Map.of("message", Map.of()))
            ));

            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(responseJson, HttpStatus.OK));

            Result<ModerationResult> result = service.moderate("内容");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().isPassed()).isFalse();
        }

        @Test
        @DisplayName("响应 JSON 格式异常时必须返回错误，禁止默认通过")
        void malformedJson_shouldReturnError() throws Exception {
            String responseJson = objectMapper.writeValueAsString(Map.of(
                    "choices", List.of(Map.of("message", Map.of("content", "not json at all")))
            ));

            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(responseJson, HttpStatus.OK));

            Result<ModerationResult> result = service.moderate("内容");

            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getData()).isNull();
        }

        @Test
        @DisplayName("部分字段缺失时应使用默认值")
        void partialFields_shouldUseDefaults() throws Exception {
            ModerationResult expected = new ModerationResult(false, "none", List.of("spam"), 0.0, null);
            String innerJson = objectMapper.writeValueAsString(expected);
            String responseJson = objectMapper.writeValueAsString(Map.of(
                    "choices", List.of(Map.of("message", Map.of("content", innerJson)))
            ));

            when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(String.class)))
                    .thenReturn(new ResponseEntity<>(responseJson, HttpStatus.OK));

            Result<ModerationResult> result = service.moderate("内容");

            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData().isPassed()).isFalse();
            assertThat(result.getData().getViolationType()).isEqualTo("none");
            assertThat(result.getData().getReasons()).containsExactly("spam");
            assertThat(result.getData().getConfidence()).isEqualTo(0.0);
            assertThat(result.getData().getSuggestion()).isNull();
        }
    }
}
