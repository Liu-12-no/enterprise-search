package com.qych.entity.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EntChatMessageVO {


    private Long id;
    // 角色：user 或 assistant
    private String role;

    // 消息内容
    private String content;

    //文件的json字符串，包含文件名、路径url
    private String fileInfo;

    // 发送时间
    private LocalDateTime createTime;
}
