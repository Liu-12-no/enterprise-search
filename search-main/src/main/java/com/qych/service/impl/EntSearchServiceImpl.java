package com.qych.service.impl;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qych.entity.dtos.EntAdvancedQueryDTO;
import com.qych.entity.es.EntBasicInfoDoc;
import com.qych.entity.vo.EntBasicInfoVO;
import com.qych.factory.EsSearchQueryFactory;
import com.qych.mapper.EntBasicInfoMapper;
import com.qych.service.EntSearchService;
import com.qych.utils.EntStateDictUtil;
import com.qych.utils.exception.BaseException;
import dev.langchain4j.internal.Json;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class EntSearchServiceImpl implements EntSearchService {

    @Autowired
    private EntBasicInfoMapper entBasicInfoMapper;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Override
    public Page<EntBasicInfoVO> advancedSearch(EntAdvancedQueryDTO dto) {

        long start = System.currentTimeMillis();
        //将dto经过md5加密
        String dtoHash = DigestUtils.md5DigestAsHex(JSON.toJSONString(dto).getBytes());

        //redis中的key
        String cacheKey = "ent:search:advanced:" + dtoHash + ":" + dto.getPageNum();


        //翻译状态标签
        parseStatusTags(dto);


        //翻译注册资本
        parseCapitalTags(dto);

        //翻译成立年限
        parseYearTags(dto);

        //定义分页参数
        Page<EntBasicInfoVO> page = new Page<>(dto.getPageNum(), dto.getPageSize());


        try {
            String json = redisTemplate.opsForValue().get(cacheKey);
            if(StringUtils.isNotBlank(json)){
                log.info("redis命中成功");
                return JSON.parseObject(json,new TypeReference<Page<EntBasicInfoVO>>(){});
            }
        } catch (Exception e) {
            log.error("redis连接异常"+e.getMessage());
        }


        log.info("dto的年限:{}",dto.getYearRanges());
        //构建 Elasticsearch 原生查询对象
        NativeSearchQuery nativeSearchQuery = EsSearchQueryFactory.buildAdvancedSearchQuery(dto);

        //执行 ES 搜索操作，获取命中结果
        SearchHits<EntBasicInfoDoc> searchHits = elasticsearchRestTemplate.search(nativeSearchQuery, EntBasicInfoDoc.class);


        List<EntBasicInfoVO> entBasicInfoVOS = searchHits.getSearchHits().stream().map(ent -> {

            //业务中真实实体对象
            EntBasicInfoDoc content = ent.getContent();

            EntBasicInfoVO entBasicInfoVO = new EntBasicInfoVO();


            BeanUtils.copyProperties(content, entBasicInfoVO);

            //注册资本
            if (content.getRegCapital() != null) {

                entBasicInfoVO.setRegCapital(String.valueOf(content.getRegCapital()));
            }

            //企业状态转换
            if(entBasicInfoVO.getStatusTag()!=null){
                entBasicInfoVO.setStatusTag(EntStateDictUtil.getLabel(entBasicInfoVO.getStatusTag()));
            }

            // 替换名称高亮
            List<String> highlightName = ent.getHighlightField("name");
            if(highlightName != null && !highlightName.isEmpty()){
                entBasicInfoVO.setName(highlightName.get(0));
            }

            // 替换法人高亮
            List<String> highlightLegal = ent.getHighlightField("legalPerson");
            if(highlightLegal != null && !highlightLegal.isEmpty()){
                entBasicInfoVO.setLegalPerson(highlightLegal.get(0));
            }

            // 替换信用代码高亮
            List<String> highlightCreditCode = ent.getHighlightField("creditCode");
            if(highlightCreditCode != null && !highlightCreditCode.isEmpty()){
                entBasicInfoVO.setCreditCode(highlightCreditCode.get(0));
            }
            return entBasicInfoVO;

        }).collect(Collectors.toList());

        page.setRecords(entBasicInfoVOS);

        //获取查询到的总数
        page.setTotal(searchHits.getTotalHits());


        try {
            //将结果回填到redis
            redisTemplate.opsForValue().set(cacheKey,JSON.toJSONString(page),1, TimeUnit.HOURS);
            log.info("将数据回填redis成功");
        } catch (Exception e) {
            log.error("Redis 连接失败: {}", e.getMessage());
        }


        //mysql中查询
//        Page<EntBasicInfoVO> entBasicInfoVOPage = entBasicInfoMapper.advancedSearchEnterprise(page, dto);

        log.info("总耗时:{}ms",System.currentTimeMillis()-start);
        return page;
    }

    private static void parseYearTags(EntAdvancedQueryDTO dto) {
        //翻译成立年限
        if(dto.getYearTags()!=null && dto.getYearTags().size()>0){
            List<EntAdvancedQueryDTO.Range> yearRanges = new ArrayList<>();
            LocalDate now = LocalDate.now();
            for (String tag : dto.getYearTags()) {
                EntAdvancedQueryDTO.Range r = new EntAdvancedQueryDTO.Range();
                if ("3个月内".equals(tag)) {
                    r.setMin(now.minusMonths(3).toString());
                    r.setMax(now.toString());
                } else if ("半年内".equals(tag)) {
                    r.setMin(now.minusMonths(6).toString());
                    r.setMax(now.toString());
                } else if ("1年内".equals(tag)) {
                    r.setMin(now.minusYears(1).toString());
                    r.setMax(now.toString());
                } else if ("1-3年".equals(tag)) {
                    r.setMin(now.minusYears(3).toString());
                    r.setMax(now.minusYears(1).toString());
                } else if ("3-5年".equals(tag)) {
                    r.setMin(now.minusYears(5).toString());
                    r.setMax(now.minusYears(3).toString());
                } else if ("5-10年".equals(tag)) {
                    r.setMin(now.minusYears(10).toString());
                    r.setMax(now.minusYears(5).toString());
                } else if ("10年以上".equals(tag)) {
                    r.setMin(null); // 没有下限，无限早
                    r.setMax(now.minusYears(10).toString());
                }
                yearRanges.add(r);
            }
            dto.setYearRanges(yearRanges);

        }
    }

    private static void parseCapitalTags(EntAdvancedQueryDTO dto) {
        //翻译注册资本
        if(dto.getCapitalTags()!=null && dto.getCapitalTags().size()>0){
            List<EntAdvancedQueryDTO.Range> capitalRanges = new ArrayList<>();
            for (String tag : dto.getCapitalTags()) {
                EntAdvancedQueryDTO.Range r = new EntAdvancedQueryDTO.Range();
                if("0-100万".equals(tag)){
                    r.setMin(0L);
                    r.setMax(1000000L);
                }else if ("100-500万".equals(tag)) {
                    r.setMin(1000000L);
                    r.setMax(5000000L);
                } else if ("500-1000万".equals(tag)) {
                    r.setMin(5000000L);
                    r.setMax(10000000L);
                } else if ("1000万以上".equals(tag)) {
                    r.setMin(10000000L);
                    // 最大值设为 Long 的极限值
                    r.setMax(Long.MAX_VALUE);
                }
                capitalRanges.add(r);
            }
            dto.setCapitalRanges(capitalRanges);
        }
    }

    private static void parseStatusTags(EntAdvancedQueryDTO dto) {
        //翻译状态标签
        if (dto.getStatusTags() != null && dto.getStatusTags().size() > 0){
            List<String> codes = new ArrayList<>();
            for (String statusTag : dto.getStatusTags()) {
                String code = EntStateDictUtil.getCode(statusTag);
                if(code != null){
                    codes.add(code);
                }
            }
            //去重
            dto.setQueryStateCodes(codes.stream().distinct().collect(Collectors.toList()));
        }
    }
}
