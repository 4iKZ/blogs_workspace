package com.blog.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES加密工具类
 * 用于敏感数据的加密和解密
 */
@Component
public class AESUtil {

    @Value("${encryption.secret-key:mySecretKey123456789012345678901234567890}")
    private String secretKey;

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/ECB/PKCS5Padding";

    /**
     * 加密数据
     * @param data 待加密的数据
     * @return 加密后的数据（Base64编码）
     */
    public String encrypt(String data) {
        try {
            // 确保密钥长度为16字节（128位）
            byte[] keyBytes = secretKey.substring(0, 16).getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, ALGORITHM);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            
            byte[] encryptedBytes = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            throw new RuntimeException("加密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解密数据
     * @param encryptedData 加密后的数据（Base64编码）
     * @return 解密后的数据
     */
    public String decrypt(String encryptedData) {
        try {
            // 确保密钥长度为16字节（128位）
            byte[] keyBytes = secretKey.substring(0, 16).getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, ALGORITHM);
            
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedData);
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);
            return new String(decryptedBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("解密失败: " + e.getMessage(), e);
        }
    }
}
