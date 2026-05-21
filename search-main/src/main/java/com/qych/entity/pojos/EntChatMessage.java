package com.qych.entity.pojos;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import java.time.LocalDateTime;

@Data
//开启，自动解析json
@TableName(value = "ent_chat_message", autoResultMap = true)
//聊天明细表
public class EntChatMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 关联的会话ID
    private Long sessionId;

    // 角色标识：USER, ASSISTANT, SYSTEM
    private String role;

    // 消息具体的文本内容
    private String content;

    /**
     * 逻辑删除标识：0-未删除，1-已删除
     */
    @TableLogic
    private Integer isDeleted;



    private String fileInfo;

    // 消息发送时间
    private LocalDateTime createTime;
}