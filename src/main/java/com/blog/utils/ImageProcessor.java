package com.blog.utils;

import com.blog.dto.ImageMetadataDTO;
import com.blog.dto.ImageConvertDTO;
import com.blog.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;

/**
 * 图片处理工具类
 * 支持格式转换、元信息提取、压缩等功能
 */
@Slf4j
public class ImageProcessor {

    private static final Map<String, String> FORMAT_MIME_TYPE_MAP = new HashMap<>();
    private static final Map<String, String> MIME_TYPE_FORMAT_MAP = new HashMap<>();

    static {
        FORMAT_MIME_TYPE_MAP.put("jpg", "image/jpeg");
        FORMAT_MIME_TYPE_MAP.put("jpeg", "image/jpeg");
        FORMAT_MIME_TYPE_MAP.put("png", "image/png");
        FORMAT_MIME_TYPE_MAP.put("gif", "image/gif");
        FORMAT_MIME_TYPE_MAP.put("bmp", "image/bmp");
        FORMAT_MIME_TYPE_MAP.put("webp", "image/webp");

        MIME_TYPE_FORMAT_MAP.put("image/jpeg", "jpg");
        MIME_TYPE_FORMAT_MAP.put("image/png", "png");
        MIME_TYPE_FORMAT_MAP.put("image/gif", "gif");
        MIME_TYPE_FORMAT_MAP.put("image/bmp", "bmp");
        MIME_TYPE_FORMAT_MAP.put("image/webp", "webp");
    }

    /**
     * 支持的输出格式列表
     */
    private static final String[] SUPPORTED_OUTPUT_FORMATS = {"jpg", "jpeg", "png", "webp", "bmp"};

    /**
     * 从MultipartFile获取BufferedImage
     */
    public static BufferedImage getBufferedImage(MultipartFile file) throws IOException {
        try (InputStream is = file.getInputStream()) {
            BufferedImage image = ImageIO.read(is);
            if (image == null) {
                throw new IllegalArgumentException("无法读取图片文件");
            }
            return image;
        }
    }

