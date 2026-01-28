package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 网站配置DTO
 */
@Data
@Schema(description = "网站配置DTO")
public class WebsiteConfigDTO {

    @Schema(description = "网站名称")
    private String websiteName;

    @Schema(description = "网站描述")
    private String websiteDescription;

    @Schema(description = "网站关键词")
    private String websiteKeywords;

    @Schema(description = "网站Logo")
    private String websiteLogo;

    @Schema(description = "网站图标")
    private String websiteFavicon;

    @Schema(description = "网站备案号")
    private String websiteIcp;

    @Schema(description = "网站统计代码")
    private String websiteAnalytics;

    @Schema(description = "网站状态：0-关闭，1-开启")
    private Integer websiteStatus;

    @Schema(description = "关闭提示信息")
    private String closeMessage;

    @Schema(description = "每页显示文章数")
    private Integer pageSize;

    @Schema(description = "RSS订阅数量")
    private Integer rssLimit;

    @Schema(description = "是否开启评论：0-关闭，1-开启")
    private Integer commentStatus;

    @Schema(description = "是否开启注册：0-关闭，1-开启")
    private Integer registerStatus;
}