package com.qych.entity.pojos;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 企业主导产品及业务概览
 */
@Data
@TableName("ent_business_overview") // 🌟 对应数据库表名
public class EntBusinessOverview {

    /**
     * 主键 ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 企业名称
     */
    @TableField("institution_name")
    private String institutionName;

    /**
     * 主导产品及业务概览
     */
    @TableField("business_overview")
    private String businessOverview;
}
