package com.blog;

import com.volcengine.tos.TOSV2;
import com.volcengine.tos.TOSV2ClientBuilder;
import com.volcengine.tos.TosClientException;
import com.volcengine.tos.TosServerException;
import com.volcengine.tos.comm.event.DataTransferStatus;
import com.volcengine.tos.comm.event.DataTransferType;
import com.volcengine.tos.model.object.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * 火山云TOS连接测试类
 * 测试上传、读取、删除功能
 */
public class TOSConnectionTest {

    // 火山云TOS配置参数 - 从环境变量或系统属性读取
    private static final String ACCESS_KEY_ID = System.getenv().getOrDefault("TOS_ACCESS_KEY_ID", "AKLTMWQxYWIxYzIxZDIzNDljM2IyNTkxZDNmOGI5N2QxN2M");
    private static final String SECRET_ACCESS_KEY = System.getenv().getOrDefault("TOS_SECRET_ACCESS_KEY", "T1daaU5tWXlNRFU1TURWaU5HSTRNVGhqWWpCaE5UVXhaRGs0Tmpsa1pUTQ==");
    private static final String ENDPOINT = System.getenv().getOrDefault("TOS_ENDPOINT", "https://tos-cn-beijing.volces.com");
    private static final String REGION = System.getenv().getOrDefault("TOS_REGION", "cn-beijing");
    private static final String BUCKET_NAME = System.getenv().getOrDefault("TOS_BUCKET_NAME", "syhaox");
    private static final String TEST_FOLDER = System.getenv().getOrDefault("TOS_BASE_FOLDER", "old_book_system/");
    private static final String TEST_FILE_NAME = TEST_FOLDER + "test_file_" + System.currentTimeMillis() + ".txt";