    /**
     * 检查格式是否支持转换
     */
    public static boolean isFormatSupported(String format) {
        String lowerFormat = format.toLowerCase();
        for (String supported : SUPPORTED_OUTPUT_FORMATS) {
            if (supported.equals(lowerFormat)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取格式对应的MIME类型
     */
    public static String getFormatMimeType(String format) {
        return FORMAT_MIME_TYPE_MAP.get(format.toLowerCase());
    }

    /**
     * 获取MIME类型对应的格式
     */
    public static String getMimeTypeFormat(String mimeType) {
        return MIME_TYPE_FORMAT_MAP.get(mimeType.toLowerCase());
    }

    /**
     * 提取图片元信息
     */
    public static ImageMetadataDTO extractMetadata(MultipartFile file) throws IOException {
        ImageMetadataDTO metadata = new ImageMetadataDTO();

        try (ImageInputStream iis = ImageIO.createImageInputStream(file.getInputStream())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);

            if (!readers.hasNext()) {
                throw new IllegalArgumentException("不支持的图片格式");
            }

            ImageReader reader = readers.next();
            reader.setInput(iis);

            // 基本尺寸信息
            metadata.setWidth(reader.getWidth(0));
            metadata.setHeight(reader.getHeight(0));

            // 格式信息
            String formatName = reader.getFormatName();
            metadata.setFormat(formatName.toLowerCase());
            metadata.setMimeType(getFormatMimeType(formatName));

            // 文件大小
            metadata.setFileSize(file.getSize());
            metadata.setFileName(file.getOriginalFilename());

            // 获取更多元数据（如色彩空间、位深度等）
            try {
                IIOMetadata imageMetadata = reader.getImageMetadata(0);
                String[] metadataNames = imageMetadata.getMetadataFormatNames();
                if (metadataNames != null && metadataNames.length > 0) {
                    // 简单解析：存储第一个元数据格式名称
                    metadata.setMetadataFormat(metadataNames[0]);
                }
            } catch (Exception e) {
                log.warn("无法读取图片元数据: {}", e.getMessage());
            }

            // 尝试读取图片获取色彩类型
            try {
                BufferedImage image = reader.read(0);
                metadata.setColorType(getColorTypeName(image.getType()));
            } catch (Exception e) {
                log.warn("无法读取图片色彩类型: {}", e.getMessage());
            }

            // 计算宽高比
            if (metadata.getHeight() > 0) {
                metadata.setAspectRatio(roundToTwoPlaces((double) metadata.getWidth() / metadata.getHeight()));
            }

            // 计算像素总数
            metadata.setTotalPixels((long) metadata.getWidth() * metadata.getHeight());

            // 估算打印尺寸（假设72 DPI）
            metadata.setPrintWidthCm(roundToTwoPlaces(metadata.getWidth() * 2.54 / 72));
            metadata.setPrintHeightCm(roundToTwoPlaces(metadata.getHeight() * 2.54 / 72));

            reader.dispose();
        } catch (Exception e) {
            log.error("提取图片元信息失败", e);
            throw new IOException("提取图片元信息失败: " + e.getMessage(), e);
        }

        return metadata;
    }

    /**
     * 转换图片格式
     */
    public static ImageConvertDTO convertFormat(
            MultipartFile file,
            String targetFormat,
            Float quality
    ) throws IOException {
        if (!isFormatSupported(targetFormat)) {
            throw new IllegalArgumentException("不支持的目标格式: " + targetFormat);
        }

        // 读取原图
        BufferedImage originalImage = getBufferedImage(file);
        String originalFormat = getMimeTypeFormat(file.getContentType());
        if (originalFormat == null) {
            // 尝试从文件名获取
            String fileName = file.getOriginalFilename();
            if (fileName != null && fileName.contains(".")) {
                originalFormat = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
            } else {
                originalFormat = "jpg";
            }
        }

        // 设置默认质量
        if (quality == null || quality <= 0 || quality > 1.0f) {
            // JPEG默认0.8，其他格式默认1.0
            quality = targetFormat.equalsIgnoreCase("jpg") || targetFormat.equalsIgnoreCase("jpeg") ? 0.8f : 1.0f;
        }

        // 转换格式（处理透明度）
        BufferedImage convertedImage = convertImageType(originalImage, targetFormat);

        // 写入字节流
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String formatName = targetFormat.equalsIgnoreCase("jpg") ? "jpeg" : targetFormat;

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(formatName);
        if (!writers.hasNext()) {
            throw new IllegalArgumentException("不支持的输出格式: " + targetFormat);
        }

        ImageWriter writer = writers.next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);

            // 设置压缩质量
            if (quality < 1.0f && (targetFormat.equalsIgnoreCase("jpg") || targetFormat.equalsIgnoreCase("jpeg"))) {
                javax.imageio.ImageWriteParam writeParam = writer.getDefaultWriteParam();
                if (writeParam.canWriteCompressed()) {
                    writeParam.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
                    writeParam.setCompressionQuality(quality);
                }
            }

            writer.write(null, new IIOImage(convertedImage, null, null), null);
        } finally {
            writer.dispose();
        }

        // 构建结果
        ImageConvertDTO result = new ImageConvertDTO();
        result.setOriginalFormat(originalFormat);
        result.setTargetFormat(targetFormat);
        result.setOriginalSize(file.getSize());
        result.setConvertedSize((long) baos.size());
        result.setWidth(originalImage.getWidth());
        result.setHeight(originalImage.getHeight());
        result.setImageData(baos.toByteArray());
        result.setMimeType(getFormatMimeType(targetFormat));

        // 计算压缩率
        if (file.getSize() > 0) {
            result.setCompressionRatio(roundToTwoPlaces(
                (1 - (double) baos.size() / file.getSize()) * 100
            ));
        }

        return result;
    }

    /**
     * 根据目标格式转换图片类型（处理透明度）
     */
    private static BufferedImage convertImageType(BufferedImage original, String targetFormat) {
        // JPEG不支持透明度，需要转换为RGB
        if (targetFormat.equalsIgnoreCase("jpg") || targetFormat.equalsIgnoreCase("jpeg")) {
            if (original.getTransparency() != Transparency.OPAQUE) {
                // 有透明度，转换为白色背景
                BufferedImage rgbImage = new BufferedImage(
                    original.getWidth(),
                    original.getHeight(),
                    BufferedImage.TYPE_INT_RGB
                );
                Graphics2D g2d = rgbImage.createGraphics();
                g2d.setColor(Color.WHITE);
                g2d.fillRect(0, 0, rgbImage.getWidth(), rgbImage.getHeight());
                g2d.drawImage(original, 0, 0, null);
                g2d.dispose();
                return rgbImage;
            }
        }

        return original;
    }

