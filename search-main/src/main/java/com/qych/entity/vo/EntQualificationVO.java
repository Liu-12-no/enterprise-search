package com.qych.entity.vo;

import lombok.Data;

/**
 * 资质荣誉
 */
@Data
public class EntQualificationVO {
    private String category;   // 类别
    private String year;       // 认定年度
    private String batch;      // 入选批次
}