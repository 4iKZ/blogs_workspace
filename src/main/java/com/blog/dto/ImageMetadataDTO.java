package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 图片元信息DTO
 */
@Data
@Schema(description = "图片元信息")
public class ImageMetadataDTO {

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "宽度（像素）")
    private Integer width;

    @Schema(description = "高度（像素）")
    private Integer height;

    @Schema(description = "宽高比")
    private Double aspectRatio;

    @Schema(description = "总像素数")
    private Long totalPixels;

    @Schema(description = "图片格式（jpg/png/gif等）")
    private String format;

    @Schema(description = "MIME类型")
    private String mimeType;

    @Schema(description = "色彩类型（RGB/ARGB/GRAY等）")
    private String colorType;

    @Schema(description = "元数据格式")
    private String metadataFormat;

    @Schema(description = "打印宽度（厘米，假设72 DPI）")
    private Double printWidthCm;

    @Schema(description = "打印高度（厘米，假设72 DPI）")
    private Double printHeightCm;
}
