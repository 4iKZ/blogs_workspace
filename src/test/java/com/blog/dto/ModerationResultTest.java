package com.blog.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ModerationResult 单元测试")
class ModerationResultTest {

    @Test
    @DisplayName("默认构造方法")
    void testDefaultConstructor() {
        ModerationResult result = new ModerationResult();
        assertFalse(result.isPassed());
    }

    @Test
    @DisplayName("全参数构造方法")
    void testFullConstructor() {
        boolean passed = true;
        String violationType = "none";
        List<String> reasons = List.of("理由1", "理由2");
        double confidence = 0.95;
        String suggestion = "建议";

        ModerationResult result = new ModerationResult(passed, violationType, reasons, confidence, suggestion);

        assertTrue(result.isPassed());
        assertEquals("none", result.getViolationType());
        assertEquals(2, result.getReasons().size());
        assertEquals(0.95, result.getConfidence());
        assertEquals("建议", result.getSuggestion());
    }

    @Test
    @DisplayName("pass 工厂方法创建通过结果")
    void testPassFactory() {
        ModerationResult result = ModerationResult.pass();

        assertTrue(result.isPassed());
        assertEquals("none", result.getViolationType());
        assertTrue(result.getReasons().isEmpty());
        assertEquals(1.0, result.getConfidence());
        assertNull(result.getSuggestion());
    }

    @Test
    @DisplayName("fail 工厂方法创建未通过结果")
    void testFailFactory() {
        List<String> reasons = List.of("包含敏感词", "违反规定");
        ModerationResult result = ModerationResult.fail("violence", reasons, 0.9, "请修改内容");

        assertFalse(result.isPassed());
        assertEquals("violence", result.getViolationType());
        assertEquals(2, result.getReasons().size());
        assertEquals(0.9, result.getConfidence());
        assertEquals("请修改内容", result.getSuggestion());
    }

    @Test
    @DisplayName("reasons 可以为 null")
    void testNullReasons() {
        ModerationResult result = new ModerationResult(false, "violence", null, 0.9, null);
        assertNull(result.getReasons());
    }

    @Test
    @DisplayName("setPassed 方法正常工作")
    void testSetPassed() {
        ModerationResult result = new ModerationResult();
        result.setPassed(true);
        assertTrue(result.isPassed());
    }

    @Test
    @DisplayName("setViolationType 方法正常工作")
    void testSetViolationType() {
        ModerationResult result = new ModerationResult();
        result.setViolationType("hate");
        assertEquals("hate", result.getViolationType());
    }
}