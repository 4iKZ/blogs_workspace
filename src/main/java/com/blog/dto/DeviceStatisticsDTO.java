package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 设备统计DTO
 */
@Data
@Schema(description = "设备统计DTO")
public class DeviceStatisticsDTO {

    @Schema(description = "设备类型统计")
    private DeviceTypeStat deviceType;

    @Schema(description = "操作系统统计")
    private OperatingSystemStat operatingSystem;

    @Schema(description = "浏览器统计")
    private BrowserStat browser;

    @Data
    @Schema(description = "设备类型统计")
    public static class DeviceTypeStat {
        @Schema(description = "桌面端访问占比（%）")
        private Double desktop;

        @Schema(description = "移动端访问占比（%）")
        private Double mobile;

        @Schema(description = "平板端访问占比（%）")
        private Double tablet;
    }

    @Data
    @Schema(description = "操作系统统计")
    public static class OperatingSystemStat {
        @Schema(description = "Windows占比（%）")
        private Double windows;

        @Schema(description = "macOS占比（%）")
        private Double macos;

        @Schema(description = "Linux占比（%）")
        private Double linux;

        @Schema(description = "Android占比（%）")
        private Double android;

        @Schema(description = "iOS占比（%）")
        private Double ios;

        @Schema(description = "其他占比（%）")
        private Double other;
    }

    @Data
    @Schema(description = "浏览器统计")
    public static class BrowserStat {
        @Schema(description = "Chrome占比（%）")
        private Double chrome;

        @Schema(description = "Firefox占比（%）")
        private Double firefox;

        @Schema(description = "Safari占比（%）")
        private Double safari;

        @Schema(description = "Edge占比（%）")
        private Double edge;

        @Schema(description = "其他占比（%）")
        private Double other;
    }
}