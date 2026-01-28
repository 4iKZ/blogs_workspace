package com.blog.integration;

import com.blog.common.Result;
import com.blog.controller.CaptchaController.CaptchaResponse;
import com.blog.dto.ArticleDTO;
import com.blog.service.AdminService;
import com.blog.service.ArticleService;
import com.blog.service.CaptchaService;
import com.blog.utils.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redis集成测试，验证Redis功能正常工作
 */
@SpringBootTest
@ActiveProfiles("test")
@Slf4j
public class RedisIntegrationTest {

    @Autowired
    private ArticleService articleService;
    
    @Autowired
    private CaptchaService captchaService;
    
    @Autowired
    private AdminService adminService;
    
    @Autowired
    private RedisUtils redisUtils;
    
    /**
     * 测试前清除所有Redis缓存
     */
    @BeforeEach
    public void setUp() {
        log.info("测试前清除所有Redis缓存");
        // 清除热门文章缓存
        Set<String> hotArticlesKeys = redisUtils.keys("hot:articles:*");
        if (hotArticlesKeys != null && !hotArticlesKeys.isEmpty()) {
            redisUtils.delete(hotArticlesKeys);
        }
        // 清除验证码缓存
        Set<String> captchaKeys = redisUtils.keys("captcha:*");
        if (captchaKeys != null && !captchaKeys.isEmpty()) {
            redisUtils.delete(captchaKeys);
        }
    }
    
    /**
     * 测试后清除所有Redis缓存
     */
    @AfterEach
    public void tearDown() {
        log.info("测试后清除所有Redis缓存");
        setUp();
    }
    
    /**
     * 测试热门文章缓存功能
     * 验证第一次查询从数据库获取，第二次查询从缓存获取
     */
    @Test
    public void testHotArticlesCache() {
        log.info("测试热门文章缓存功能");
        
        // 第一次查询，应该从数据库获取
        Result<List<ArticleDTO>> result1 = articleService.getHotArticles(10);
        assertTrue(result1.isSuccess(), "第一次获取热门文章失败");
        assertNotNull(result1.getData(), "第一次获取热门文章数据为空");
        assertFalse(result1.getData().isEmpty(), "第一次获取热门文章列表为空");
        log.info("第一次获取热门文章成功，数量：{}", result1.getData().size());
        
        // 验证Redis中存在缓存
        List<ArticleDTO> cachedArticles = redisUtils.get("hot:articles:10");
        assertNotNull(cachedArticles, "Redis中未缓存热门文章");
        assertFalse(cachedArticles.isEmpty(), "Redis中缓存的热门文章列表为空");
        assertEquals(result1.getData().size(), cachedArticles.size(), "缓存的热门文章数量与实际不符");
        log.info("Redis缓存热门文章成功");
        
        // 第二次查询，应该从缓存获取
        Result<List<ArticleDTO>> result2 = articleService.getHotArticles(10);
        assertTrue(result2.isSuccess(), "第二次获取热门文章失败");
        assertNotNull(result2.getData(), "第二次获取热门文章数据为空");
        assertFalse(result2.getData().isEmpty(), "第二次获取热门文章列表为空");
        log.info("第二次获取热门文章成功，数量：{}", result2.getData().size());
        
        // 验证两次结果一致
        assertEquals(result1.getData().size(), result2.getData().size(), "两次获取的热门文章数量不一致");
        for (int i = 0; i < result1.getData().size(); i++) {
            assertEquals(result1.getData().get(i).getId(), result2.getData().get(i).getId(), "两次获取的热门文章ID不一致");
        }
        log.info("热门文章缓存测试通过");
    }
    
