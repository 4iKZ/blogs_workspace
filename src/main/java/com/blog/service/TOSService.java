package com.blog.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 火山云TOS对象存储服务接口
 */
public interface TOSService {
    
    /**
     * 上传文件到TOS
     * @param file 文件
     * @param folder 文件夹类型（covers/articles/attachments）
     * @return 文件公开访问URL
     */
    String uploadFile(MultipartFile file, String folder);

    /**
     * 上传文件到TOS并返回带样式的访问URL
     * @param file 文件
     * @param folder 文件夹类型（covers/articles/attachments）
     * @param useStyle 是否使用默认样式
     * @return 文件公开访问URL（带样式参数时返回处理后的URL）
     */
    String uploadFileWithStyle(MultipartFile file, String folder, boolean useStyle);

    /**
     * 上传文件到TOS并返回指定样式的访问URL
     * @param file 文件
     * @param folder 文件夹类型（covers/articles/attachments）
     * @param styleName 样式名称（在TOS控制台创建），为null则返回原图URL
     * @return 文件公开访问URL（带样式参数时返回处理后的URL）
     */
    String uploadFileWithStyle(MultipartFile file, String folder, String styleName);
    
    /**
     * 上传字节数据到TOS
     * @param bytes 字节数据
     * @param fileName 文件名
     * @param folder 文件夹类型
     * @param contentType MIME类型
     * @return 文件公开访问URL
     */
    String uploadBytes(byte[] bytes, String fileName, String folder, String contentType);
    
    /**
     * 删除TOS文件
     * @param objectKey TOS对象Key
     * @return 是否删除成功
     */
    boolean deleteFile(String objectKey);
    
    /**
     * 批量删除TOS文件
     * @param objectKeys TOS对象Key列表
     * @return 是否全部删除成功
     */
    boolean batchDeleteFiles(List<String> objectKeys);
    
    /**
     * 获取文件公开访问URL
     * @param objectKey TOS对象Key
     * @return 公开访问URL
     */
    String getPublicUrl(String objectKey);
    
    /**
     * 检查文件是否存在
     * @param objectKey TOS对象Key
     * @return 是否存在
     */
    boolean fileExists(String objectKey);
}
