package com.blog.config;

import com.volcengine.tos.TOSV2;
import com.volcengine.tos.TOSV2ClientBuilder;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 火山云TOS对象存储配置类
 */
@Configuration
@ConfigurationProperties(prefix = "tos")
@Data
public class TOSConfig {
    
    /**
     * Access Key ID
     */
    private String accessKeyId;
    
    /**
     * Secret Access Key
     */
    private String secretAccessKey;
    
    /**
     * 服务端点
     */
    private String endpoint;
    
    /**
     * 区域
     */
    private String region;
    
    /**
     * 存储桶名称
     */
    private String bucketName;
    
    /**
     * 基础文件夹路径
     */
    private String baseFolder;
    
    /**
     * ACL权限
     */
    private String acl = "public-read";
    
    /**
     * 创建TOS客户端Bean
     */
    @Bean
    public TOSV2 tosClient() {
        return new TOSV2ClientBuilder()
                .build(region, endpoint, accessKeyId, secretAccessKey);
    }
    
    /**
     * 获取文件的完整对象Key
     * @param relativePath 相对路径（如：covers/2025/12/08/uuid.jpg）
     * @return 完整的对象Key
     */
    public String getFullObjectKey(String relativePath) {
        String base = baseFolder == null ? "" : baseFolder.trim();
        if (!base.isEmpty() && !base.endsWith("/")) {
            base = base + "/";
        }
        return base + (relativePath == null ? "" : relativePath);
    }
    
    /**
     * 获取文件的公开访问URL
     * @param objectKey TOS对象Key
     * @return 公开访问URL
     */
    public String getPublicUrl(String objectKey) {
        // 构建公开访问URL格式：https://{bucket}.{domain}/{objectKey}
        String ep = endpoint == null ? "" : endpoint.trim();
        String domain = ep.replaceFirst("^https?://", "");
        String key = objectKey == null ? "" : objectKey;
        return String.format("https://%s.%s/%s", bucketName, domain, key);
    }
}
