package com.qych.entity.vo;

import io.swagger.annotations.ApiModel;
import lombok.Data;

@ApiModel("登录返回信息")
@Data
public class LoginVO {
    
    // 身份令牌
    private String token;
    
    // 用户真实姓名/昵称
    private String trueName;
    
}