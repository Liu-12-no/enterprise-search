package com.qych.entity.dtos;


import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class MessageDeleteDto {

    @NotEmpty(message = "请选择要删除的消息")
    private List<Long> ids;

}
