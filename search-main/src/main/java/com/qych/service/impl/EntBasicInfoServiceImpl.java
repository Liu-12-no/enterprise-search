package com.qych.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qych.entity.dtos.EntBasicInfoQueryDTO;
import com.qych.entity.es.EntBasicInfoDoc;
import com.qych.entity.pojos.*;
import com.qych.entity.vo.*;
import com.qych.factory.EsSearchQueryFactory;
import com.qych.mapper.*;
import com.qych.service.ICacheAsyncService;
import com.qych.service.IEntBasicInfoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qych.utils.EntStateDictUtil;
import com.qych.utils.exception.BaseException;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * <p>
 * 企业基础信息表 服务实现类
 * </p>
 *
 * @author byl
 * @since 2026-04-17
 */
@Service
@Slf4j
public  class EntBasicInfoServiceImpl extends ServiceImpl<EntBasicInfoMapper, EntBasicInfo> implements IEntBasicInfoService {


    @Autowired
    private  EntBasicInfoMapper entBasicInfoMapper;
    @Autowired
    private EntShareholderMapper shareholderMapper;
    @Autowired
    private EntExecutiveMapper executiveMapper;
    @Autowired
    private EntPatentMapper patentMapper;
    @Autowired
    private EntQualificationMapper qualificationMapper;
    @Autowired
    EntBusinessOverviewMapper businessOverviewMapper;

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ICacheAsyncService cacheAsyncService;

    //热搜排行榜的Redis key
    private static final String HOT_SEARCH_KEY = "ent:search:hot_keywords";

