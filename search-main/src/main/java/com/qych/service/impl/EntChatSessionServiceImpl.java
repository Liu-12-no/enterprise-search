package com.qych.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qych.entity.dtos.SessionUpdateDTO;
import com.qych.entity.pojos.EntChatMessage;
import com.qych.entity.pojos.EntChatSession;
import com.qych.entity.vo.EntChatSessionVO;
import com.qych.mapper.EntChatMessageMapper;
import com.qych.mapper.EntChatSessionMapper;
import com.qych.service.IEntChatSessionService;
import com.qych.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class EntChatSessionServiceImpl extends ServiceImpl<EntChatSessionMapper, EntChatSession> implements IEntChatSessionService {

    @Autowired
    private EntChatMessageMapper entChatMessageMapper;
    @Override
    public List<EntChatSessionVO> getHistoryList() {

        Long userId = UserContext.getUserId();

        //构建查询条件
        LambdaQueryWrapper<EntChatSession> wrapper = new LambdaQueryWrapper<EntChatSession>().eq(EntChatSession::getUserId, userId)
                .orderByDesc(EntChatSession::getCreateTime);

        //查询当前用户的
        List<EntChatSession> entChatSessionList = list(wrapper);

        List<EntChatSessionVO> entChatSessionVOList = new ArrayList<>();

        for (EntChatSession session : entChatSessionList) {

            EntChatSessionVO entChatSessionVO = new EntChatSessionVO();
            BeanUtils.copyProperties(session,entChatSessionVO);

            entChatSessionVO.setId(String.valueOf(session.getId()));

            entChatSessionVOList.add(entChatSessionVO);

        }

        return entChatSessionVOList;
    }

    /**
     * 删除会话及其关联的所有聊天记录
     * @param sessionId
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSessionAndMessage(Long sessionId) {

        //构建查询条件
        LambdaQueryWrapper<EntChatMessage> wrapper = new LambdaQueryWrapper<EntChatMessage>().eq(EntChatMessage::getSessionId, sessionId);
        //删除该sessionID对应聊天记录
        entChatMessageMapper.delete(wrapper);

        //删除会话本身
        boolean isRemove = removeById(sessionId);

        if(!isRemove){
            log.warn("会话 {} 删除失败，可能已被删除", sessionId);
        }


    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EntChatSessionVO updateSessionName(SessionUpdateDTO dto) {

        if (dto.getId() == null) {
            log.error("更新会话标题失败：sessionId 为空");
            return null; // 或者抛出自定义异常
        }

        EntChatSession session = new EntChatSession();

        //将前端传过来的新title复制给session
        BeanUtils.copyProperties(dto,session);

        boolean isUpdate = updateById(session);

        if(!isUpdate){
            log.error("更新会话标题未成功，{}",dto.getId());
        }
        EntChatSessionVO entChatSessionVO = new EntChatSessionVO();
        entChatSessionVO.setId(String.valueOf(dto.getId()));
        entChatSessionVO.setTitle(dto.getTitle());


        return entChatSessionVO;
    }
}