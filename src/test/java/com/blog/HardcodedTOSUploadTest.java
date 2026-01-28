package com.blog;

import com.volcengine.tos.TOSV2;
import com.volcengine.tos.TOSV2ClientBuilder;
import com.volcengine.tos.TosClientException;
import com.volcengine.tos.TosServerException;
import com.volcengine.tos.model.object.*;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 使用环境变量或默认配置，进行独立上传验证。
 * 可通过设置环境变量覆盖默认配置：
 * - TOS_ACCESS_KEY_ID
 * - TOS_SECRET_ACCESS_KEY
 * - TOS_ENDPOINT
 * - TOS_REGION
 * - TOS_BUCKET_NAME
 * - TOS_BASE_FOLDER
 */
public class HardcodedTOSUploadTest {
    @Test
    public void uploadSmoke() {
        String ACCESS_KEY_ID = System.getenv().getOrDefault("TOS_ACCESS_KEY_ID", "AKLTMWQxYWIxYzIxZDIzNDljM2IyNTkxZDNmOGI5N2QxN2M");
        String SECRET_ACCESS_KEY = System.getenv().getOrDefault("TOS_SECRET_ACCESS_KEY", "T1daaU5tWXlNRFU1TURWaU5HSTRNVGhqWWpCaE5UVXhaRGs0Tmpsa1pUTQ==");
        String ENDPOINT = System.getenv().getOrDefault("TOS_ENDPOINT", "https://tos-cn-beijing.volces.com");
        String REGION = System.getenv().getOrDefault("TOS_REGION", "cn-beijing");
        String BUCKET_NAME = System.getenv().getOrDefault("TOS_BUCKET_NAME", "syhaox");
        String BASE_FOLDER = System.getenv().getOrDefault("TOS_BASE_FOLDER", "old_book_system/");

        TOSV2 client = null;
        String objectKey = null;

        try {
            client = new TOSV2ClientBuilder().build(REGION, ENDPOINT, ACCESS_KEY_ID, SECRET_ACCESS_KEY);
            String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            objectKey = BASE_FOLDER + "covers/" + datePath + "/" + System.currentTimeMillis() + ".jpg";

            byte[] content = ("Hardcoded TOS Upload Test @ " + LocalDateTime.now()).getBytes(StandardCharsets.UTF_8);

            PutObjectInput putInput = new PutObjectInput()
                    .setBucket(BUCKET_NAME)
                    .setKey(objectKey)
                    .setContent(new java.io.ByteArrayInputStream(content))
                    .setContentLength((long) content.length);

            System.out.println("[上传] objectKey=" + objectKey);
            PutObjectOutput putOut = client.putObject(putInput);
            System.out.println("[上传成功] etag=" + putOut.getEtag());

            HeadObjectV2Input headInput = new HeadObjectV2Input().setBucket(BUCKET_NAME).setKey(objectKey);
            HeadObjectV2Output headOut = client.headObject(headInput);
            System.out.println("[校验存在] contentLength=" + headOut.getContentLength());

            DeleteObjectInput delInput = new DeleteObjectInput().setBucket(BUCKET_NAME).setKey(objectKey);
            DeleteObjectOutput delOut = client.deleteObject(delInput);
            System.out.println("[删除成功] deleteMarker=" + delOut.isDeleteMarker());

            assertTrue(true);
        } catch (TosServerException e) {
            System.out.println("[服务器错误] statusCode=" + e.getStatusCode() + ", code=" + e.getCode() + ", message=" + e.getMessage());
            assertTrue(false);
        } catch (TosClientException e) {
            System.out.println("[客户端错误] " + e.getMessage());
            assertTrue(false);
        } catch (Exception e) {
            System.out.println("[未知错误] " + e.getMessage());
            assertTrue(false);
        } finally {
            if (client != null) try { client.close(); } catch (Exception ignore) {}
        }
    }
}

