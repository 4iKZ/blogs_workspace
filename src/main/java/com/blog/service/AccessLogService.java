package com.blog.service;

import com.blog.entity.WebsiteAccessLog;
import com.blog.mapper.WebsiteAccessLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 访问日志异步写入服务
 * 独立为 Spring Bean，确保 @Async 通过代理生效（避免自调用失效问题）
 */
@Slf4j
@Service
public class AccessLogService {

    @Autowired
    private WebsiteAccessLogMapper websiteAccessLogMapper;

    /**
     * 异步插入访问日志记录，不阻塞主请求线程
     */
    @Async("accessLogExecutor")
    public void saveAsync(WebsiteAccessLog accessLog) {
        try {
            websiteAccessLogMapper.insert(accessLog);
        } catch (Exception e) {
            log.error("异步写入访问日志失败: {}", e.getMessage());
        }
    }
}
