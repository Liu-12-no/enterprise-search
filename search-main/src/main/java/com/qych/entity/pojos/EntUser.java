package com.qych.entity.pojos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ent_user")
//用户表
public class EntUser {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    // 登录账号/手机号
    private String username;
    
    // 加密后的密码
    private String password;
    
    // 真实姓名/昵称
    private String trueName;
    
    // 账号状态：0-禁用，1-启用
    private Integer status;
    
    // 创建时间
    private LocalDateTime createTime;
    
    // 更新时间
    private LocalDateTime updateTime;
}