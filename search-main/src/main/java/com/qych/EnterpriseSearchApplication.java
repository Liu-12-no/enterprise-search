package com.qych;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.qych.mapper")
public class EnterpriseSearchApplication {
    public static void main(String[] args) {
        SpringApplication.run(EnterpriseSearchApplication.class, args);
    }
}