    /**
     * 获取色彩类型名称
     */
    private static String getColorTypeName(int type) {
        switch (type) {
            case BufferedImage.TYPE_INT_RGB:
                return "RGB";
            case BufferedImage.TYPE_INT_ARGB:
                return "ARGB";
            case BufferedImage.TYPE_INT_ARGB_PRE:
                return "ARGB_PRE";
            case BufferedImage.TYPE_INT_BGR:
                return "BGR";
            case BufferedImage.TYPE_3BYTE_BGR:
                return "3BYTE_BGR";
            case BufferedImage.TYPE_4BYTE_ABGR:
                return "4BYTE_ABGR";
            case BufferedImage.TYPE_4BYTE_ABGR_PRE:
                return "4BYTE_ABGR_PRE";
            case BufferedImage.TYPE_BYTE_GRAY:
                return "GRAY";
            case BufferedImage.TYPE_USHORT_GRAY:
                return "USHORT_GRAY";
            case BufferedImage.TYPE_BYTE_BINARY:
                return "BINARY";
            default:
                return "UNKNOWN";
        }
    }

    /**
     * 保留两位小数
     */
    private static double roundToTwoPlaces(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    /**
     * 压缩图片（按质量）
     */
    public static byte[] compressByQuality(MultipartFile file, float quality) throws IOException {
        BufferedImage image = getBufferedImage(file);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        String format = getMimeTypeFormat(file.getContentType());
        if (format == null) {
            format = "jpg";
        }

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(format);
        if (!writers.hasNext()) {
            throw new IllegalArgumentException("不支持的格式: " + format);
        }

        ImageWriter writer = writers.next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);

            javax.imageio.ImageWriteParam writeParam = writer.getDefaultWriteParam();
            if (writeParam.canWriteCompressed()) {
                writeParam.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
                writeParam.setCompressionQuality(Math.max(0.1f, Math.min(1.0f, quality)));
            }

            writer.write(null, new IIOImage(image, null, null), writeParam);
        } finally {
            writer.dispose();
        }

        return baos.toByteArray();
    }

    /**
     * 缩放图片
     */
    public static BufferedImage scaleImage(BufferedImage original, int maxWidth, int maxHeight) {
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();

        // 计算缩放比例
        double widthRatio = (double) maxWidth / originalWidth;
        double heightRatio = (double) maxHeight / originalHeight;
        double scale = Math.min(widthRatio, heightRatio);

        // 如果图片小于目标尺寸，不缩放
        if (scale >= 1.0) {
            return original;
        }

        int newWidth = (int) (originalWidth * scale);
        int newHeight = (int) (originalHeight * scale);

        // 使用高质量缩放
        BufferedImage scaled = new BufferedImage(newWidth, newHeight, original.getType());
        Graphics2D g2d = scaled.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(original, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        return scaled;
    }

    /**
     * 缩放并压缩图片
     */
    public static byte[] scaleAndCompress(
            MultipartFile file,
            int maxWidth,
            int maxHeight,
            float quality
    ) throws IOException {
        BufferedImage original = getBufferedImage(file);
        BufferedImage scaled = scaleImage(original, maxWidth, maxHeight);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        String format = getMimeTypeFormat(file.getContentType());
        if (format == null) {
            format = "jpg";
        }

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(format);
        if (!writers.hasNext()) {
            throw new IllegalArgumentException("不支持的格式: " + format);
        }

        ImageWriter writer = writers.next();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);

            javax.imageio.ImageWriteParam writeParam = writer.getDefaultWriteParam();
            if (writeParam.canWriteCompressed()) {
                writeParam.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
                writeParam.setCompressionQuality(Math.max(0.1f, Math.min(1.0f, quality)));
            }

            writer.write(null, new IIOImage(scaled, null, null), writeParam);
        } finally {
            writer.dispose();
        }

        return baos.toByteArray();
    }

    /**
     * 检查是否为有效图片
     */
    public static boolean isValidImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return false;
        }

        try {
            BufferedImage image = getBufferedImage(file);
            return image != null && image.getWidth() > 0 && image.getHeight() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取支持的输出格式列表
     */
    public static String[] getSupportedOutputFormats() {
        return SUPPORTED_OUTPUT_FORMATS.clone();
    }
}
