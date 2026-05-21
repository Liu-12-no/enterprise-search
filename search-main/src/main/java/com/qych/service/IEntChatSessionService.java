package com.qych.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qych.entity.dtos.SessionUpdateDTO;
import com.qych.entity.pojos.EntChatSession;
import com.qych.entity.vo.EntChatSessionVO;

import java.util.List;

public interface IEntChatSessionService extends IService<EntChatSession> {

    public List<EntChatSessionVO> getHistoryList();

    public void deleteSessionAndMessage(Long sessionId);

    public EntChatSessionVO updateSessionName(SessionUpdateDTO dto);
}