package com.qych.controller;

import com.qych.entity.pojos.EntChatMessage;
import com.qych.entity.pojos.EntChatSession;
import com.qych.entity.vo.EntChatMessageVO;
import com.qych.entity.vo.EntChatSessionVO;
import com.qych.service.IEntChatMessageService;
import com.qych.service.IEntChatSessionService;
import com.qych.utils.pojo.BaseResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chat/history")
@Api(tags = "AI 搜索历史记录接口")
public class ChatHistoryController {

    @Autowired
    private IEntChatSessionService chatSessionService;

    @Autowired
    private IEntChatMessageService chatMessageService;

    /**
     * 获取左侧历史记录会话表
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("获取当前用户的历史会话列表")
    public BaseResponse<List<EntChatSessionVO>> getHistoryList(){

        //根据当前用户id查询历史记录
        List<EntChatSessionVO> entChatSessionHistoryList = chatSessionService.getHistoryList();

        return BaseResponse.success(entChatSessionHistoryList);
    }

    @GetMapping("/detail/{sessionId}")
    @ApiOperation("获取指定会话的完整聊天内容")
    public BaseResponse<List<EntChatMessageVO>> getHistoryDetail(@PathVariable("sessionId") Long sessionId){

        //获取指定会话的完整聊天内容
        List<EntChatMessageVO> historyDetail = chatMessageService.getHistoryDetail(sessionId);

        return BaseResponse.success(historyDetail);
    }

}
