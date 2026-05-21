package com.qych.entity.pojos;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ent_chat_session")
//会话表
public class EntChatSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    // 所属用户ID
    private Long userId;

    // 会话标题
    private String title;

    /**
     * 逻辑删除标识：0-未删除，1-已删除
     */
    @TableLogic
    private Integer isDeleted;

    // 对话创建时间
    private LocalDateTime createTime;

    // 最后活跃时间
    private LocalDateTime updateTime;
}