package com.blog.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Component
@Validated
@ConfigurationProperties(prefix = "upload.image")
public class ImageValidationProperties {
    @Min(1)
    @Max(32_768)
    private int maxDimension = 8192;
    @Min(1)
    @Max(400_000_000)
    private long maxFramePixels = 25_000_000L;
    @Min(1)
    @Max(1_000_000_000)
    private long maxTotalPixels = 50_000_000L;
    @Min(1)
    @Max(1_000)
    private int maxGifFrames = 100;

    public int getMaxDimension() { return maxDimension; }
    public void setMaxDimension(int maxDimension) { this.maxDimension = maxDimension; }
    public long getMaxFramePixels() { return maxFramePixels; }
    public void setMaxFramePixels(long maxFramePixels) { this.maxFramePixels = maxFramePixels; }
    public long getMaxTotalPixels() { return maxTotalPixels; }
    public void setMaxTotalPixels(long maxTotalPixels) { this.maxTotalPixels = maxTotalPixels; }
    public int getMaxGifFrames() { return maxGifFrames; }
    public void setMaxGifFrames(int maxGifFrames) { this.maxGifFrames = maxGifFrames; }

    @AssertTrue(message = "max-total-pixels must be greater than or equal to max-frame-pixels")
    public boolean isTotalPixelsAtLeastFramePixels() {
        return maxTotalPixels >= maxFramePixels;
    }
}
