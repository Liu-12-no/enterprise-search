package com.qych.component;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.qych.entity.pojos.EntBasicInfo;
import com.qych.mapper.EntBasicInfoMapper;
import lombok.extern.slf4j.Slf4j;
import org.ahocorasick.trie.Trie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CompanyCacheService {
    @Autowired
    private EntBasicInfoMapper entBasicInfoMapper;


    List<String> companyNamesList = new ArrayList<>();

    private Trie trie;


    //系统启动时的“缓存预热”初始化方法。
    @PostConstruct
    public void initCache() {
        refreshCache();
    }


    public void refreshCache(){

        log.info("正在从数据库动态加载企业词库...");

        List<Object> names = entBasicInfoMapper.selectObjs(new LambdaQueryWrapper<EntBasicInfo>().select(EntBasicInfo::getInstitutionName));

        if(names!=null){
            //数据库公司名
           companyNamesList = names.stream()
                   .map(Object::toString)
                   //去重
                   .distinct()
                   .collect(Collectors.toList());

           //构建Trie
            trie = Trie.builder()
                    //忽视大小写
                    .ignoreCase()
                    .addKeywords(companyNamesList)
                    .build();
            log.info("动态词库加载完成，共计 {} 家企业。", companyNamesList.size());
        }else {
            log.warn("⚠️ 数据库中未查询到企业信息，词库已清空。");
        }

    }

    public Trie getTrie() {
        return trie;
    }

    //获取动态词库
    public List<String> getCompanyNames() {

        return companyNamesList;
    }
}
