package com.qych.entity.pojos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * <p>
 * 高管人员表
 * </p>
 *
 * @author byl
 * @since 2026-04-17
 */
@Data
@TableName("ent_executive")
public class EntExecutive implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 机构ID
     */
    @TableField("institution_id")
    private String institutionId;

    /**
     * 高管姓名
     */
    @TableField("executive_name")
    private String executiveName;

    /**
     * 职务名称
     */
    @TableField("position")
    private String position;

    /**
     * 职务编码
     */
    @TableField("position_id")
    private String positionId;

    /**
     * 公司名称
     */
    @TableField("institution_name")
    private String institutionName;

    /**
     * 统计截止日期
     */
    @TableField("end_date")
    private LocalDate endDate;
}
