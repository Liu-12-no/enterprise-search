package com.qych.entity.pojos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * <p>
 * 企业基础信息表
 * </p>
 *
 * @author byl
 * @since 2026-04-17
 */
@Data
@TableName("ent_basic_info")
public class EntBasicInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 机构ID
     */
    @TableField("institution_id")
    private String institutionId;

    /**
     * 公司名称
     */
    @TableField("institution_name")
    private String institutionName;

    /**
     * 统一社会信用代码
     */
    @TableField("social_credit_code")
    private String socialCreditCode;

    /**
     * 1运营,2破产清算,3调查,4终止
     */
    @TableField("business_state")
    private Byte businessState;

    /**
     * 财务审计机构
     */
    @TableField("auditor")
    private String auditor;

    /**
     * 审计统计时间
     */
    @TableField("auditor_sgn_time")
    private LocalDate auditorSgnTime;

    /**
     * 成立日期
     */
    @TableField("establish_date")
    private LocalDate establishDate;

    /**
     * 注册资本
     */
    @TableField("registered_capital")
    private BigDecimal registeredCapital;

    /**
     * 注册资本币种
     */
    @TableField("registered_currency")
    private String registeredCurrency;

    /**
     * 员工人数
     */
    @TableField("staff_number")
    private String staffNumber;

    /**
     * 1实际,2范围
     */
    @TableField("staff_number_sgn_type")
    private Byte staffNumberSgnType;

    /**
     * 员工统计时间
     */
    @TableField("staff_number_sgn_time")
    private LocalDate staffNumberSgnTime;

    /**
     * 主营业务
     */
    @TableField("main_bussiness_industry")
    private String mainBussinessIndustry;

    /**
     * 其他业务
     */
    @TableField("other_bussiness_industry")
    private String otherBussinessIndustry;

    /**
     * 主营门类
     */
    @TableField("gb_code_2017_main_class")
    private String gbCode2017MainClass;

    /**
     * 主要产品
     */
    @TableField("main_product")
    private String mainProduct;

    /**
     * 公司登记注册类型
     */
    @TableField("enterprise_nature")
    private String enterpriseNature;

    /**
     * 所属省份
     */
    @TableField("province_name")
    private String provinceName;

    /**
     * 所属城市
     */
    @TableField("city_name")
    private String cityName;

    /**
     * 注册地址
     */
    @TableField("registered_address")
    private String registeredAddress;

    /**
     * 办公地址
     */
    @TableField("office_address")
    private String officeAddress;

    /**
     * 工商注册号
     */
    @TableField("reg_number")
    private String regNumber;

    /**
     * 网址
     */
    @TableField("website")
    private String website;

    /**
     * 电话
     */
    @TableField("tel")
    private String tel;

    /**
     * 邮箱
     */
    @TableField("email")
    private String email;

    /**
     * 经营范围
     */
    @TableField("business_scope")
    private String businessScope;

    /**
     * ISIN编码
     */
    @TableField("isin_code")
    private String isinCode;

    /**
     * 统计截止日期
     */
    @TableField("end_date")
    private LocalDate endDate;

    @TableField("update_time")
    private LocalDateTime updateTime;


}
