package com.blog.utils;

import com.blog.mapper.SensitiveWordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;

/**
 * 敏感词过滤器
 * 使用Trie树算法实现高效的敏感词检测
 */
@Slf4j
@Component
public class SensitiveWordFilter {

    @Autowired
    private RedisCacheUtils redisCacheUtils;

    @Autowired
    private SensitiveWordMapper sensitiveWordMapper;

    // 敏感词Trie树根节点；volatile 保证重建后一次性发布，检测线程不会读到半成品树
    private volatile TrieNode rootNode = new TrieNode();

    // 初始化敏感词Trie树
    @PostConstruct
    public void initSensitiveWords() {
        List<String> sensitiveWords = null;

        try {
            // 先从Redis获取敏感词列表
            sensitiveWords = (List<String>) redisCacheUtils.getCache(RedisCacheUtils.SENSITIVE_WORDS_KEY);
        } catch (Exception e) {
            log.warn("从Redis加载敏感词失败，尝试从数据库获取", e);
        }

        // 如果Redis中没有，从数据库获取
        if (sensitiveWords == null || sensitiveWords.isEmpty()) {
            try {
                sensitiveWords = sensitiveWordMapper.getAllSensitiveWords();
                // 缓存到Redis，有效期24小时
                try {
                    redisCacheUtils.setCache(RedisCacheUtils.SENSITIVE_WORDS_KEY, sensitiveWords, 24,
                            java.util.concurrent.TimeUnit.HOURS);
                } catch (Exception e) {
                    log.warn("缓存敏感词到Redis失败", e);
                }
            } catch (Exception e) {
                log.warn("从数据库加载敏感词失败，使用空列表", e);
                sensitiveWords = Collections.emptyList();
            }
        }

        // 构建Trie树
        buildTrieTree(sensitiveWords);
    }

    // 构建敏感词Trie树
    private void buildTrieTree(List<String> sensitiveWords) {
        // 线程安全：在局部变量上完整构建新树，末尾再原子替换引用，避免检测线程读到构建中的半成品树
        TrieNode newRoot = new TrieNode();

        for (String word : sensitiveWords) {
            if (word == null || word.isEmpty()) {
                continue;
            }

            // 入库词先归一化再建树，保证大小写/全角变体可被检出
            char[] normalizedChars = normalize(word).toCharArray();
            TrieNode currentNode = newRoot;

            for (int i = 0; i < normalizedChars.length; i++) {
                char c = normalizedChars[i];
                TrieNode childNode = currentNode.getChild(c);

                if (childNode == null) {
                    childNode = new TrieNode();
                    currentNode.addChild(c, childNode);
                }

                currentNode = childNode;

                // 标记敏感词结束
                if (i == normalizedChars.length - 1) {
                    currentNode.setIsEnd(true);
                }
            }
        }

        this.rootNode = newRoot;
    }

    /**
     * 归一化文本：全角 ASCII（U+FF01–U+FF5E）转半角，ASCII 字母转小写。
     * 严格保持长度 1:1，对已归一化的输入幂等；不做去空格/去零宽/去分隔符处理。
     */
    public static String normalize(String text) {
        if (text == null) {
            return null;
        }
        char[] chars = text.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            chars[i] = normalizeChar(chars[i]);
        }
        return new String(chars);
    }

    private static char normalizeChar(char c) {
        // ① 全角 ASCII（U+FF01–U+FF5E）转半角：减去 0xFEE0
        if (c >= '\uFF01' && c <= '\uFF5E') {
            c = (char) (c - 0xFEE0);
        }
        // ② ASCII 字母转小写（仅处理 A-Z，locale 无关，等价 Locale.ROOT 语义）
        if (c >= 'A' && c <= 'Z') {
            c = (char) (c + ('a' - 'A'));
        }
        return c;
    }

    // 检查文本是否包含敏感词
    public boolean containsSensitiveWords(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        char[] normalizedChars = normalize(text).toCharArray();
        for (int i = 0; i < normalizedChars.length; i++) {
            int length = checkSensitiveWord(normalizedChars, i);
            if (length > 0) {
                return true;
            }
        }

        return false;
    }

    // 检查从指定位置开始的敏感词（输入为归一化后的字符序列）
    private int checkSensitiveWord(char[] normalizedChars, int startIndex) {
        if (startIndex >= normalizedChars.length) {
            return 0;
        }

        TrieNode currentNode = rootNode;
        int matchLength = 0;

        for (int i = startIndex; i < normalizedChars.length; i++) {
            char c = normalizedChars[i];
            TrieNode childNode = currentNode.getChild(c);

            if (childNode == null) {
                break;
            }

            matchLength++;
            currentNode = childNode;

            // 如果匹配到完整的敏感词，返回匹配长度
            if (currentNode.isEnd()) {
                return matchLength;
            }
        }

        return 0;
    }

    // 替换敏感词（保留原文未命中片段，长度不变）
    public String replaceSensitiveWords(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // 在归一化序列上定位命中区间（下标与原文 1:1 对齐），命中区间用原文切片替换
        char[] normalizedChars = normalize(text).toCharArray();
        StringBuilder result = new StringBuilder(text);
        int i = 0;

        while (i < normalizedChars.length) {
            int length = checkSensitiveWord(normalizedChars, i);
            if (length > 0) {
                // 替换为*号，仅覆盖命中区间，其余字符保留原文
                for (int j = i; j < i + length; j++) {
                    result.setCharAt(j, '*');
                }
                i += length;
            } else {
                i++;
            }
        }

        return result.toString();
    }

    // 获取文本中的所有敏感词
    public Set<String> getSensitiveWords(String text) {
        Set<String> sensitiveWords = new HashSet<>();

        if (text == null || text.isEmpty()) {
            return sensitiveWords;
        }

        char[] normalizedChars = normalize(text).toCharArray();
        for (int i = 0; i < normalizedChars.length; i++) {
            int length = checkSensitiveWord(normalizedChars, i);
            if (length > 0) {
                String sensitiveWord = text.substring(i, i + length);
                sensitiveWords.add(sensitiveWord);
                // 跳过当前敏感词，避免重复检测
                i += length - 1;
            }
        }

        return sensitiveWords;
    }

    // 重新加载敏感词
    public void reloadSensitiveWords() {
        // 清除Redis缓存
        redisCacheUtils.deleteCache(RedisCacheUtils.SENSITIVE_WORDS_KEY);
        // 重新初始化
        initSensitiveWords();
    }

    // Trie树节点类
    private static class TrieNode {
        // 子节点映射
        private Map<Character, TrieNode> children;
        // 是否为敏感词结束节点
        private boolean isEnd;

        public TrieNode() {
            this.children = new HashMap<>();
            this.isEnd = false;
        }

        public Map<Character, TrieNode> getChildren() {
            return children;
        }

        public TrieNode getChild(char c) {
            return children.get(c);
        }

        public void addChild(char c, TrieNode node) {
            children.put(c, node);
        }

        public boolean isEnd() {
            return isEnd;
        }

        public void setIsEnd(boolean isEnd) {
            this.isEnd = isEnd;
        }
    }
}
