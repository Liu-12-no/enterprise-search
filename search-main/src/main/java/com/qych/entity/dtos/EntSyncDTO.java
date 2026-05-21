package com.qych.entity.dtos;

import com.qych.entity.pojos.EntBasicInfo;
import lombok.Data;

/**
 * 用于 MySQL 同步到 ES 的数据传输对象
 */
@Data
public class EntSyncDTO extends EntBasicInfo {
    
    /**
     * 法定代表人姓名
     */
    private String legalPersonName; 
}