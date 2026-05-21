package com.qych.entity.vo;

import lombok.Data;

/**
 * 股东信息
 */
@Data
public class EntShareholderVO {
    // 股东名称
    private String shareholderName;

    // 持股比例
    private String ownershipProportion;

    // 股东类型（如：自然人、境内合伙企业）
    private String shareholderType;
}