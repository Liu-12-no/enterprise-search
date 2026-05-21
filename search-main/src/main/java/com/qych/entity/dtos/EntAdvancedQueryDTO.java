package com.qych.entity.dtos;

import dev.langchain4j.model.output.structured.Description;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 高级搜索实体类
 */
@Data
public class EntAdvancedQueryDTO {

    @Description("搜索关键词，例如：公司名称、法人名称、社会统一性代码等。")
    private String keyword;

    /**
     * 分页参数
     */
    @Description("页码，默认填 1")
    private Integer pageNum = 1;

    /**
     * 分页参数
     */
    @Description("每页条数，默认填 10")
    private Integer pageSize = 10;

    
    /**
     * 登记状态多选标签集合（比如：["存续", "注销", "吊销"]）
     */
    @Description("企业状态。根据语境推断，输出确切的中文词汇，如：'存续', '在业', '注销', '吊销', '破产', '清算'等。若用户未提及，务必为空！")
    private List<String> statusTags;  
    
    /**
     * 注册资本多选标签集合（比如["100万以下", "100-500万", "1000万以上"]）
     * */
    @Description("注册资本。必须、严格、只能从以下固定选项中挑选并输出原话：['0-100万', '100-500万', '500-1000万', '1000万以上']。若用户未提及，务必为空！")
    private List<String> capitalTags; 
    
    /**
     * 成立年限多选标签集合（比如：["1年内", "1-3年", "10年以上"]）
     */
    @Description("成立年限。必须、严格、只能从以下固定选项中挑选并输出原话：['3个月内', '半年内', '1年内', '1-3年', '3-5年', '5-10年', '10年以上']。若用户未提及，务必为空！")
    private List<String> yearTags;    


    
    /**
     *翻译后的状态码集合
     */
    private List<String> queryStateCodes; 
    
    /**
     * 解析后的注册资本边界集合
     */
    private List<Range> capitalRanges;
    
    /**
     *逆推计算后的成立日期边界集合
     */
    private List<Range> yearRanges;

    /**
     * 内部通用辅助类：区间边界封装
     */
    @Data
    @NoArgsConstructor  // 🌟 必须加，反射的命门
    @AllArgsConstructor //
    public static class Range {
        /**
         * 区间下限
         */
        private Object min; 
        
        /**
         *区间上限
         */
        private Object max;
    }
}