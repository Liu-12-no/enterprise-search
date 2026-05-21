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

/**
 * <p>
 * 股东信息表
 * </p>
 *
 * @author byl
 * @since 2026-04-17
 */
@Data
@TableName("ent_shareholder")
public class EntShareholder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属公司机构ID
     */
    @TableField("institution_id")
    private String institutionId;

    /**
     * 股东名称
     */
    @TableField("shareholder_name")
    private String shareholderName;

    /**
     * 1-10对应不同类型
     */
    @TableField("shareholder_type")
    private Byte shareholderType;

    /**
     * 所有权比例
     */
    @TableField("ownership_proportion")
    private BigDecimal ownershipProportion;

    /**
     * 股东公司机构ID(若为公司则用于二次穿透)
     */
    @TableField("shareholder_id")
    private String shareholderId;

    /**
     * 工商注册号
     */
    @TableField("reg_number")
    private String regNumber;

    /**
     * 统一社会信用代码
     */
    @TableField("social_credit_code")
    private String socialCreditCode;

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
}
