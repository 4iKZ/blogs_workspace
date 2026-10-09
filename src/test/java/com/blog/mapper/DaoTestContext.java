package com.blog.mapper;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * DAO 测试统一注解：使用 {@link DaoTestConfig} 的最小上下文与 dao-test 配置（默认内存 H2）。
 * 需要真实 MySQL 语法的用例请加 {@code @Tag("mysql")}，见 AGENTS.md。
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(classes = DaoTestConfig.class)
@ActiveProfiles("dao-test")
public @interface DaoTestContext {
}
