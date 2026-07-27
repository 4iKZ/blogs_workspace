package com.blog.service.impl;

import com.blog.common.Result;
import com.blog.dto.ModerationResult;
import com.blog.service.ContentModerationService;
import com.blog.utils.BusinessUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI内容审核服务实现 - 基于DeepSeek API
 */
@Service
@Slf4j
public class ContentModerationServiceImpl implements ContentModerationService {

    @Value("${spring.ai.openai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.openai.api-key}")
    private String apiKey;

    @Value("${spring.ai.openai.chat.options.model:deepseek-chat}")
    private String model;

    @Autowired
    @Qualifier("moderationRestTemplate")
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String ARTICLE_MODERATION_PROMPT = """
你是一个严格的内容安全审核员。请审核以下文章内容，判断是否包含违规信息。

审核维度：
1. 暴力血腥 - 描述暴力行为、武器使用等
2. 仇恨言论 - 种族、性别、地域歧视等
3. 政治敏感 - 领导人不当提及、敏感事件等
4. 色情低俗 - 性暗示、裸露描写等
5. 虚假信息 - 未经证实的重大事件
6. 广告推广 - 硬广告、导流信息等

文章标题：%s
文章内容：%s

请以JSON格式返回审核结果：
{
    "passed": true/false,
    "violationType": "none/violence/hate/politics/porn/fake/ad/other",
    "reasons": ["违规原因1", "违规原因2"],
    "confidence": 0.95,
    "suggestion": "修改建议（如有）"
}
""";

    private static final String COMMENT_MODERATION_PROMPT = """
你是一个严格的内容安全审核员。请审核以下评论内容，判断是否包含违规信息。

审核维度：
1. 暴力血腥 - 描述暴力行为、武器使用等
2. 仇恨言论 - 种族、性别、地域歧视等
3. 政治敏感 - 领导人不当提及、敏感事件等
4. 色情低俗 - 性暗示、裸露描写等
5. 虚假信息 - 未经证实的重大事件
6. 广告推广 - 硬广告、导流信息等

评论内容：%s

请以JSON格式返回审核结果：
{
    "passed": true/false,
    "violationType": "none/violence/hate/politics/porn/fake/ad/other",
    "reasons": ["违规原因1", "违规原因2"],
    "confidence": 0.95,
    "suggestion": "修改建议（如有）"
}
""";

    @Override
    public Result<ModerationResult> moderateArticle(String title, String content) {
        if (title == null) {
            title = "";
        }
        if (content == null) {
            content = "";
        }

        // 截断过长的内容（DeepSeek输入有长度限制）
        if (content.length() > 4000) {
            content = content.substring(0, 4000);
        }

        String promptText = String.format(ARTICLE_MODERATION_PROMPT, title, content);
        return doModerate(promptText);
    }

    @Override
    public Result<ModerationResult> moderateComment(String content) {
        if (content == null) {
            content = "";
        }

        // 截断过长的评论
        if (content.length() > 1000) {
            content = content.substring(0, 1000);
        }

        String promptText = String.format(COMMENT_MODERATION_PROMPT, content);
        return doModerate(promptText);
    }

    @Override
    public Result<ModerationResult> moderate(String content) {
        if (content == null) {
            content = "";
        }

        // 截断过长的内容
        if (content.length() > 2000) {
            content = content.substring(0, 2000);
        }

        String promptText = "你是一个严格的内容安全审核员。请审核以下内容，判断是否包含违规信息。\n\n内容：" + content + "\n\n请以JSON格式返回审核结果：\n{\n    \"passed\": true/false,\n    \"violationType\": \"none/violence/hate/politics/porn/fake/ad/other\",\n    \"reasons\": [\"违规原因1\", \"违规原因2\"],\n    \"confidence\": 0.95,\n    \"suggestion\": \"修改建议（如有）\"\n}";
        return doModerate(promptText);
    }

    private Result<ModerationResult> doModerate(String promptText) {
        try {
            log.info("开始AI内容审核，Prompt长度: {}", promptText.length());

            // 构建请求
            String url = baseUrl + "/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(
                    Map.of("role", "user", "content", promptText)
            ));
            requestBody.put("temperature", 0.1);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            // 发送请求
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("AI审核响应: {}", response.getBody());
                ModerationResult result = parseResponse(response.getBody());
                if (result == null) {
                    return BusinessUtils.error("AI审核响应解析失败");
                }
                return BusinessUtils.success(result);
            } else {
                log.error("AI审核请求失败: {}", response.getStatusCode());
                return BusinessUtils.error("AI审核请求失败: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("AI内容审核异常", e);
            // 审核服务异常时返回错误，不阻止用户发布
            return BusinessUtils.error("AI审核服务异常: " + e.getMessage());
        }
    }

    private ModerationResult parseResponse(String jsonStr) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonStr);

            // 解析DeepSeek的响应格式
            JsonNode choicesNode = rootNode.path("choices");
            if (choicesNode.isArray() && choicesNode.size() > 0) {
                JsonNode messageNode = choicesNode.get(0).path("message");
                String content = messageNode.path("content").asText();

                // 解析content中的JSON
                return parseModerationResult(content);
            }

            // 备用：直接从root解析
            return parseModerationResult(jsonStr);

        } catch (Exception e) {
            log.error("解析AI审核响应失败: {}", jsonStr, e);
            return null;
        }
    }

    private ModerationResult parseModerationResult(String content) {
        try {
            // 清理可能有的markdown代码块
            content = content.trim();
            if (content.startsWith("```json")) {
                content = content.substring(7);
            }
            if (content.startsWith("```")) {
                content = content.substring(3);
            }
            if (content.endsWith("```")) {
                content = content.substring(0, content.length() - 3);
            }
            content = content.trim();

            JsonNode node = objectMapper.readTree(content);

            boolean passed = node.has("passed") && node.get("passed").asBoolean();
            String violationType = node.has("violationType") ? node.get("violationType").asText() : "none";
            double confidence = node.has("confidence") ? node.get("confidence").asDouble() : 0.0;
            String suggestion = node.has("suggestion") && !node.get("suggestion").isNull() ? node.get("suggestion").asText() : null;

            List<String> reasons = new ArrayList<>();
            if (node.has("reasons") && node.get("reasons").isArray()) {
                node.get("reasons").forEach(n -> reasons.add(n.asText()));
            }

            return new ModerationResult(passed, violationType, reasons, confidence, suggestion);

        } catch (Exception e) {
            log.error("解析审核结果内容失败: {}", content, e);
            return null;
        }
    }
}
