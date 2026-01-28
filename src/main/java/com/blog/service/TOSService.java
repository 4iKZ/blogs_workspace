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
