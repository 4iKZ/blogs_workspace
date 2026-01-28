package com.blog.common;

/**
 * 响应状态码枚举
 */
public enum ResultCode {

    // 成功状态码
    SUCCESS(200, "操作成功"),

    // 客户端错误状态码 4xx
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未认证"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    CONFLICT(409, "资源冲突"),
    UNPROCESSABLE_ENTITY(422, "请求参数验证失败"),
    TOO_MANY_REQUESTS(429, "请求过于频繁"),

    // 服务器错误状态码 5xx
    ERROR(500, "服务器内部错误"),
    SERVICE_UNAVAILABLE(503, "服务暂不可用"),

    // 业务状态码 1000-9999
    USER_NOT_FOUND(1001, "用户不存在"),
    USER_DISABLED(1002, "用户已被禁用"),
    PASSWORD_ERROR(1003, "密码错误"),
    USERNAME_EXIST(1004, "用户名已存在"),
    EMAIL_EXIST(1005, "邮箱已存在"),
    PHONE_EXIST(1006, "手机号已存在"),
    OLD_PASSWORD_ERROR(1007, "原密码错误"),
    PASSWORD_NOT_MATCH(1008, "两次密码输入不一致"),

    ARTICLE_NOT_FOUND(2001, "文章不存在"),
    ARTICLE_NOT_PUBLISHED(2002, "文章未发布"),
    ARTICLE_STATUS_ERROR(2003, "文章状态错误"),
    COMMENT_NOT_FOUND(2004, "评论不存在"),
    COMMENT_STATUS_ERROR(2005, "评论状态错误"),

    CATEGORY_NOT_FOUND(3001, "分类不存在"),
    CATEGORY_EXIST_ARTICLE(3002, "分类下存在文章，无法删除"),

    TAG_NOT_FOUND(4001, "标签不存在"),
    TAG_EXIST_ARTICLE(4002, "标签下存在文章，无法删除"),

    FILE_NOT_FOUND(5001, "文件不存在"),
    FILE_UPLOAD_ERROR(5002, "文件上传失败"),
    FILE_TYPE_ERROR(5003, "文件类型不支持"),
    FILE_SIZE_ERROR(5004, "文件大小超出限制"),
    FILE_DELETE_ERROR(5005, "文件删除失败"),

    CONFIG_NOT_FOUND(6001, "配置不存在"),
    CONFIG_KEY_EXIST(6002, "配置键已存在"),

    COLLECTION_EXIST(7001, "已收藏"),
    LIKE_EXIST(7002, "已点赞"),
    FOLLOW_EXIST(7003, "已关注该用户"),

    // 系统状态码 10000+
    SYSTEM_ERROR(10000, "系统错误"),
    DATABASE_ERROR(10001, "数据库错误"),
    CACHE_ERROR(10002, "缓存错误"),
    MESSAGE_SEND_ERROR(10003, "消息发送失败"),
    API_CALL_ERROR(10004, "接口调用失败"),
    PERMISSION_DENIED(10005, "权限不足");

    private final Integer code;
    private final String message;

    ResultCode(Integer code, String message) {
        this.code = code;
        this.message = message;
    }

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public static ResultCode valueOf(Integer code) {
        for (ResultCode resultCode : values()) {
            if (resultCode.getCode().equals(code)) {
                return resultCode;
            }
        }
        return SYSTEM_ERROR;
    }
}