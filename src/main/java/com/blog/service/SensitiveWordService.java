package com.blog.service;

import com.blog.common.PageResult;
import com.blog.common.Result;
import com.blog.dto.SensitiveCheckResultDTO;
import com.blog.dto.SensitiveWordCreateDTO;
import com.blog.dto.SensitiveWordDTO;

import java.util.List;

/**
 * 敏感词服务接口
 */
public interface SensitiveWordService {

    /**
     * 检测内容是否包含敏感词
     * 
     * @param content 待检测内容
     * @return 包含检测结果（是否通过、命中的词）
     */
    Result<SensitiveCheckResultDTO> checkContent(String content);

    /**
     * 检测文本是否包含敏感词，如果包含则直接返回错误 Result
     * 
     * @param content 待检测内容
     * @return 如果不包含则返回 success()，包含则返回带有命中词的 error()
     */
    Result<Void> validateContent(String content);

    /**
     * 获取文本中所有命中的敏感词
     * 
     * @param content 待检测内容
     * @return 命中的敏感词集合
     */
    Result<List<String>> getHitWords(String content);

    /**
     * 替换内容中的敏感词为星号(*)
     * 
     * @param content 待替换内容
     * @return 替换后的内容
     */
    Result<String> replaceContent(String content);

    /**
     * 新增敏感词
     * 
     * @param createDTO 敏感词信息
     * @return 新增的敏感词ID
     */
    Result<Long> addWord(SensitiveWordCreateDTO createDTO);

    /**
     * 删除敏感词
     * 
     * @param id 敏感词ID
     * @return 操作结果
     */
    Result<Void> deleteWord(Long id);

    /**
     * 批量删除敏感词
     * 
     * @param ids 敏感词ID列表
     * @return 操作结果
     */
    Result<Void> batchDeleteWords(List<Long> ids);

    /**
     * 更新敏感词
     * 
     * @param id        敏感词ID
     * @param createDTO 敏感词信息
     * @return 操作结果
     */
    Result<Void> updateWord(Long id, SensitiveWordCreateDTO createDTO);

    /**
     * 分页查询敏感词
     * 
     * @param page     页码
     * @param size     每页大小
     * @param keyword  关键字搜索
     * @param category 分类筛选
     * @return 分页结果
     */
    Result<PageResult<SensitiveWordDTO>> getWordList(Integer page, Integer size, String keyword, String category);

    /**
     * 批量导入敏感词
     * 
     * @param words    敏感词列表
     * @param category 分类
     * @param level    级别
     * @return 导入成功的数量
     */
    Result<Integer> batchImport(List<String> words, String category, Integer level);

    /**
     * 重新加载敏感词缓存并重建Trie树
     * 
     * @return 操作结果
     */
    Result<Void> reloadCache();
}
