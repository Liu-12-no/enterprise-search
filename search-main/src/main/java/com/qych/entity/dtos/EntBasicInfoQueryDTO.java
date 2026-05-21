package com.qych.entity.dtos;

import lombok.Data;

@Data
public class EntBasicInfoQueryDTO {

    /**
     * 查询的关键字
     */
    private String keyword;

    /**
     * 分页参数
     */
    private Integer pageNum = 1;

    /**
     * 分页参数
     */
    private Integer pageSize = 10;



}
