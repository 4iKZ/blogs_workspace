package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.SensitiveWord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 敏感词Mapper接口
 */
@Mapper
public interface SensitiveWordMapper extends BaseMapper<SensitiveWord> {

    /**
     * 获取所有敏感词
     * @return 敏感词列表
     */
    @Select("SELECT word FROM sensitive_words")
    List<String> getAllSensitiveWords();

    /**
     * 获取指定级别的敏感词
     * @param level 敏感词级别
     * @return 敏感词列表
     */
    @Select("SELECT word FROM sensitive_words WHERE level = #{level}")
    List<String> getSensitiveWordsByLevel(@Param("level") Integer level);

    /**
     * 根据分类获取敏感词
     * @param category 分类
     * @return 敏感词列表
     */
    @Select("SELECT word FROM sensitive_words WHERE category = #{category}")
    List<String> getSensitiveWordsByCategory(@Param("category") String category);

    /**
     * 检查是否包含敏感词
     * @param word 待检查词
     * @return 是否存在
     */
    @Select("SELECT COUNT(1) > 0 FROM sensitive_words WHERE word = #{word}")
    boolean existsSensitiveWord(@Param("word") String word);
}
