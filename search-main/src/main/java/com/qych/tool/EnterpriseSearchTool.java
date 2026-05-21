package com.qych.tool;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.qych.entity.dtos.EntAdvancedQueryDTO;
import com.qych.entity.pojos.EntBusinessOverview;
import com.qych.entity.vo.*;
import com.qych.mapper.EntShareholderMapper;
import com.qych.service.EntSearchService;
import com.qych.service.IEntBasicInfoService;
import com.qych.utils.SseManager;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class EnterpriseSearchTool {

    @Autowired
    private EntSearchService entSearchService;

    @Autowired
   private IEntBasicInfoService entBasicInfoService;




    @Tool("用于在企业数据库中检索详细的企业信息。当用户询问公司法人、注册资本、企业状态等信息时调用。"+
            "【系统级强制指令】：在你决定调用本工具前，绝对禁止输出任何文字（包括‘好的’、‘我先查一下’、‘为您查询’）。如果你敢在调用前输出半个字，系统将立刻崩溃！")
    public String searchEnterprise(
            @P("搜索关键词（支持具体的公司名、法人名，或18位统一社会信用代码）。" + "【致命警告】：如果是泛查询（如'所有企业'、'10年内的公司'），或者用户仅提到了'企业'、'公司'等通用名词，此字段【必须传空字符串 \"\"】！绝对禁止传 null，也绝对禁止把'企业'、'公司'等词作为关键词！") String keyword,
            @P("页码，默认填 1") Integer pageNum,
            @P("每页条数，默认填 10") Integer pageSize,
            @P("企业状态列表。仅在用户明确提到状态时从['存续','在业','注销','吊销','破产','清算']中选择，否则不传。") List<String> statusTags,
            @P("注册资本区间。必须严格从['0-100万','100-500万','500-1000万','1000万以上']中选择。" + "【逻辑指导】：如果用户提问的金额跨越了多个区间（例如用户问‘1000万以内’或‘1000万以下’），你需要利用数学常识，将涵盖的所有区间作为一个列表同时传入，例如：['0-100万','100-500万','500-1000万']；如果问‘500万以上’，则传入：['500-1000万','1000万以上']。") List<String> capitalTags,
            @P("成立年限。必须严格从['3个月内','半年内','1年内','1-3年','3-5年','5-10年','10年以上']中选择。【逻辑指导】：如果用户说‘10年以内’，你需要利用常识，将涵盖在该时间段内的所有标签作为一个列表同时传入，例如：['3个月内','半年内','1年内','1-3年','3-5年','5-10年']。") List<String> yearTags
    ) {
        //向前端推送思考流
        SseManager.sendToolLog("正在连接工商数据库，检索关键词: [" + keyword + "]");

        if ("null".equalsIgnoreCase(keyword) || "".equals(keyword)) {
            keyword = null;
        }
        log.info(">>> 命中 Tool 工具！正在组装参数查询数据库...");
        log.info(">>> AI 传入的参数: keyword=[{}], yearTags={}, capitalTags={},statusTags={}", keyword, yearTags, capitalTags,statusTags);

        // 1. 重新封装成 DTO 以复用原有的 Service 逻辑
        EntAdvancedQueryDTO dto = new EntAdvancedQueryDTO();
        dto.setKeyword(keyword);
        dto.setPageNum(pageNum == null ? 1 : pageNum);
        dto.setPageSize(pageSize == null ? 10 : pageSize);
        dto.setStatusTags(statusTags);
        dto.setCapitalTags(capitalTags);
        dto.setYearTags(yearTags);

        log.info("装载后的实体类dto:{}",dto);

        try {
            // 执行你原有的高级搜索逻辑（包含标签转 Range 的逻辑）
            Page<EntBasicInfoVO> result = entSearchService.advancedSearch(dto);

            if (result == null || result.getRecords().isEmpty()) {
                SseManager.sendToolLog("未查询到相关企业数据，准备向用户反馈。");
                return "未查询到符合条件的企业。";
            }

            // 查询成功，推送结果摘要
            SseManager.sendToolLog("检索完毕，共找到 " + result.getRecords().size() + " 条匹配数据。");

            // 格式化返回给 AI 的字符串
            return result.getRecords().stream().map(it -> String.format(
                    "【企业ID】:%s |【机构关联ID(institutionId)】:%s |【公司名称】:%s | 【法人】:%s | 【状态】:%s | 【注册资本】:%s | 【地址】:%s",
                    it.getId(),
                    it.getInstitutionId(),
                    it.getName(),
                    it.getLegalPerson() != null ? it.getLegalPerson() : "未知",
                    it.getStatusTag() != null ? it.getStatusTag() : "未知",
                    it.getRegCapital() != null ? it.getRegCapital() : "未知",
                    it.getAddress() != null ? it.getAddress() : "未知"
            )).collect(Collectors.joining("\n"));

        } catch (Exception e) {
            return "数据库查询出错: " + e.getMessage();
        }
    }


    @Tool("用于查询特定企业的深度详细信息（如：股东信息、主要人员、工商信息、专利信息等）。" +
            "【注意】：调用此工具前，必须先通过 searchEnterprise 工具获取到目标公司的【企业ID】,调用前【绝对禁止】回复用户任何废话，闭上嘴，直接调工具！。")
    public String getEnterpriseDetail(@P("企业的唯一标识 ID，必须从 searchEnterprise 工具的返回结果中提取") String id){

        SseManager.sendToolLog("正在提取企业 ID 为 " + id + " 的深度关联数据 (股东、专利等)...");
        log.info(">>> 命中 Tool 工具！正在查询企业详情，ID: {}", id);

        try {
            long longId = Long.parseLong(id);

            //根据id查询详细信息
            EntDetailVO entDetailVO = entBasicInfoService.getEnterpriseDetailById(longId);

            if(entDetailVO==null){
                return "未查询到该企业的详情信息";
            }
            if (entDetailVO.getPatentList() != null && entDetailVO.getPatentList().size() > 10) {
                entDetailVO.setPatentList(entDetailVO.getPatentList().subList(0, 10));
                // 用额外字段或假数据给大模型一个提示，让它知道数据没查完
                entDetailVO.setRemark("专利总数极多，此处仅展示最新10项，请建议用户去系统查看完整列表。");
            }

            if (entDetailVO.getShareholderList() != null && entDetailVO.getShareholderList().size() > 10) {
                entDetailVO.setShareholderList(entDetailVO.getShareholderList().subList(0, 10));

            }

            return JSON.toJSONString(entDetailVO,
                    SerializerFeature.IgnoreNonFieldGetter, // 忽略那些没有实际字段的 get 方法
                    SerializerFeature.SkipTransientField  // 忽略序列化不需要的字段
                    );


        } catch (Exception e) {

            log.info(e.getMessage());

            return "深度详情查询失败"+e.getMessage();

        }





    }
    /**
     * 股权与高管穿透
     */
    @Tool("查询企业的核心股东结构、持股比例及高管名册。当你需要分析企业的资本背景、实际控制人或管理层实力时调用。")
    public String getPersonnelDetail(
            @P("机构关联ID (institutionId)，必须从基础检索工具的结果中获取") String institutionId
    ){
        log.info("命中工具Tool：股权与高管穿透");
        SseManager.sendToolLog("正在穿透股权结构，调取高管与股东名册...");
        try {
            //股东信息
            List<EntShareholderVO> entShareholderVOS = entBasicInfoService.getEntShareholderVOS(institutionId);

            //高管信息
            List<EntExecutiveVO> executiveVOS = entBasicInfoService.getExecutiveVOS(institutionId);

            StringBuilder stringBuilder = new StringBuilder("【人员档案检索结果】\n");

            stringBuilder.append("核心股东：").append(entShareholderVOS.isEmpty() ? "暂无数据" : JSON.toJSONString(entShareholderVOS)).append("\n");

            stringBuilder.append("高管成员：").append(executiveVOS.isEmpty() ? "暂无数据" : JSON.toJSONString(executiveVOS));


            return stringBuilder.toString();
        } catch (Exception e) {
            log.error("获取人员档案失败: {}",e);
            return "获取人员档案失败: " + e.getMessage();
        }
    }

    /**
     * 技术与荣誉资产（研发实力分析）
     */
    @Tool("检索企业的知识产权（专利）和荣誉资质。当用户询问‘研发实力如何’、‘有什么证书’或‘技术水平’时调用。" +
            "【专业指导】：对于机械制造或自动化企业，请重点关注其发明专利的含金量。")
    public String getTechnicalAssets(
            @P("机构关联ID (institutionId)") String institutionId,
            @P("公司全称 (institutionName)") String institutionName
    ){
        log.info("命中工具Tool：技术与荣誉资产（研发实力分析）");
        SseManager.sendToolLog("正在扫描专利库与资质证书，评估技术护城河...");

        try {
            //专利信息
            List<EntPatentVO> entPatentVOS = entBasicInfoService.getEntPatentVOS(institutionId);

            //企业资质
            List<EntQualificationVO> entQualificationVOS = entBasicInfoService.getEntQualificationVOS(institutionName);
            String s = String.format("【技术资产清单】\n专利项数：%d | 详细列表：%s\n资质荣誉项数：%d | 详细列表：%s",
                    entPatentVOS.size(), JSON.toJSONString(entPatentVOS), entQualificationVOS.size(), JSON.toJSONString(entQualificationVOS));

            return s;
        } catch (Exception e) {
            log.error("技术资产检索失败: {}",e);
            return "技术资产检索失败: " + e.getMessage();
        }

    }

    /**
     * 业务情报与产品概览（主营业务分析）
     */
    @Tool("获取企业的业务深度描述、主营业务和主导产品信息。当用户问‘这家公司是做什么的’、‘有什么产品’或分析‘核心业务逻辑’时调用。")
    public String getBusinessContext(
            @P("公司全称 (institutionName)，必须从基础检索结果中获取") String institutionName
    ){
        log.info("命中工具Tool：业务情报与产品概览（主营业务分析）");
        SseManager.sendToolLog("正在调取企业业务概览及主导产品数据...");


        try {
            //主导产品及业务概览信息
            EntBusinessOverview entBusinessOverview = entBasicInfoService.getEntBusinessOverview(institutionName);

            String businessOverview = entBusinessOverview.getBusinessOverview();

            if(businessOverview==null || businessOverview.trim().isEmpty()){
                return "【业务情报】\n暂无该企业的主营业务详细描述。";
            }
            return String.format("【业务情报分析】\n核心业务与概览描述：%s", businessOverview);
        } catch (Exception e) {
            log.error("业务情报调取失败: {}", e);
            return "业务情报调取失败: " + e.getMessage();
        }

    }






}
