package com.blog.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.blog.entity.Category;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 分类Mapper接口
 */
@Mapper
public interface CategoryMapper extends BaseMapper<Category> {

    /**
     * 查询顶级分类
     * @return 顶级分类列表
     */
    @Select("SELECT * FROM categories WHERE parent_id = 0 AND status = 1 ORDER BY sort_order ASC, id ASC")
    List<Category> selectTopLevelCategories();

    /**
     * 查询子分类
     * @param parentId 父分类ID
     * @return 子分类列表
     */
    @Select("SELECT * FROM categories WHERE parent_id = #{parentId} AND status = 1 ORDER BY sort_order ASC, id ASC")
    List<Category> selectChildrenCategories(@Param("parentId") Long parentId);

    /**
     * 查询所有启用的分类
     * @return 分类列表
     */
    @Select("SELECT * FROM categories WHERE status = 1 ORDER BY sort_order ASC, id ASC")
    List<Category> selectAllActiveCategories();

    /**
     * 根据分类名称查询
     * @param name 分类名称
     * @return 分类实体
     */
    @Select("SELECT * FROM categories WHERE name = #{name}")
    Category selectByName(@Param("name") String name);

    /**
     * 检查分类名称是否存在（排除当前ID）
     * @param name 分类名称
     * @param id 当前分类ID（更新时排除自身）
     * @return 存在返回1，不存在返回0
     */
    @Select("SELECT COUNT(1) FROM categories WHERE name = #{name} AND id != #{id}")
    int countByNameExcludeId(@Param("name") String name, @Param("id") Long id);

    /**
     * 更新分类文章数量
     * @param categoryId 分类ID
     * @param increment 增量（正数增加，负数减少）
     * @return 影响行数
     */
    @Select("UPDATE categories SET article_count = article_count + #{increment}, update_time = NOW() WHERE id = #{categoryId}")
    int updateArticleCount(@Param("categoryId") Long categoryId, @Param("increment") Integer increment);
}