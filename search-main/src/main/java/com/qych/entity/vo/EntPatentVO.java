package com.qych.entity.vo;

import lombok.Data;

/**
 *专利信息
 */
@Data
public class EntPatentVO {
    // 专利名称
    private String patentName;
    // 专利类型
    private String type;
    // 申请日期
    private String applicationDate;
    // 公开公告号
    private String openNumber;
    // 发明人
    private String inventor;
}