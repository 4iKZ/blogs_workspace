package com.blog.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("website_access_log")
@Schema(description = "网站访问日志")
public class WebsiteAccessLog {
    
    @Schema(description = "访问日志ID")
    private Long id;
    
    @Schema(description = "访问日期")
    private String accessDate;
    
    @Schema(description = "访问时间")
    private LocalDateTime accessTime;
    
    @Schema(description = "IP地址")
    private String ipAddress;
    
    @Schema(description = "用户代理")
    private String userAgent;
    
    @Schema(description = "请求URL")
    private String requestUrl;
    
    @Schema(description = "页面URL")
    private String pageUrl;
    
    @Schema(description = "请求方法")
    private String requestMethod;
    
    @Schema(description = "响应状态码")
    private Integer responseStatus;
    
    @Schema(description = "响应时间（毫秒）")
    private Long responseTime;
    
    @Schema(description = "页面来源")
    private String referer;
    
    @Schema(description = "用户ID（游客为NULL）")
    private Long userId;
    
    @Schema(description = "会话ID")
    private String sessionId;
    
    @Schema(description = "国家")
    private String country;
    
    @Schema(description = "省份")
    private String province;
    
    @Schema(description = "城市")
    private String city;
    
    @Schema(description = "设备类型")
    private String deviceType;
    
    @Schema(description = "浏览器")
    private String browser;
    
    @Schema(description = "操作系统")
    private String operatingSystem;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    public void setPageUrl(String pageUrl) {
        this.pageUrl = pageUrl;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setAccessTime(LocalDateTime accessTime) {
        this.accessTime = accessTime;
    }
}