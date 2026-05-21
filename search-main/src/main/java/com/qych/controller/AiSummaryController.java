package com.qych.controller;

import com.qych.service.AiChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api/ai/summary")
public class AiSummaryController {

    @Autowired
    private AiChatService aiChatService;

    @GetMapping(value = "/company", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getCompanySummary(@RequestParam("id") Long id, HttpServletResponse response){

        SseEmitter companySummaryStream = aiChatService.createCompanySummaryStream(id);

        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Connection", "keep-alive");
        return companySummaryStream;
    }

}
