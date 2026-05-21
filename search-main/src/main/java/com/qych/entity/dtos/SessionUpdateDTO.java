package com.qych.entity.dtos;

import lombok.Data;

@Data
public class SessionUpdateDTO {

    //sessionId
    private Long id;
    //前端传过来的新会话名称
    private String title;
}
