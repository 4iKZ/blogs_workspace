package com.blog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 图片格式转换结果DTO
 */
@Data
@Schema(description = "图片格式转换结果")
public class ImageConvertDTO {

    @Schema(description = "原始格式")
    private String originalFormat;

    @Schema(description = "目标格式")
    private String targetFormat;

    @Schema(description = "原始大小（字节）")
    private Long originalSize;

    @Schema(description = "转换后大小（字节）")
    private Long convertedSize;

    @Schema(description = "压缩率（百分比）")
    private Double compressionRatio;

    @Schema(description = "宽度（像素）")
    private Integer width;

    @Schema(description = "高度（像素）")
    private Integer height;

    @Schema(description = "图片数据")
    private byte[] imageData;

    @Schema(description = "MIME类型")
    private String mimeType;
}