    public static void main(String[] args) {
        System.out.println("=".repeat(80));
        System.out.println("开始测试火山云TOS连接和功能");
        System.out.println("=".repeat(80));
        
        TOSV2 tosClient = null;
        boolean allTestsPassed = true;
        
        try {
            // 1. 创建TOS客户端
            System.out.println("\n[步骤1] 创建TOS客户端...");
            tosClient = new TOSV2ClientBuilder()
                    .build(REGION, ENDPOINT, ACCESS_KEY_ID, SECRET_ACCESS_KEY);
            System.out.println("✓ TOS客户端创建成功");
            System.out.println("  - Endpoint: " + ENDPOINT);
            System.out.println("  - Region: " + REGION);
            System.out.println("  - Bucket: " + BUCKET_NAME);
            
            // 2. 测试上传文件
            System.out.println("\n[步骤2] 测试文件上传...");
            String testContent = "这是一个测试文件，用于验证火山云TOS上传功能。\n" +
                                "测试时间: " + new java.util.Date() + "\n" +
                                "如果您看到这个文件，说明上传功能正常！";
            
            byte[] data = testContent.getBytes(StandardCharsets.UTF_8);
            InputStream inputStream = new ByteArrayInputStream(data);
            
            PutObjectInput putObjectInput = new PutObjectInput()
                    .setBucket(BUCKET_NAME)
                    .setKey(TEST_FILE_NAME)
                    .setContent(inputStream)
                    .setContentLength((long) data.length);
            
            PutObjectOutput putOutput = tosClient.putObject(putObjectInput);
            System.out.println("✓ 文件上传成功");
            System.out.println("  - 文件路径: " + TEST_FILE_NAME);
            System.out.println("  - 文件大小: " + data.length + " bytes");
            System.out.println("  - ETag: " + putOutput.getEtag());
            
            // 3. 测试读取文件
            System.out.println("\n[步骤3] 测试文件读取...");
            GetObjectV2Input getObjectInput = new GetObjectV2Input()
                    .setBucket(BUCKET_NAME)
                    .setKey(TEST_FILE_NAME);
            
            GetObjectV2Output getOutput = tosClient.getObject(getObjectInput);
            
            // 读取文件内容
            byte[] buffer = new byte[1024];
            int bytesRead = getOutput.getContent().read(buffer);
            String downloadedContent = new String(buffer, 0, bytesRead, StandardCharsets.UTF_8);
            
            System.out.println("✓ 文件读取成功");
            System.out.println("  - 内容长度: " + getOutput.getContentLength() + " bytes");
            System.out.println("  - 内容类型: " + getOutput.getContentType());
            System.out.println("  - 文件内容预览:");
            System.out.println("    " + downloadedContent.substring(0, Math.min(100, downloadedContent.length())) + "...");
            
            // 验证内容是否一致
            if (downloadedContent.contains("测试文件")) {
                System.out.println("✓ 文件内容验证通过");
            } else {
                System.out.println("✗ 文件内容验证失败");
                allTestsPassed = false;
            }
            
            getOutput.getContent().close();
            
            // 4. 测试获取文件信息
            System.out.println("\n[步骤4] 测试获取文件元数据...");
            HeadObjectV2Input headObjectInput = new HeadObjectV2Input()
                    .setBucket(BUCKET_NAME)
                    .setKey(TEST_FILE_NAME);
            
            HeadObjectV2Output headOutput = tosClient.headObject(headObjectInput);
            System.out.println("✓ 文件元数据获取成功");
            System.out.println("  - 最后修改时间: " + headOutput.getLastModified());
            System.out.println("  - 存储类型: " + headOutput.getStorageClass());
            
            // 5. 测试删除文件
            System.out.println("\n[步骤5] 测试文件删除...");
            DeleteObjectInput deleteObjectInput = new DeleteObjectInput()
                    .setBucket(BUCKET_NAME)
                    .setKey(TEST_FILE_NAME);
            
            DeleteObjectOutput deleteOutput = tosClient.deleteObject(deleteObjectInput);
            System.out.println("✓ 文件删除成功");
            System.out.println("  - 删除标记: " + deleteOutput.isDeleteMarker());
            
            // 6. 验证文件已删除
            System.out.println("\n[步骤6] 验证文件已删除...");
            try {
                tosClient.headObject(headObjectInput);
                System.out.println("✗ 文件仍然存在，删除失败");
                allTestsPassed = false;
            } catch (TosServerException e) {
                if (e.getStatusCode() == 404) {
                    System.out.println("✓ 文件已成功删除，验证通过");
                } else {
                    throw e;
                }
            }
            
        } catch (TosClientException e) {
            System.err.println("\n✗ 客户端错误: " + e.getMessage());
            System.err.println("  可能原因: 网络问题、配置错误");
            e.printStackTrace();
            allTestsPassed = false;
            
        } catch (TosServerException e) {
            System.err.println("\n✗ 服务器错误: " + e.getMessage());
            System.err.println("  - 状态码: " + e.getStatusCode());
            System.err.println("  - 错误码: " + e.getCode());
            System.err.println("  - 请求ID: " + e.getRequestID());
            
            if (e.getStatusCode() == 403) {
                System.err.println("\n  可能原因:");
                System.err.println("  1. Access Key ID 或 Secret Access Key 不正确");
                System.err.println("  2. Secret Access Key 需要Base64解码");
                System.err.println("  3. 账户没有操作该Bucket的权限");
            } else if (e.getStatusCode() == 404) {
                System.err.println("\n  可能原因:");
                System.err.println("  1. Bucket不存在");
                System.err.println("  2. Region或Endpoint配置错误");
            }
            
            e.printStackTrace();
            allTestsPassed = false;
            
        } catch (Exception e) {
            System.err.println("\n✗ 未知错误: " + e.getMessage());
            e.printStackTrace();
            allTestsPassed = false;
            
        } finally {
            // 关闭客户端
            if (tosClient != null) {
                try {
                    tosClient.close();
                    System.out.println("\n[清理] TOS客户端已关闭");
                } catch (Exception e) {
                    System.err.println("关闭客户端时出错: " + e.getMessage());
                }
            }
        }
        
        // 测试结果总结
        System.out.println("\n" + "=".repeat(80));
        if (allTestsPassed) {
            System.out.println("✓✓✓ 所有测试通过！火山云TOS配置正确，可以正常使用！");
            System.out.println("接下来可以进行项目改造。");
        } else {
            System.out.println("✗✗✗ 测试失败！请检查配置参数。");
            System.out.println("\n建议检查项:");
            System.out.println("1. Secret Access Key是否需要Base64解码");
            System.out.println("2. Access Key ID和Secret Access Key是否正确");
            System.out.println("3. Bucket名称是否正确");
            System.out.println("4. Region和Endpoint是否匹配");
            System.out.println("5. 账户是否有访问Bucket的权限");
        }
        System.out.println("=".repeat(80));
        
        System.exit(allTestsPassed ? 0 : 1);
    }
}
