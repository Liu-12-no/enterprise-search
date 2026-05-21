package com.qych.entity.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class EntDetailVO extends EntBasicInfoVO{


    private String regNumber;           // 工商注册号
    private String orgCode;             // 组织机构代码
    private String industry;            // 行业
    private String businessType;        // 企业类型
    private String staffNumber;         // 人员规模


    private String website;             // 官网
/*    private String email;               // 邮箱
    private String tel;                 // 电话
    private String address;              // 注册地址*/


    private String businessScope;       // 经营范围

    private String remark;   //大模型提示

    /**
     * 主导产品及业务概览
     */
    private String businessOverview;


    /**
     * 股东信息列表
     */
    private List<EntShareholderVO> shareholderList;

    /**
     * 高管信息列表
     */
    private List<EntExecutiveVO> executiveList;

    /**
     * 专利信息列表
     */
    private List<EntPatentVO> patentList;

    /**
     * 资质荣誉列表
     */
    private List<EntQualificationVO> qualificationList;
}

