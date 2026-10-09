package com.blog.mapper;

import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.blog.config.MyBatisPlusConfig;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * DAO 测试的最小 Spring 上下文：只加载数据源、事务、JdbcTemplate、SQL 初始化与 MyBatis-Plus，
 * 以及生产的 {@link MyBatisPlusConfig}（Mapper 扫描、自动填充、分页拦截器）。
 * 不加载 Web、Redis、安全、邮件等模块，因此不需要它们的配置即可运行。
 */
@Configuration
@Import(MyBatisPlusConfig.class)
@ImportAutoConfiguration({
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        JdbcTemplateAutoConfiguration.class,
        SqlInitializationAutoConfiguration.class,
        MybatisPlusAutoConfiguration.class
})
public class DaoTestConfig {
}
