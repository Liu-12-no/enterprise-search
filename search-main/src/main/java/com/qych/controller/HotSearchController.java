package com.qych.controller;


import com.qych.utils.pojo.BaseResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/enterprise")
public class HotSearchController {
    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String HOT_SEARCH_KEY = "ent:search:hot_keywords";

    @GetMapping("/hot-keywords")
    public BaseResponse<List<String>> getHotKeywords(){

        //获取热搜前三名的
        Set<String> hotWordsSet = redisTemplate.opsForZSet().reverseRange(HOT_SEARCH_KEY, 0, 2);

        List<String> resultList = new ArrayList<>();

        //将set转换为list
        if(hotWordsSet!=null && hotWordsSet.size()>0){

            resultList.addAll(hotWordsSet);
        }
        //默认词库
        List<String> defaultWords = Arrays.asList("京东方","青岛海尔","宁德时代", "比亚迪", "科大讯飞");


        int index =0;
        while(resultList.size()<3 && index < defaultWords.size()){
            String defaultWord = defaultWords.get(index);
            //不能重复
            if(!resultList.contains(defaultWord)){
                resultList.add(defaultWord);
            }
            index++;
        }

        return BaseResponse.success(resultList);


    }
}
