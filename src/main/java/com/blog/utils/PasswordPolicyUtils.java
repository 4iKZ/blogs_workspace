package com.blog.utils;

import org.springframework.stereotype.Component;

/**
 * 密码策略验证工具类
 */
@Component
public class PasswordPolicyUtils {

    /**
     * 验证密码是否符合策略
     * @param password 密码
     * @return 是否符合策略
     */
    public static boolean validatePassword(String password) {
        // 密码不能为空
        if (password == null || password.isEmpty()) {
            return false;
        }
        
        // 密码长度至少8位
        if (password.length() < 8) {
            return false;
        }
        
        // 密码长度不超过20位
        if (password.length() > 20) {
            return false;
        }
        
        // 包含至少一个大写字母
        if (!password.matches(".*[A-Z].*")) {
            return false;
        }
        
        // 包含至少一个小写字母
        if (!password.matches(".*[a-z].*")) {
            return false;
        }
        
        // 包含至少一个数字
        if (!password.matches(".*[0-9].*")) {
            return false;
        }
        
        // 包含至少一个特殊字符
        if (!password.matches(".*[!@#$%^&*(),.?\\:{}|<>].*")) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 获取密码策略描述
     * @return 密码策略描述
     */
    public static String getPasswordPolicy() {
        return "密码必须包含：1. 长度8-20位；2. 至少一个大写字母；3. 至少一个小写字母；4. 至少一个数字；5. 至少一个特殊字符（!@#$%^&*(),.?\\:{}|<>）";
    }
}