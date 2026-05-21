package com.qych.entity.dtos;

import lombok.Data;

@Data
public class LoginDTO {
    // 账号
    private String username;
    // 密码
    private String password;
}