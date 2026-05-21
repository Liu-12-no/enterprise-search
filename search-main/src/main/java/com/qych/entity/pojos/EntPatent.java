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
 * 企业专利表
 * </p>
 *
 * @author byl
 * @since 2026-04-17
 */
@Data
@TableName("ent_patent")
public class EntPatent implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 机构ID
     */
    @TableField("institution_id")
    private String institutionId;

    /**
     * 专利申请号
     */
    @TableField("application_number")
    private String applicationNumber;

    /**
     * 专利名称
     */
    @TableField("patent_name")
    private String patentName;

    /**
     * I发明,U实用,D外观
     */
    @TableField("type")
    private String type;

    /**
     * 申请人
     */
    @TableField("applicant")
    private String applicant;

    /**
     * 申请日期
     */
    @TableField("application_date")
    private LocalDate applicationDate;

    /**
     * 公开号
     */
    @TableField("open_number")
    private String openNumber;

    /**
     * 公开日
     */
    @TableField("open_date")
    private LocalDate openDate;

    /**
     * 分类号
     */
    @TableField("ipc_classification")
    private String ipcClassification;

    /**
     * 发明人
     */
    @TableField("inventor")
    private String inventor;

    /**
     * 代理人
     */
    @TableField("agent")
    private String agent;

    /**
     * 代理机构
     */
    @TableField("agency")
    private String agency;

    /**
     * 摘要
     */
    @TableField("summary")
    private String summary;

    /**
     * 截止日期
     */
    @TableField("end_date")
    private LocalDate endDate;
}
