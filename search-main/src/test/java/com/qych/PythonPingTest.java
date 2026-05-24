package com.qych;

// 导入 Spring 框架自带的 HTTP 客户端工具类
import org.springframework.web.client.RestTemplate;

public class PythonPingTest {


    public static void main(String[] args) {


        // 实例化一个 RestTemplate，你可以把它理解为一个虚拟的浏览器
        RestTemplate restTemplate = new RestTemplate();


        // /ping 是你要访问的具体接口路径
        String pythonUrl = "http://127.0.0.1:8000/ping";

        // 打印一行日志，告诉你程序执行到哪了，方便找 Bug
        System.out.println(" [Java端] 正在跨越 8080 端口，向 Python 8000 端口发送心跳...");



        try {
            // getForObject 是 RestTemplate 最常用的方法之一：
            // 参数1 (pythonUrl): 请求的地址
            // 参数2 (String.class): 告诉 Java“无论 Python 返回什么格式（比如 JSON），都先当成纯文本字符串接住”
            String result = restTemplate.getForObject(pythonUrl, String.class);

            // 如果代码能走到这一行，说明 Python 成功响应了！打印出 Python 返回的 JSON 内容
            System.out.println("✅ [Java端] 世纪握手成功！收到 Python 侧回复：\n" + result);

        } catch (Exception e) {
            // 如果目标端口没开放，或者 Python 那边代码报错 500，就会跳到这里
            System.err.println("❌ [Java端] 通讯失败，看看是不是接口路径没对上：" + e.getMessage());
        }
    }
}