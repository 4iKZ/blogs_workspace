package com.blog.mapper;

import com.blog.entity.Category;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dao-test")
@DisplayName("CategoryMapper DAO 直测")
class CategoryMapperDaoTest {

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanup() {
        jdbcTemplate.execute("DELETE FROM categories WHERE name LIKE 'dao-test-%'");
    }

    @Test
    @DisplayName("分类层级查询、名称检查与文章数量更新")
    void categoryQueries_shouldPersistAndReturnRows() {
        Category parent = new Category();
        parent.setName("dao-test-parent");
        parent.setDescription("dao");
        parent.setParentId(0L);
        parent.setSortOrder(1);
        parent.setArticleCount(0);
        parent.setStatus(1);
        categoryMapper.insert(parent);

        Category child = new Category();
        child.setName("dao-test-child");
        child.setDescription("dao-child");
        child.setParentId(parent.getId());
        child.setSortOrder(1);
        child.setArticleCount(0);
        child.setStatus(1);
        categoryMapper.insert(child);

        assertThat(categoryMapper.selectByName("dao-test-parent")).isNotNull();
        assertThat(categoryMapper.countByNameExcludeId("dao-test-parent", child.getId())).isNotZero();
        assertThat(categoryMapper.countByNameExcludeId("dao-test-parent", parent.getId())).isZero();

        List<Category> topLevel = categoryMapper.selectTopLevelCategories();
        assertThat(topLevel).extracting(Category::getName).contains("dao-test-parent");

        List<Category> children = categoryMapper.selectChildrenCategories(parent.getId());
        assertThat(children).extracting(Category::getName).contains("dao-test-child");

        List<Category> allActive = categoryMapper.selectAllActiveCategories();
        assertThat(allActive).extracting(Category::getName).contains("dao-test-parent", "dao-test-child");
    }

    @Test
    @DisplayName("分类文章数量增减")
    void updateArticleCount_shouldAffectCounter() {
        Category category = new Category();
        category.setName("dao-test-count");
        category.setDescription("dao");
        category.setParentId(0L);
        category.setSortOrder(2);
        category.setArticleCount(0);
        category.setStatus(1);
        categoryMapper.insert(category);

        int incremented = categoryMapper.updateArticleCount(category.getId(), 5);
        assertThat(incremented).isEqualTo(1);

        Category updated = categoryMapper.selectById(category.getId());
        assertThat(updated.getArticleCount()).isEqualTo(5);
    }
}
