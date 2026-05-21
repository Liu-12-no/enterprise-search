package com.qych.config;

import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

@Configuration
@EnableSwagger2WebMvc
@EnableKnife4j
public class Knife4jConfig {
    @Bean
    public Docket defaultApi2() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(new ApiInfoBuilder()
                        .title("企业搜索系统接口文档")
                        .version("1.0")
                        .build())
                .select()
                // 这里要指向你的 Controller 所在的包名
                .apis(RequestHandlerSelectors.basePackage("com.qych.controller"))
                .paths(PathSelectors.any())
                .build();
    }
}
