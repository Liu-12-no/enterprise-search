package com.qych.utils;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;

import java.util.Collections;

public class CodeGenerator {
    public static void main(String[] args) {
        FastAutoGenerator.create("jdbc:mysql://192.168.200.130:3306/company_db", "root", "root")
                .globalConfig(builder -> {
                    builder.author("byl") // 设置作者
                            .commentDate("yyyy-MM-dd")
                            .outputDir(System.getProperty("user.dir") + "/src/main/java"); // 输出目录
                })
                .packageConfig(builder -> {
                    builder.parent("com.qych") // 设置父包名
                            .entity("entity")
                            .mapper("mapper")
                            .service("service")
                            .controller("controller")
                            .pathInfo(Collections.singletonMap(OutputFile.xml, System.getProperty("user.dir") + "/src/main/resources/mapper"));
                })
                .strategyConfig(builder -> {
                    builder.addInclude("ent_basic_info","ent_shareholder","ent_executive","ent_patent","ent_qualification") // 包含这5张表
                            .entityBuilder()
                            .enableLombok() // 使用Lombok
                            .enableTableFieldAnnotation() // 开启字段注解
                            .controllerBuilder()
                            .enableRestStyle(); // 开启RestController模式
                })
                .templateEngine(new VelocityTemplateEngine())
                .execute();
    }
}
