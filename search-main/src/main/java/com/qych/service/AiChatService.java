package com.qych.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiChatService {

    /**
     * 创建并处理AI对话流，每次用户发送提问时的总入口
     * @param sessionId
     * @param question
     * @return
     */
    public SseEmitter createChatStream(Long sessionId, String question);


    /**
     * 生成企业详情的专属 AI 智能速览
     * @param companyId
     * @return
     */
    public SseEmitter createCompanySummaryStream(Long companyId);



}
