package com.qych.entity.pojos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * <p>
 * 企业资质认定表
 * </p>
 *
 * @author byl
 * @since 2026-04-17
 */
@Data
@TableName("ent_qualification")
public class EntQualification implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 公司名称
     */
    @TableField("company_name")
    private String companyName;

    /**
     * 认定年度
     */
    @TableField("award_year")
    private Integer awardYear;

    /**
     * 所属省份
     */
    @TableField("province")
    private String province;

    /**
     * 所属城市
     */
    @TableField("city")
    private String city;

    /**
     * 入选批次
     */
    @TableField("batch")
    private String batch;

    /**
     * 类别(如专精特新)
     */
    @TableField("category")
    private String category;

    /**
     * 是否上市公司:0否,1是
     */
    @TableField("is_listed")
    private Byte isListed;

    /**
     * 上市公司代码
     */
    @TableField("listed_code")
    private String listedCode;

    /**
     * 与关联上市公司关系
     */
    @TableField("relation_with_listed")
    private String relationWithListed;

    /**
     * 关联上市公司代码
     */
    @TableField("related_listed_code")
    private String relatedListedCode;
}
