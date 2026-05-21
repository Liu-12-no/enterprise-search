package com.qych.utils;

import io.minio.*;

import io.minio.errors.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
@Slf4j
public class MinioUtil {

    @Value("${spring.minio.endpoint}")
    private String endpoint;

    @Value("${spring.minio.accessKey}")
    private String accessKey;

    @Value("${spring.minio.secretKey}")
    private String secretKey;

    @Value("${spring.minio.bucketName}")
    private String bucketName;


    private MinioClient minioClient;

    /**
     * 初始化方法：在Spring Bean的属性注入完成后自动执行
     * 作用：创建MinIO客户端实例并初始化
     * 执行时机：构造方法执行完毕 → 所有@Value属性注入完成 → 执行@PostConstruct方法
     */
    @PostConstruct
    public void init() {
        try {
            minioClient = MinioClient.builder()
                    .endpoint(endpoint)
                    .credentials(accessKey, secretKey)
                    .build();

            // 新加这段：自动检查并创建桶
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {

                log.info("检测到 MinIO 桶 [{}] 不存在，正在自动创建...", bucketName);
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());

                //桶创建成功后，立刻自动配置公共只读权限，代替手动去网页上点 Add
                log.info("正在为桶 [{}] 自动配置公共只读权限...", bucketName);
                String policyJson = "{\n" +
                        "  \"Version\": \"2012-10-17\",\n" +
                        "  \"Statement\": [\n" +
                        "    {\n" +
                        "      \"Effect\": \"Allow\",\n" +
                        "      \"Principal\": {\"AWS\": [\"*\"]},\n" +
                        "      \"Action\": [\"s3:GetBucketLocation\", \"s3:ListBucket\"],\n" +
                        "      \"Resource\": [\"arn:aws:s3:::" + bucketName + "\"]\n" +
                        "    },\n" +
                        "    {\n" +
                        "      \"Effect\": \"Allow\",\n" +
                        "      \"Principal\": {\"AWS\": [\"*\"]},\n" +
                        "      \"Action\": [\"s3:GetObject\"],\n" +
                        "      \"Resource\": [\"arn:aws:s3:::" + bucketName + "/*\"]\n" +
                        "    }\n" +
                        "  ]\n" +
                        "}";

                minioClient.setBucketPolicy(
                        io.minio.SetBucketPolicyArgs.builder()
                                .bucket(bucketName)
                                .config(policyJson)
                                .build()
                );

                log.info("桶 [{}] 创建并公共只读授权成功！", bucketName);

            }
        } catch (Exception e) {
            log.error("MinIO 初始化异常：", e);
        }
    }

    public String uploadFile(MultipartFile file) throws Exception {

        //获取当前时间并格式化
        String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));

        //获取文件名
        String originalFilename = file.getOriginalFilename();
        //生成唯一的文件名
        String uuidName = UUID.randomUUID().toString().replace("-", "") + "_" + originalFilename;


        //拼接成minio中对象名
        String objectName = datePath + '/' + uuidName;

        // 🌟 核心修改点：构建 Headers，告诉浏览器这是用来“预览”的，不是用来“下载”的！
        java.util.Map<String, String> headers = new java.util.HashMap<>();
        // inline 表示内联显示，这是干掉下载框的唯一真理
        headers.put("Content-Disposition", "inline; filename=\"" + java.net.URLEncoder.encode(originalFilename, "UTF-8") + "\"");


        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .headers(headers)
                        .build()
        );

        return endpoint + "/" + bucketName + "/" + objectName;
    }

    public void removeObject(String fileName){

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );
            log.info("MinIO 文件粉碎成功: 桶:{}, 文件:{}", bucketName, fileName);
        } catch (Exception e) {
            log.error("MinIO 文件粉碎失败: 桶:{}, 文件:{}。原因: {}", bucketName, fileName, e.getMessage());
        }
    }

    public InputStream getObjectStream(String fileName) {
        try {
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );
            log.info("获取文件流成功: {}" , fileName);
            return stream;
        } catch (Exception e) {
            log.error("获取文件流失败:{}" , fileName, e);
            throw new RuntimeException(e);
        }

    }
}
