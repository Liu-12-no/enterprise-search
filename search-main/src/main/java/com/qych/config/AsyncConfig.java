package com.qych.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;


/**
 * 专属异步线程池，处理非核心业务的耗时操作
 */
@Configuration
//开启spring异步支持
@EnableAsync
public class AsyncConfig {

    public static final String CACHE_EXECUTOR = "cacheAsyncExecutor";

    @Bean(name = CACHE_EXECUTOR)
    public Executor cacheAsyncExecutor(){

        //实例化Spring包装的异步执行器
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        //核心线程数
        threadPoolTaskExecutor.setCorePoolSize(10);
        //最大线程数
        threadPoolTaskExecutor.setMaxPoolSize(20);
        //队列容量
        threadPoolTaskExecutor.setQueueCapacity(200);
        //线程前缀
        threadPoolTaskExecutor.setThreadNamePrefix("Cache-Async-");
        //拒绝策略，如果排队队列也满了，新来的请求丢回给发起任务的那个请求同步执行
        threadPoolTaskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        //初始化并启动线程池
        threadPoolTaskExecutor.initialize();

        return threadPoolTaskExecutor;
    }
}
