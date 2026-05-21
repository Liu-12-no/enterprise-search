package com.qych.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qych.entity.pojos.EntChatSession;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface EntChatSessionMapper extends BaseMapper<EntChatSession> {

    public List<Long> selectDeletedSessionIds();

    public void physicalDeleteById(Long id);
}