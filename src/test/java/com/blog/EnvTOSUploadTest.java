package com.blog;

import com.volcengine.tos.TOSV2;
import com.volcengine.tos.TOSV2ClientBuilder;
import com.volcengine.tos.TosClientException;
import com.volcengine.tos.TosServerException;
import com.volcengine.tos.model.object.DeleteObjectInput;
import com.volcengine.tos.model.object.DeleteObjectOutput;
import com.volcengine.tos.model.object.HeadObjectV2Input;
import com.volcengine.tos.model.object.HeadObjectV2Output;
import com.volcengine.tos.model.object.PutObjectInput;
import com.volcengine.tos.model.object.PutObjectOutput;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 独立的对象存储上传验证（使用项目同参数）
 * 不依赖项目后端接口，直接使用 TOS SDK 进行 Put/Head/Delete 验证。
 */
public class EnvTOSUploadTest {

    @Test
    public void smokeUploadUsingProjectParams() {
        String accessKeyId = System.getenv("TOS_ACCESS_KEY_ID");
        String secretAccessKey = System.getenv("TOS_SECRET_ACCESS_KEY");

        // 项目 application.yml 中的默认参数
        String endpoint = "https://tos-cn-beijing.volces.com";
        String region = "cn-beijing";
        String bucketName = "syhaox";
        String baseFolder = "old_book_system/";

        if (accessKeyId == null || accessKeyId.isBlank() || secretAccessKey == null || secretAccessKey.isBlank()) {
            System.out.println("[跳过] 未找到环境变量 TOS_ACCESS_KEY_ID / TOS_SECRET_ACCESS_KEY，无法执行独立上传验证。");
            assertTrue(true);
            return;
        }

        TOSV2 client = null;
        String objectKey = null;

        try {
            client = new TOSV2ClientBuilder().build(region, endpoint, accessKeyId, secretAccessKey);

            String datePath = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            objectKey = baseFolder + "covers/" + datePath + "/" + System.currentTimeMillis() + ".jpg";

            byte[] content = ("TOS Upload Smoke Test @ " + LocalDateTime.now()).getBytes(StandardCharsets.UTF_8);

            PutObjectInput putInput = new PutObjectInput()
                    .setBucket(bucketName)
                    .setKey(objectKey)
                    .setContent(new java.io.ByteArrayInputStream(content))
                    .setContentLength((long) content.length);

            System.out.println("[上传] objectKey=" + objectKey);
            PutObjectOutput putOut = client.putObject(putInput);
            System.out.println("[上传成功] etag=" + putOut.getEtag());

            HeadObjectV2Input headInput = new HeadObjectV2Input().setBucket(bucketName).setKey(objectKey);
            HeadObjectV2Output headOut = client.headObject(headInput);
            System.out.println("[校验存在] contentLength=" + headOut.getContentLength());

            DeleteObjectInput delInput = new DeleteObjectInput().setBucket(bucketName).setKey(objectKey);
            DeleteObjectOutput delOut = client.deleteObject(delInput);
            System.out.println("[删除成功] deleteMarker=" + delOut.isDeleteMarker());

            assertTrue(true);
        } catch (TosServerException e) {
            System.out.println("[服务器错误] statusCode=" + e.getStatusCode() + ", code=" + e.getCode() + ", message=" + e.getMessage());
            System.out.println("[建议] 检查 AK/SK、桶权限、Region/Endpoint 是否匹配；确认 baseFolder 路径策略。");
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