    /**
     * 根据关键词搜索
     * @param dto
     * @return
     */
    @Override
    public Page<EntBasicInfoVO> searchByKeyword(EntBasicInfoQueryDTO dto) {

        long start = System.currentTimeMillis();


       if(StringUtils.isNotBlank(dto.getKeyword()) && dto.getKeyword().length() > 1){

           redisTemplate.opsForZSet().incrementScore(HOT_SEARCH_KEY,dto.getKeyword(),1);
       }

        //构造缓存key
        String cacheKey = "ent:search:keyword:" + dto.getKeyword() + ":page:" + dto.getPageNum();



        try {
            //先从redis中查询数据
            String json = redisTemplate.opsForValue().get(cacheKey);
            if(StringUtils.isNotBlank(json)){

                log.info("redis缓存命中");
                return JSON.parseObject(json,new TypeReference<Page<EntBasicInfoVO>>(){});
            }
        } catch (Exception e) {

            throw new BaseException("redis连接异常"+e.getMessage());
        }


        if(StringUtils.isBlank(dto.getKeyword())){
            throw new BaseException("请输入关键字");
        }
        //分页参数
        Page<EntBasicInfoVO> entityPage = new Page<>(dto.getPageNum(), dto.getPageSize());

        //关键字
//        String keyword = dto.getKeyword();
        //查询基本信息mysql查询
//        Page<EntBasicInfoVO> entBasicInfoVOPage = entBasicInfoMapper.searchEnterprise(entityPage, keyword);


        //构建 Elasticsearch 原生查询对象
        NativeSearchQuery nativeSearchQuery = EsSearchQueryFactory.buildBasicSearchQuery(dto);

        //执行 ES 搜索操作，获取命中结果
        SearchHits<EntBasicInfoDoc> searchHits = elasticsearchRestTemplate.search(nativeSearchQuery, EntBasicInfoDoc.class);


        List<EntBasicInfoVO> entBasicInfoVOS = searchHits.getSearchHits().stream().map(ent -> {

            //获取实体对象
            EntBasicInfoDoc content = ent.getContent();

            EntBasicInfoVO entBasicInfoVO = new EntBasicInfoVO();

            BeanUtils.copyProperties(content, entBasicInfoVO);

            //注册资本
            if (content.getRegCapital() != null) {
                entBasicInfoVO.setRegCapital(String.valueOf(content.getRegCapital()));
            }

            //企业状态转换
            if (entBasicInfoVO.getStatusTag() != null) {
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


        entityPage.setRecords(entBasicInfoVOS);

        //获取查询到的总数
        entityPage.setTotal(searchHits.getTotalHits());

        //异步回填企业搜索结果缓存
        cacheAsyncService.saveSearchCache(cacheKey,entityPage);

        log.info("总耗时:{}ms",System.currentTimeMillis()-start);
        return entityPage;
    }

    @Override
    public EntDetailVO getEnterpriseDetailById(Long id) {

        //  记录方法开始时间
        long start = System.currentTimeMillis();
        //构造详情查询的专用key
        String cacheKey = "ent:detail:id:" + id;

        try {
            String detailJson = redisTemplate.opsForValue().get(cacheKey);
            if(StringUtils.isNotBlank(detailJson)){
                log.info("⚡ 企业详情[缓存命中]成功, id: {}, 耗时: {} ms", id, System.currentTimeMillis() - start);
                EntDetailVO entDetailVO = JSON.parseObject(detailJson, EntDetailVO.class);
                return entDetailVO;
            }
        } catch (Exception e) {

            log.error("读取企业详情缓存异常: {}", e.getMessage());
        }

        //查询基本信息
        EntBasicInfo basicInfo = getById(id);
        if(basicInfo==null){
            throw new BaseException("未找到相关企业信息");
        }

        String institutionId = basicInfo.getInstitutionId();
        String institutionName = basicInfo.getInstitutionName();

        if(StringUtils.isBlank(institutionId)||StringUtils.isBlank(institutionName)){
            throw new BaseException("系统异常：无法获取详情数据");
        }


        EntDetailVO vo = new EntDetailVO();
        BeanUtils.copyProperties(basicInfo,vo);

        //vo实体填充信息
        fillBasicInfo(vo, basicInfo);


        //查询股东信息
        List<EntShareholderVO> shvolist = getEntShareholderVOS(institutionId);

        //查询高管信息
        List<EntExecutiveVO> evolist = getExecutiveVOS(institutionId);


        //查询专利信息
        List<EntPatentVO> epvolist = getEntPatentVOS(institutionId);

        //查询企业资质信息
        List<EntQualificationVO> eqvolist = getEntQualificationVOS(institutionName);

        //查询主导产品及概览
        EntBusinessOverview entBusinessOverview = getEntBusinessOverview(institutionName);

        if(entBusinessOverview != null &&StringUtils.isNotBlank(entBusinessOverview.getBusinessOverview())){
            vo.setBusinessOverview(entBusinessOverview.getBusinessOverview());
        }


        //填充高管信息列表
        vo.setExecutiveList(evolist);
        //填充股东信息列表
        vo.setShareholderList(shvolist);
        //填充专利信息列表
        vo.setPatentList(epvolist);
        //填充企业资质信息列表
        vo.setQualificationList(eqvolist);

        cacheAsyncService.saveDetailCache(cacheKey,vo);

        log.info("🐢 企业详情[走数据库]查询完毕并触发异步回填, id: {}, 总耗时: {} ms", id, System.currentTimeMillis() - start);

        return vo;
    }

    //查询主导产品及概览
    public EntBusinessOverview getEntBusinessOverview(String institutionName) {
        EntBusinessOverview entBusinessOverview = businessOverviewMapper.selectOne(
                new LambdaQueryWrapper<EntBusinessOverview>().eq(EntBusinessOverview::getInstitutionName, institutionName)
        );
        return entBusinessOverview;
    }

    private static void fillBasicInfo(EntDetailVO vo, EntBasicInfo basicInfo) {
        //公司名称
        vo.setName(basicInfo.getInstitutionName());
        //统一社会信用代码
        vo.setCreditCode(basicInfo.getSocialCreditCode());
        //注册地址
        vo.setAddress(basicInfo.getOfficeAddress());
        //注册资本
        if(basicInfo.getRegisteredCapital()!=null){

            vo.setRegCapital(String.valueOf(basicInfo.getRegisteredCapital()));
        }
        //成立日期
        if(basicInfo.getEstablishDate() != null){
            vo.setEstablishDate(String.valueOf(basicInfo.getEstablishDate()));
        }
        //工商注册号映射
        vo.setRegNumber(basicInfo.getRegNumber());
        //行业 (主营业务)
        vo.setIndustry(basicInfo.getMainBussinessIndustry());
        //企业类型
        vo.setBusinessType(basicInfo.getEnterpriseNature());

    }

    /**
     * 查询企业资质
     * @param institutionName
     * @return
     */
    public List<EntQualificationVO> getEntQualificationVOS(String institutionName) {
        //查询企业资质
        List<EntQualification> entQualifications = qualificationMapper.selectList(
                new LambdaQueryWrapper<EntQualification>().eq(EntQualification::getCompanyName, institutionName)
        );

        List<EntQualificationVO> eqvolist = entQualifications.stream().map(entity -> {
            EntQualificationVO eqvo = new EntQualificationVO();

            BeanUtils.copyProperties(entity, eqvo);

            if (entity.getAwardYear() != null) {
                eqvo.setYear(String.valueOf(entity.getAwardYear()));
            }

            return eqvo;
        }).collect(Collectors.toList());
        return eqvolist;
    }

    /**
     * 查询专利信息
     * @param institutionId
     * @return
     */
    public List<EntPatentVO> getEntPatentVOS(String institutionId) {
        //查询专利信息
        List<EntPatent> entPatents = patentMapper.selectList(
                new LambdaQueryWrapper<EntPatent>().eq(EntPatent::getInstitutionId, institutionId)
        );

        List<EntPatentVO> epvolist = entPatents.stream().map(entity->{
            EntPatentVO epvo = new EntPatentVO();

            BeanUtils.copyProperties(entity,epvo);

            //申请日期
            if(entity.getApplicationDate()!=null){
                epvo.setApplicationDate(String.valueOf(entity.getApplicationDate()));
            }
            //专利类型
            if(entity.getType()!=null){
                String type ;
                switch (entity.getType().toUpperCase()){
                    case "I": type = "发明专利"; break;
                    case "U": type = "实用新型"; break;
                    case "D": type = "外观设计"; break;
                    default: type = entity.getType();
                }
                epvo.setType(type);
            }
            return epvo;

        }).collect(Collectors.toList());
        return epvolist;
    }

    /**
     * 查询高管信息
     * @param institutionId
     * @return
     */
    public List<EntExecutiveVO> getExecutiveVOS(String institutionId) {
        //查询高管信息
        List<EntExecutive> entExecutives = executiveMapper.selectList(
                new LambdaQueryWrapper<EntExecutive>().eq(EntExecutive::getInstitutionId, institutionId)
        );

        List<EntExecutiveVO> evolist = entExecutives.stream().map(entity->{
            EntExecutiveVO evo = new EntExecutiveVO();

            BeanUtils.copyProperties(entity,evo);

            return evo;

        }).collect(Collectors.toList());
        return evolist;
    }

    /**
     * 查询股东信息
     * @param institutionId
     * @return
     */
    public List<EntShareholderVO> getEntShareholderVOS(String institutionId) {
        //查询股东信息
        List<EntShareholder> entShareholders = shareholderMapper.selectList(
                new LambdaQueryWrapper<EntShareholder>().eq(EntShareholder::getInstitutionId, institutionId)
        );
        List<EntShareholderVO> shvolist = entShareholders.stream().map(entity ->{

            EntShareholderVO shvo = new EntShareholderVO();

            BeanUtils.copyProperties(entity,shvo);
            //股东类型
            if(entity.getShareholderType() !=null){
                shvo.setShareholderType(String.valueOf(entity.getShareholderType()));
            }
            //所有权比例
            if(entity.getOwnershipProportion()!=null){
                shvo.setOwnershipProportion(String.valueOf(entity.getOwnershipProportion())+ "%");
            }

            return shvo;

        }).collect(Collectors.toList());
        return shvolist;
    }
}
