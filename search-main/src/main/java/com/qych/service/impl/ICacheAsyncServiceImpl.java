package com.qych.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qych.config.AsyncConfig;
import com.qych.entity.vo.EntBasicInfoVO;
import com.qych.entity.vo.EntDetailVO;
import com.qych.service.ICacheAsyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class ICacheAsyncServiceImpl implements ICacheAsyncService {


    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 异步回填企业搜索结果缓存
     *
     * @param cacheKey
     * @param entityPage
     */
    @Override
    @Async(AsyncConfig.CACHE_EXECUTOR)
    public void saveSearchCache(String cacheKey, Page<EntBasicInfoVO> entityPage) {

        try {
            //耗时的json序列化
            String jsonString = JSON.toJSONString(entityPage);
            //将结果回填到redis
            redisTemplate.opsForValue().set(cacheKey,jsonString,1, TimeUnit.HOURS);
            log.info("异步回填关键字查询Redis成功, cacheKey: {}, 线程: {}", cacheKey, Thread.currentThread().getName());
        } catch (Exception e) {
            log.error("Redis 连接失败: {}", e.getMessage());
        }
    }

    /**
     * 异步回填企业详情缓存
     *
     * @param cacheKey
     * @param vo
     */
    @Override
    @Async(AsyncConfig.CACHE_EXECUTOR)
    public void saveDetailCache(String cacheKey, EntDetailVO vo) {
        try {
            // 详情对象的序列化
            String jsonString = JSON.toJSONString(vo);
            // 企业详情一般不经常变动，缓存时间2小时
            redisTemplate.opsForValue().set(cacheKey, jsonString, 2, TimeUnit.HOURS);
            log.info("异步回填企业详情Redis成功, cacheKey: {}, 线程: {}", cacheKey, Thread.currentThread().getName());
        } catch (Exception e) {
            log.error("异步回填企业详情Redis失败: {}", e.getMessage());
        }
    }
}
