package com.blog.service.impl;

import com.blog.config.TOSConfig;
import com.blog.service.TOSService;
import com.volcengine.tos.TOSV2;
import com.volcengine.tos.TosClientException;
import com.volcengine.tos.TosServerException;
import com.volcengine.tos.model.object.*;
import com.volcengine.tos.model.object.ObjectMetaRequestOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 火山云TOS对象存储服务实现类
 */
@Slf4j
@Service
public class TOSServiceImpl implements TOSService {
    
    @Autowired
    private TOSV2 tosClient;
    
    @Autowired
    private TOSConfig tosConfig;

    @Value("${upload.max-size:10485760}")
    private long maxFileSize;

    
    @Override
    public String uploadFile(MultipartFile file, String folder) {
        try {
            // 获取原始文件名和扩展名
            String fileExtension = extensionForMime(file.getContentType(), file.getOriginalFilename());
            
            // 生成唯一文件名
            String fileName = UUID.randomUUID().toString() + fileExtension;
            
            // 构建相对路径：folder/YYYY/MM/DD/uuid.ext
            String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String relativePath = folder + "/" + datePath + "/" + fileName;
            
            // 获取完整的对象Key
            String objectKey = tosConfig.getFullObjectKey(relativePath);
            
            log.info("开始上传文件到TOS: {}", objectKey);
            
            // 服务端根据文件扩展名推导 Content-Type，忽略客户端声明的值，防止存储型XSS
            String contentType = contentTypeForExtension(fileExtension);
            ObjectMetaRequestOptions options = new ObjectMetaRequestOptions().setContentType(contentType);
            // 附件以 attachment 形式下载，避免在浏览器中直接执行
            if ("attachments".equals(folder)) {
                options.setContentDisposition("attachment");
            }
            
            // 构建上传请求
            PutObjectInput putObjectInput = new PutObjectInput()
                    .setBucket(tosConfig.getBucketName())
                    .setKey(objectKey)
                    .setContent(file.getInputStream())
                    .setContentLength(file.getSize())
                    .setOptions(options);
            
            // 执行上传
            PutObjectOutput output = tosClient.putObject(putObjectInput);
            
            log.info("文件上传成功: objectKey={}, etag={}", objectKey, output.getEtag());
            
            // 返回公开访问URL
            return tosConfig.getPublicUrl(objectKey);
            
        } catch (TosClientException e) {
            log.error("TOS客户端错误: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        } catch (TosServerException e) {
            log.error("TOS服务器错误: statusCode={}, code={}, message={}", 
                    e.getStatusCode(), e.getCode(), e.getMessage(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        } catch (IOException e) {
            log.error("文件读取错误: {}", e.getMessage(), e);
            throw new RuntimeException("文件读取失败: " + e.getMessage());
        }
    }

    @Override
    public String uploadFileWithStyle(MultipartFile file, String folder, boolean useStyle) {
        // 先上传原图
        String originalUrl = uploadFile(file, folder);

        // 如果启用样式，添加样式参数
        if (useStyle) {
            String styleName = tosConfig.getDefaultImageStyle();
            if (styleName != null && !styleName.isEmpty()) {
                return originalUrl + "?x-tos-process=style/" + styleName;
            }
        }
        return originalUrl;
    }

    @Override
    public String uploadFileWithStyleAtObjectKey(MultipartFile file, String objectKey, boolean useStyle) {
        String fullObjectKey = tosConfig.getFullObjectKey(objectKey);
        try {
            PutObjectInput input = new PutObjectInput()
                    .setBucket(tosConfig.getBucketName())
                    .setKey(fullObjectKey)
                    .setContent(file.getInputStream())
                    .setContentLength(file.getSize())
                    .setOptions(new ObjectMetaRequestOptions().setContentType(file.getContentType()));
            tosClient.putObject(input);
            String url = tosConfig.getPublicUrl(fullObjectKey);
            if (useStyle && tosConfig.getDefaultImageStyle() != null
                    && !tosConfig.getDefaultImageStyle().isEmpty()) {
                return url + "?x-tos-process=style/" + tosConfig.getDefaultImageStyle();
            }
            return url;
        } catch (TosClientException | TosServerException | IOException e) {
            log.error("按稳定对象Key上传失败: {}", fullObjectKey, e);
            throw new RuntimeException("文件上传失败");
        }
    }

    @Override
    public String uploadFileWithStyle(MultipartFile file, String folder, String styleName) {
        // 先上传原图
        String originalUrl = uploadFile(file, folder);

        // 如果指定了样式名称，添加样式参数
        if (styleName != null && !styleName.isEmpty()) {
            return originalUrl + "?x-tos-process=style/" + styleName;
        }
        return originalUrl;
    }

    @Override
    public String uploadBytes(byte[] bytes, String fileName, String folder, String contentType) {
        try {
            // 构建相对路径
            String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            String relativePath = folder + "/" + datePath + "/" + fileName;
            
            // 获取完整的对象Key
            String objectKey = tosConfig.getFullObjectKey(relativePath);
            
            log.info("开始上传字节数据到TOS: {}", objectKey);
            
            // 构建上传请求
            InputStream inputStream = new ByteArrayInputStream(bytes);
            PutObjectInput putObjectInput = new PutObjectInput()
                    .setBucket(tosConfig.getBucketName())
                    .setKey(objectKey)
                    .setContent(inputStream)
                    .setContentLength((long) bytes.length)
                    .setOptions(new ObjectMetaRequestOptions().setContentType(contentType));
            
            // 执行上传
            PutObjectOutput output = tosClient.putObject(putObjectInput);
            
            log.info("字节数据上传成功: objectKey={}, size={}", objectKey, bytes.length);
            
            // 返回公开访问URL
            return tosConfig.getPublicUrl(objectKey);
            
        } catch (TosClientException e) {
            log.error("TOS客户端错误: {}", e.getMessage(), e);
            throw new RuntimeException("数据上传失败: " + e.getMessage());
        } catch (TosServerException e) {
            log.error("TOS服务器错误: {}", e.getMessage(), e);
            throw new RuntimeException("数据上传失败: " + e.getMessage());
        }
    }
    
    @Override
    public boolean deleteFile(String objectKey) {
        try {
            log.info("开始删除TOS文件: {}", objectKey);
            
            DeleteObjectInput input = new DeleteObjectInput()
                    .setBucket(tosConfig.getBucketName())
                    .setKey(objectKey);
            
            tosClient.deleteObject(input);
            
            log.info("文件删除成功: {}", objectKey);
            return true;
            
        } catch (TosClientException e) {
            log.error("TOS客户端错误: {}", e.getMessage(), e);
            return false;
        } catch (TosServerException e) {
            log.error("TOS服务器错误: {}", e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public boolean batchDeleteFiles(List<String> objectKeys) {
        if (objectKeys == null || objectKeys.isEmpty()) {
            return true;
        }
        
        boolean allSuccess = true;
        for (String objectKey : objectKeys) {
            if (!deleteFile(objectKey)) {
                allSuccess = false;
            }
        }
        return allSuccess;
    }
    
    @Override
    public String getPublicUrl(String objectKey) {
        return tosConfig.getPublicUrl(objectKey);
    }
    
    @Override
    public boolean fileExists(String objectKey) {
        try {
            HeadObjectV2Input input = new HeadObjectV2Input()
                    .setBucket(tosConfig.getBucketName())
                    .setKey(objectKey);
            
            tosClient.headObject(input);
            return true;
            
        } catch (TosServerException e) {
            if (e.getStatusCode() == 404) {
                return false;
            }
            log.error("检查文件存在性失败: {}", e.getMessage(), e);
            return false;
        } catch (TosClientException e) {
            log.error("TOS客户端错误: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf("."));
        }
        return "";
    }

    private String extensionForMime(String mime, String originalFilename) {
        return switch (mime == null ? "" : mime) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            default -> {
                String extension = getFileExtension(originalFilename);
                yield extension.matches("\\.[A-Za-z0-9]{1,10}") ? extension.toLowerCase() : ".bin";
            }
        };
    }

    /**
     * 根据文件扩展名推导 Content-Type；未知类型统一为 application/octet-stream，
     * 不使用客户端声明的值，避免将可执行内容以可渲染类型托管到受信域名。
     */
    private String contentTypeForExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return "application/octet-stream";
        }
        return switch (extension.toLowerCase()) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".gif" -> "image/gif";
            case ".webp" -> "image/webp";
            case ".bmp" -> "image/bmp";
            case ".svg" -> "image/svg+xml";
            case ".pdf" -> "application/pdf";
            case ".txt", ".md", ".log" -> "text/plain";
            case ".doc" -> "application/msword";
            case ".docx" ->
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case ".xls" -> "application/vnd.ms-excel";
            case ".xlsx" ->
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case ".ppt" -> "application/vnd.ms-powerpoint";
            case ".pptx" ->
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case ".zip" -> "application/zip";
            case ".gz" -> "application/gzip";
            case ".tar" -> "application/x-tar";
            case ".7z" -> "application/x-7z-compressed";
            case ".rar" -> "application/vnd.rar";
            case ".mp3" -> "audio/mpeg";
            case ".wav" -> "audio/wav";
            case ".mp4" -> "video/mp4";
            case ".webm" -> "video/webm";
            default -> "application/octet-stream";
        };
    }
}
