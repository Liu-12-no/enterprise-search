package com.qych.entity.es;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
// ES 中的索引名
@Document(indexName = "ent_basic_info")
public class EntBasicInfoDoc {
    @Id
    private Long id;

    @Field(type = FieldType.Keyword)
    private String institutionId;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    // 企业名称：需要模糊搜索
    private String name;

    @Field(type = FieldType.Keyword)
    // 状态：精确匹配
    private String statusTag;

    @Field(type = FieldType.Keyword)
    // 特性：精确匹配
    private String typeTag;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    // 法人：模糊搜索
    private String legalPerson;

    @Field(type = FieldType.Long)
    // 注册资本：数值型，方便范围过滤
    private Long regCapital;

    @Field(type = FieldType.Keyword)
    // 成立日期
    private String establishDate;

    @Field(type = FieldType.Keyword)
    // 信用代码
    private String creditCode;

    @Field(type = FieldType.Keyword)
    private String tel;

    @Field(type = FieldType.Keyword)
    private String email;

    @Field(type = FieldType.Text, analyzer = "ik_max_word")
    // 地址：模糊搜索
    private String address;
}