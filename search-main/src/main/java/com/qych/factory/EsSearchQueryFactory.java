package com.qych.factory;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.qych.entity.dtos.EntAdvancedQueryDTO;
import com.qych.entity.dtos.EntBasicInfoQueryDTO;
import com.qych.utils.exception.BaseException;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;

public class EsSearchQueryFactory {

    /**
     * 根据首页传入的关键字查询
     * @param dto
     * @return
     */
    public static NativeSearchQuery buildBasicSearchQuery(EntBasicInfoQueryDTO dto){

        //创建一个布尔查询主容器
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();


        //关键字全局搜索
        boolQueryBuilder.must(QueryBuilders.multiMatchQuery(
                dto.getKeyword(),
                "name","legalPerson","creditCode"
           //要求关键词拆分后的每一个词都必须在字段中出现
        ).operator(Operator.AND)
                        //BEST_FIELDS: 评分策略。系统会寻找匹配得分最高的那一个字段作为该文档的最终搜索分
                        .type(MultiMatchQueryBuilder.Type.BEST_FIELDS)
        );

        //如果用户输入的是完整公司名
        boolQueryBuilder.should(
                QueryBuilders.matchPhraseQuery("name", dto.getKeyword()).boost(2.0f)
        );

        //分页参数处理
        int pageNum = (dto.getPageNum() != null && dto.getPageNum() > 0) ? dto.getPageNum() : 1;
        int pageSize = (dto.getPageSize() != null && dto.getPageSize() > 0) ? dto.getPageSize() : 10;

        //Spring Data ES 的底层页码是从 0 开始的,所以是pageNum - 1
        PageRequest pageRequest = PageRequest.of(pageNum - 1, pageSize);

        NativeSearchQuery nativeSearchQuery = new NativeSearchQueryBuilder()
                .withQuery(boolQueryBuilder)
                .withPageable(pageRequest)
                .withHighlightBuilder(getHighlightBuilder("name","legalPerson","creditCode"))
                .build();

        return nativeSearchQuery;

    }

    /**
     * 根据前端传入高级搜索的dto，动态构建es查询
     * @param dto
     * @return
     */
    public static NativeSearchQuery buildAdvancedSearchQuery(EntAdvancedQueryDTO dto){

        //创建一个布尔查询主容器
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        //关键字全局搜索
        if(StringUtils.isNotBlank(dto.getKeyword())){
            boolQueryBuilder.must(QueryBuilders.multiMatchQuery(
                    dto.getKeyword(),
                    "name","legalPerson","creditCode"
            ));
        }



        //经营状态过滤
        if(dto.getStatusTags()!=null && !dto.getStatusTags().isEmpty()){
            //termsQuery: 相当于 SQL 的 IN (状态1, 状态2...)
            boolQueryBuilder.filter(QueryBuilders.termsQuery("statusTag",dto.getQueryStateCodes()));
        }

        //注册资本范围过滤
        if(dto.getCapitalRanges()!=null && !dto.getCapitalRanges().isEmpty()){
            //创建一个内部bool查询容器
            BoolQueryBuilder capitalQueryBuilder = QueryBuilders.boolQuery();
            for (EntAdvancedQueryDTO.Range capitalRange : dto.getCapitalRanges()) {
                if(capitalRange==null && (capitalRange.getMin() == null && capitalRange.getMax() == null)){
                    continue;
                }
                RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("regCapital");

                if(capitalRange.getMin()!=null){
                    //gte 查询es中注册资本大于等于capitalRange.getMin()的
                    rangeQueryBuilder.gte(capitalRange.getMin());
                }
                if(capitalRange.getMax()!=null){
                    //lte 查询es中注册资本小于等于capitalRange.getMax()
                    rangeQueryBuilder.lte(capitalRange.getMax());
                }
                //shoud:放入容器内部 相当于sql中的or
                capitalQueryBuilder.should(rangeQueryBuilder);
            }
            //将拼装好的（范围a or 范围b）作为整体，用and的形式加入到主查询中
            boolQueryBuilder.filter(capitalQueryBuilder);
        }

        //成立年份过滤
        if(dto.getYearRanges()!=null && !dto.getYearRanges().isEmpty()){
            //创建一个内部bool查询容器
            BoolQueryBuilder yearQueryBuilder = QueryBuilders.boolQuery();
            for (EntAdvancedQueryDTO.Range yearRange : dto.getYearRanges()) {
                RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("establishDate");
                if(yearRange.getMin()!=null){
                    //gte 查询es中成立年限大于等于yearRange.getMin()
                    rangeQueryBuilder.gte(yearRange.getMin());
                }
                if(yearRange.getMax()!=null){
                    //lte 查询es中成立年限小于等于yearRange.getMax()
                    rangeQueryBuilder.lte(yearRange.getMax());
                }
                //shoud:放入容器内部 相当于sql中的or
                yearQueryBuilder.should(rangeQueryBuilder);
            }
            //将拼装好的（范围a or 范围b）作为整体，用and的形式加入到主查询中
            boolQueryBuilder.filter(yearQueryBuilder);
        }

        //如果查询条件为空，抛出异常
        if(boolQueryBuilder.must().isEmpty() && boolQueryBuilder.filter().isEmpty()){

            throw new BaseException("搜索条件不能为空，请至少输入或选择一项搜索条件");
        }

        //分页参数处理
        int pageNum = (dto.getPageNum() != null && dto.getPageNum() > 0) ? dto.getPageNum() : 1;
        int pageSize = (dto.getPageSize() != null && dto.getPageSize() > 0) ? dto.getPageSize() : 10;

        //Spring Data ES 的底层页码是从 0 开始的,所以是pageNum - 1
        PageRequest pageRequest = PageRequest.of(pageNum - 1, pageSize);

        NativeSearchQuery nativeSearchQuery = new NativeSearchQueryBuilder()
                .withQuery(boolQueryBuilder)
                .withPageable(pageRequest)
                .withHighlightBuilder(getHighlightBuilder("name","legalPerson","creditCode"))
                .build();

        return nativeSearchQuery;



    }

    /**
     * 构建es高亮参数工具方法
     * @param keywords
     * @return
     */
    public static HighlightBuilder getHighlightBuilder(String... keywords){

        HighlightBuilder highlightBuilder = new HighlightBuilder();

        //高亮的颜色
        highlightBuilder.preTags("<span style='color: #7c4dff; font-weight: bold;'>");
        highlightBuilder.postTags("</span>");

        //遍历传入的可变参数，注册高亮字段
        for (String keyword : keywords) {
            highlightBuilder.field(keyword);
        }

        //设置高亮片段的字符长度
        highlightBuilder.fragmentSize(100);
        //设置高亮片段的数量
        highlightBuilder.numOfFragments(1);
        return highlightBuilder;
    }
}
