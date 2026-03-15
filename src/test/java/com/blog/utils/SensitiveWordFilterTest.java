package com.blog.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 敏感词过滤器单元测试
 */
@DisplayName("敏感词过滤器测试")
public class SensitiveWordFilterTest {

    private SensitiveWordFilter filter;

    @BeforeEach
    public void setUp() {
        filter = new SensitiveWordFilter();
        // 直接构建测试用的 Trie 树（不依赖数据库和 Redis）
        List<String> testWords = Arrays.asList(
                "傻逼", "傻B", "煞笔", "草泥马", "操你妈", "妈的",
                "垃圾", "废物", "白痴", "脑残", "智障", "贱人",
                "强奸", "乱伦", "赌博", "诈骗", "毒品", "枪支",
                "GFW", "FLG", "ISIS"
        );
        invokeBuildTrieTree(testWords);
    }

    /**
     * 通过反射调用私有方法构建 Trie 树
     */
    private void invokeBuildTrieTree(List<String> words) {
        try {
            java.lang.reflect.Method method = SensitiveWordFilter.class.getDeclaredMethod(
                    "buildTrieTree", List.class);
            method.setAccessible(true);
            method.invoke(filter, words);
        } catch (Exception e) {
            throw new RuntimeException("构建 Trie 树失败", e);
        }
    }

    // ==================== 基础匹配测试 ====================

    @Test
    @DisplayName("测试单个敏感词匹配")
    public void testContainsSensitiveWords_SingleWord() {
        assertTrue(filter.containsSensitiveWords("你是个傻逼"));
        assertTrue(filter.containsSensitiveWords("这个人真垃圾"));
        assertTrue(filter.containsSensitiveWords("测试GFW防火墙"));
        assertTrue(filter.containsSensitiveWords("ISIS恐怖组织"));
    }

    @Test
    @DisplayName("测试多个敏感词匹配")
    public void testContainsSensitiveWords_MultipleWords() {
        assertTrue(filter.containsSensitiveWords("这个傻逼真垃圾"));
        assertTrue(filter.containsSensitiveWords("强奸和赌博都是犯罪"));
        assertTrue(filter.containsSensitiveWords("GFW和FLG都是敏感词"));
    }

    @Test
    @DisplayName("测试无敏感词文本")
    public void testContainsSensitiveWords_NoSensitiveWords() {
        assertFalse(filter.containsSensitiveWords("这是一篇正常的文章"));
        assertFalse(filter.containsSensitiveWords("Hello World"));
        assertFalse(filter.containsSensitiveWords("今天天气真好"));
        assertFalse(filter.containsSensitiveWords("技术分享：Java并发编程"));
    }

    @Test
    @DisplayName("测试敏感词在文本开头")
    public void testContainsSensitiveWords_AtStart() {
        assertTrue(filter.containsSensitiveWords("傻逼说的对"));
        assertTrue(filter.containsSensitiveWords("垃圾文章不值得看"));
    }

    @Test
    @DisplayName("测试敏感词在文本结尾")
    public void testContainsSensitiveWords_AtEnd() {
        assertTrue(filter.containsSensitiveWords("这个人是个傻逼"));
        assertTrue(filter.containsSensitiveWords("写的真是垃圾"));
    }

    // ==================== 替换功能测试 ====================

    @Test
    @DisplayName("测试单个敏感词替换")
    public void testReplaceSensitiveWords_SingleWord() {
        assertEquals("你是个**", filter.replaceSensitiveWords("你是个傻逼"));
        assertEquals("这个人真**", filter.replaceSensitiveWords("这个人真垃圾"));
        assertEquals("测试***防火墙", filter.replaceSensitiveWords("测试GFW防火墙"));
    }

    @Test
    @DisplayName("测试多个敏感词替换")
    public void testReplaceSensitiveWords_MultipleWords() {
        String result = filter.replaceSensitiveWords("这个傻逼真垃圾");
        assertEquals("这个**真**", result);
        
        result = filter.replaceSensitiveWords("强奸和赌博都是犯罪");
        assertEquals("**和**都是犯罪", result);
    }

    @Test
    @DisplayName("测试无敏感词文本替换（应保持原样）")
    public void testReplaceSensitiveWords_NoChange() {
        assertEquals("这是一篇正常的文章", filter.replaceSensitiveWords("这是一篇正常的文章"));
        assertEquals("Hello World", filter.replaceSensitiveWords("Hello World"));
    }

    // ==================== 获取敏感词列表测试 ====================

    @Test
    @DisplayName("测试获取文本中的敏感词")
    public void testGetSensitiveWords() {
        Set<String> words = filter.getSensitiveWords("这个傻逼真垃圾，简直废物");
        assertTrue(words.contains("傻逼"));
        assertTrue(words.contains("垃圾"));
        assertTrue(words.contains("废物"));
        assertEquals(3, words.size());
    }