    /**
     * 测试验证码Redis存储功能
     * 验证验证码生成、存储、验证和过期
     */
    @Test
    public void testCaptchaRedisStorage() {
        log.info("测试验证码Redis存储功能");
        
        // 生成验证码
        Result<CaptchaResponse> captchaResult = captchaService.getCaptchaImage();
        assertTrue(captchaResult.isSuccess(), "生成验证码失败");
        assertNotNull(captchaResult.getData(), "生成验证码数据为空");
        String captchaKey = captchaResult.getData().getCaptchaKey();
        String captchaImage = captchaResult.getData().getCaptchaImage();
        assertNotNull(captchaKey, "验证码Key为空");
        assertNotNull(captchaImage, "验证码图片为空");
        log.info("生成验证码成功，Key：{}", captchaKey);
        
        // 从Redis中获取验证码
        String cachedCaptcha = redisUtils.get("captcha:" + captchaKey);
        assertNotNull(cachedCaptcha, "Redis中未存储验证码");
        assertTrue(cachedCaptcha.matches("\\d{4}"), "验证码格式不正确");
        log.info("Redis存储验证码成功，验证码：{}", cachedCaptcha);
        
        // 验证正确的验证码
        boolean verifyResult1 = captchaService.verifyCaptcha(captchaKey, cachedCaptcha);
        assertTrue(verifyResult1, "验证正确的验证码失败");
        log.info("验证正确的验证码成功");
        
        // 验证使用过的验证码（应该失败）
        boolean verifyResult2 = captchaService.verifyCaptcha(captchaKey, cachedCaptcha);
        assertFalse(verifyResult2, "验证已使用的验证码应该失败");
        log.info("验证已使用的验证码失败，符合预期");
        
        // 生成新的验证码，测试过期
        Result<CaptchaResponse> captchaResult2 = captchaService.getCaptchaImage();
        assertTrue(captchaResult2.isSuccess(), "生成新验证码失败");
        String newCaptchaKey = captchaResult2.getData().getCaptchaKey();
        String newCachedCaptcha = redisUtils.get("captcha:" + newCaptchaKey);
        assertNotNull(newCachedCaptcha, "Redis中未存储新验证码");
        
        // 等待5分钟，验证验证码过期
        try {
            log.info("等待验证码过期（5分钟）");
            TimeUnit.MINUTES.sleep(5);
            
            // 验证过期的验证码
            boolean verifyResult3 = captchaService.verifyCaptcha(newCaptchaKey, newCachedCaptcha);
            assertFalse(verifyResult3, "验证过期的验证码应该失败");
            log.info("验证过期的验证码失败，符合预期");
        } catch (InterruptedException e) {
            log.error("等待验证码过期时发生异常", e);
            Thread.currentThread().interrupt();
        }
        
        log.info("验证码Redis存储测试通过");
    }
    
    /**
     * 测试缓存清除接口
     * 验证调用clearCache接口后，缓存被清除
     */
    @Test
    public void testClearCache() {
        log.info("测试缓存清除接口");
        
        // 先获取热门文章，生成缓存
        Result<List<ArticleDTO>> result1 = articleService.getHotArticles(10);
        assertTrue(result1.isSuccess(), "获取热门文章失败");
        
        // 验证Redis中存在缓存
        List<ArticleDTO> cachedArticles = redisUtils.get("hot:articles:10");
        assertNotNull(cachedArticles, "Redis中未缓存热门文章");
        log.info("Redis缓存热门文章成功");
        
        // 调用缓存清除接口
        Result<Void> clearResult = adminService.clearCache();
        assertTrue(clearResult.isSuccess(), "调用缓存清除接口失败");
        log.info("调用缓存清除接口成功");
        
        // 验证Redis中缓存被清除
        List<ArticleDTO> clearedArticles = redisUtils.get("hot:articles:10");
        assertNull(clearedArticles, "Redis中缓存未被清除");
        log.info("Redis缓存被成功清除");
        
        // 再次获取热门文章，应该从数据库重新获取
        Result<List<ArticleDTO>> result2 = articleService.getHotArticles(10);
        assertTrue(result2.isSuccess(), "清除缓存后获取热门文章失败");
        assertNotNull(result2.getData(), "清除缓存后获取热门文章数据为空");
        assertFalse(result2.getData().isEmpty(), "清除缓存后获取热门文章列表为空");
        log.info("清除缓存后重新获取热门文章成功");
        
        log.info("缓存清除接口测试通过");
    }
}