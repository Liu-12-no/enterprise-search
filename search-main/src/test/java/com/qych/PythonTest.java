package com.qych;

import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

public class PythonTest {
    public static void main(String[] args) {

        RestTemplate restTemplate = new RestTemplate();

        String url = "http://127.0.0.1:8000/api/parse/pdf";

        //设置请求头
        HttpHeaders headers = new HttpHeaders();
        //传送文件类型为文件表单
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        //封装请求体
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        body.add("file", new FileSystemResource("E:\\利用大模型从各类报告中提取企业信息\\青岛征信服务有限公司\\项目申报\\6.21和22年税务汇算清缴材料.pdf"));

        //Post请求
        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        System.out.println("正在向 Python 微服务发送文件，请稍候...");


        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
            System.out.println("✅ 解析成功！Python 返回的 JSON 如下：");
            System.out.println(response.getBody());
        } catch (RestClientException e) {
            System.out.println("❌ 解析失败，报错信息：" + e.getMessage());
        }


    }
}
