package com.blog.mapper;

import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * #16: countByUserId 必须只统计已发布文章的赞（join articles 且过滤 status），
 * 否则 total 会把已下架文章的赞算进去，导致按 total 分页时尾页为空。
 * 无 DB 环境下，断言 MyBatis 生成的 mapped statement SQL 含状态过滤。
 */
class UserLikeMapperLowFixTest {

    private String boundSql(String statementId) {
        Configuration configuration = new Configuration();
        configuration.addMapper(UserLikeMapper.class);
        MappedStatement ms = configuration.getMappedStatement(
                UserLikeMapper.class.getName() + "." + statementId);
        return ms.getBoundSql(null).getSql();
    }

    @Test
    void countByUserId_shouldFilterPublishedArticlesOnly() {
        String sql = boundSql("countByUserId");

        assertThat(sql).containsIgnoringCase("articles");
        // 已发布口径与同文件 selectByUserId 的 a.status = 2 一致
        assertThat(sql.replaceAll("\\s+", " ")).contains("a.status = 2");
    }

    @Test
    void selectByUserId_shouldKeepPublishedFilter() {
        // 回归：列表查询的已发布过滤不动
        String sql = boundSql("selectByUserId");

        assertThat(sql.replaceAll("\\s+", " ")).contains("a.status = 2");
    }
}
