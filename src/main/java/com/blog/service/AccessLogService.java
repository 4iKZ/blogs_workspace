package com.blog.service;

import com.blog.entity.WebsiteAccessLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 访问日志缓冲写入入口服务
 */
@Slf4j
@Service
public class AccessLogService {

    @Autowired
    private AccessLogBufferService accessLogBufferService;

    /**
     * 将访问日志写入缓冲队列，不阻塞主请求线程
     */
    public void saveAsync(WebsiteAccessLog accessLog) {
        boolean enqueued = accessLogBufferService.offer(accessLog);
        if (!enqueued) {
            log.warn("访问日志缓冲队列繁忙，日志入队失败");
        }
    }
}
