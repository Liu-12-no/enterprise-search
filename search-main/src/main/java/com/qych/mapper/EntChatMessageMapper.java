package com.qych.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.qych.entity.pojos.EntChatMessage;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface EntChatMessageMapper extends BaseMapper<EntChatMessage> {

    /**
     * 查询标记为删除的消息记录
     * @return
     */
   public List<EntChatMessage> selectDeletedMessages();

    /**
     * 捞取该会话下所有消息
     * @param sessionId
     * @return
     */
    public List<EntChatMessage> selectBySessionId(Long sessionId);

    /**
     * 根据sessionId物理删除数据库
     * @param sessionId
     */
    public void physicalDeleteBySessionId(Long sessionId);

    /**
     *根据id物理删除数据库
     * @param id
     */
    public void physicalDeleteById(Long id);
}