package com.blog.config;

import com.blog.service.ArticleRankService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 应用启动时异步初始化文章排行榜
 * 使用异步执行避免阻塞应用启动
 * 确保所有已发布的文章都被初始化到 Redis ZSet 中
 */
@Component
@Order(100) // 确保在其他组件初始化之后执行
@Slf4j
public class ArticleRankInitializer implements ApplicationRunner {

    @Autowired
    private ArticleRankService articleRankService;

    @Override
    @Async("notificationTaskExecutor") // 使用配置的线程池异步执行
    public void run(ApplicationArguments args) {
        log.info("开始异步初始化文章排行榜...");
        try {
            articleRankService.initializeAllArticles();
            log.info("文章排行榜初始化完成");
        } catch (Exception e) {
            log.error("文章排行榜初始化失败", e);
        }
    }
}