    @Test
    @DisplayName("测试获取重复敏感词（应去重）")
    public void testGetSensitiveWords_Duplicate() {
        Set<String> words = filter.getSensitiveWords("傻逼傻逼傻逼");
        assertTrue(words.contains("傻逼"));
        assertEquals(1, words.size());
    }

    @Test
    @DisplayName("测试无敏感词时返回空集合")
    public void testGetSensitiveWords_Empty() {
        Set<String> words = filter.getSensitiveWords("这是一段正常的文字");
        assertTrue(words.isEmpty());
    }

    // ==================== 边界条件测试 ====================

    @Test
    @DisplayName("测试空字符串")
    public void testEmptyString() {
        assertFalse(filter.containsSensitiveWords(""));
        assertEquals("", filter.replaceSensitiveWords(""));
        assertTrue(filter.getSensitiveWords("").isEmpty());
    }

    @Test
    @DisplayName("测试 null 值")
    public void testNullValue() {
        assertFalse(filter.containsSensitiveWords(null));
        assertNull(filter.replaceSensitiveWords(null));
        assertTrue(filter.getSensitiveWords(null).isEmpty());
    }

    @Test
    @DisplayName("测试超长文本")
    public void testLongText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb.append("正常的文字");
        }
        String longText = sb.toString();
        
        assertFalse(filter.containsSensitiveWords(longText));
        assertEquals(longText, filter.replaceSensitiveWords(longText));
    }

    @Test
    @DisplayName("测试敏感词前后有特殊字符")
    public void testSensitiveWordsWithSpecialChars() {
        // 敏感词前后有空格
        assertTrue(filter.containsSensitiveWords(" 傻逼 "));
        // 敏感词前后有标点符号
        assertTrue(filter.containsSensitiveWords("，傻逼。"));
        // 敏感词前后有数字
        assertTrue(filter.containsSensitiveWords("123傻逼456"));
    }

    // ==================== 性能测试 ====================

    @Test
    @DisplayName("测试大文本匹配性能")
    public void testPerformanceWithLargeText() {
        // 构建包含敏感词的大文本
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("这是一段正常的测试文字，包含一些技术内容。");
            if (i % 100 == 0) {
                sb.append("这是一个傻逼测试");
            }
        }
        String largeText = sb.toString();

        long startTime = System.currentTimeMillis();
        boolean contains = filter.containsSensitiveWords(largeText);
        long endTime = System.currentTimeMillis();

        assertTrue(contains);
        // 确保在合理时间内完成（小于 1 秒）
        assertTrue(endTime - startTime < 1000, 
                "敏感词匹配耗时过长: " + (endTime - startTime) + "ms");
    }

    // ==================== Trie 树构建测试 ====================

    @Test
    @DisplayName("测试空敏感词列表构建 Trie 树")
    public void testBuildTrieTree_EmptyList() {
        SensitiveWordFilter newFilter = new SensitiveWordFilter();
        invokeBuildTrieTreeForFilter(newFilter, Arrays.asList());
        
        assertFalse(newFilter.containsSensitiveWords("任何文字都不会被匹配"));
    }

    @Test
    @DisplayName("测试包含 null 和空字符串的列表")
    public void testBuildTrieTree_WithNullAndEmpty() {
        SensitiveWordFilter newFilter = new SensitiveWordFilter();
        List<String> words = Arrays.asList("测试", null, "", "敏感");
        invokeBuildTrieTreeForFilter(newFilter, words);
        
        assertTrue(newFilter.containsSensitiveWords("测试文字"));
        assertTrue(newFilter.containsSensitiveWords("敏感内容"));
    }

    private void invokeBuildTrieTreeForFilter(SensitiveWordFilter targetFilter, List<String> words) {
        try {
            java.lang.reflect.Method method = SensitiveWordFilter.class.getDeclaredMethod(
                    "buildTrieTree", List.class);
            method.setAccessible(true);
            method.invoke(targetFilter, words);
        } catch (Exception e) {
            throw new RuntimeException("构建 Trie 树失败", e);
        }
    }

    // ==================== 特殊场景测试 ====================

    @Test
    @DisplayName("测试敏感词部分匹配（不应匹配）")
    public void testPartialMatch_ShouldNotMatch() {
        // "傻" 单独出现，不应该匹配
        assertFalse(filter.containsSensitiveWords("他是个傻小子"));
        // "逼" 单独出现，不应该匹配
        assertFalse(filter.containsSensitiveWords("逼不得已"));
    }

    @Test
    @DisplayName("测试英文敏感词大小写敏感")
    public void testEnglishCaseSensitivity() {
        // 当前实现区分大小写
        assertTrue(filter.containsSensitiveWords("GFW"));
        // 小写不应匹配（当前 Trie 树实现区分大小写）
        assertFalse(filter.containsSensitiveWords("gfw"));
    }

    @Test
    @DisplayName("测试敏感词替换后长度一致")
    public void testReplaceLengthConsistency() {
        String original = "这个傻逼真垃圾";
        String replaced = filter.replaceSensitiveWords(original);
        assertEquals(original.length(), replaced.length(), 
                "替换后的长度应该与原文本一致");
    }
}