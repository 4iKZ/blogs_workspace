package com.blog.utils;

import com.blog.mapper.SensitiveWordMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 敏感词过滤器
 * 使用Trie树算法实现高效的敏感词检测
 */
@Component
public class SensitiveWordFilter {

    @Autowired
    private RedisCacheUtils redisCacheUtils;

    @Autowired
    private SensitiveWordMapper sensitiveWordMapper;

    // 敏感词Trie树根节点
    private TrieNode rootNode = new TrieNode();

    // 初始化敏感词Trie树
    public void initSensitiveWords() {
        // 先从Redis获取敏感词列表
        List<String> sensitiveWords = (List<String>) redisCacheUtils.getCache(RedisCacheUtils.SENSITIVE_WORDS_KEY);
        
        // 如果Redis中没有，从数据库获取
        if (sensitiveWords == null || sensitiveWords.isEmpty()) {
            sensitiveWords = sensitiveWordMapper.getAllSensitiveWords();
            // 缓存到Redis，有效期24小时
            redisCacheUtils.setCache(RedisCacheUtils.SENSITIVE_WORDS_KEY, sensitiveWords, 24, java.util.concurrent.TimeUnit.HOURS);
        }
        
        // 构建Trie树
        buildTrieTree(sensitiveWords);
    }

    // 构建敏感词Trie树
    private void buildTrieTree(List<String> sensitiveWords) {
        rootNode = new TrieNode();
        
        for (String word : sensitiveWords) {
            if (word == null || word.isEmpty()) {
                continue;
            }
            
            TrieNode currentNode = rootNode;
            
            for (int i = 0; i < word.length(); i++) {
                char c = word.charAt(i);
                TrieNode childNode = currentNode.getChild(c);
                
                if (childNode == null) {
                    childNode = new TrieNode();
                    currentNode.addChild(c, childNode);
                }
                
                currentNode = childNode;
                
                // 标记敏感词结束
                if (i == word.length() - 1) {
                    currentNode.setIsEnd(true);
                }
            }
        }
    }

    // 检查文本是否包含敏感词
    public boolean containsSensitiveWords(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        
        for (int i = 0; i < text.length(); i++) {
            int length = checkSensitiveWord(text, i);
            if (length > 0) {
                return true;
            }
        }
        
        return false;
    }

    // 检查从指定位置开始的敏感词
    private int checkSensitiveWord(String text, int startIndex) {
        if (startIndex >= text.length()) {
            return 0;
        }
        
        TrieNode currentNode = rootNode;
        int matchLength = 0;
        
        for (int i = startIndex; i < text.length(); i++) {
            char c = text.charAt(i);
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

    // 替换敏感词
    public String replaceSensitiveWords(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        
        StringBuilder result = new StringBuilder(text);
        int i = 0;
        
        while (i < result.length()) {
            int length = checkSensitiveWord(result.toString(), i);
            if (length > 0) {
                // 替换为*号
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
        
        for (int i = 0; i < text.length(); i++) {
            int length = checkSensitiveWord(text, i);
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
