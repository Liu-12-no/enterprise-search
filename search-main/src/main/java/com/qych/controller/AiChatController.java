package com.qych.controller;

import com.qych.agent.EnterpriseChatAgent;
import com.qych.entity.dtos.MessageDeleteDto;
import com.qych.entity.dtos.SessionUpdateDTO;
import com.qych.entity.dtos.UserUpdateDTO;
import com.qych.entity.vo.EntChatSessionVO;
import com.qych.service.AiChatService;
import com.qych.service.IEntChatMessageService;
import com.qych.service.IEntChatSessionService;
import com.qych.utils.SseManager;
import com.qych.utils.pojo.BaseResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class AiChatController {

    @Autowired
    private AiChatService aiChatService;

    @Autowired
    private EnterpriseChatAgent enterpriseChatAgent;

    @Autowired
    private IEntChatSessionService entChatSessionService;

    @Autowired
    private IEntChatMessageService entChatMessageService;

    @GetMapping(value = "/stream", produces = "text/event-stream;charset=UTF-8")
    public SseEmitter chatStream(@RequestParam Long sessionId, @RequestParam String question) {


        SseEmitter sseEmitter = aiChatService.createChatStream(sessionId, question);
        return sseEmitter;
    }

    /**
     * 删除用户对应的session以及聊天内容
     * @param sessionId
     * @return
     */
    @DeleteMapping("/delete/{sessionId}")
    public BaseResponse<Void> deleteSession(@PathVariable("sessionId") Long sessionId){


        entChatSessionService.deleteSessionAndMessage(sessionId);

        return BaseResponse.success();
    }

    @PutMapping("/rename")
    public BaseResponse<EntChatSessionVO> renameSessionn(@RequestBody SessionUpdateDTO dto){

        EntChatSessionVO entChatSessionVO = entChatSessionService.updateSessionName(dto);

        return BaseResponse.success(entChatSessionVO);
    }

    /**
     * 删除聊天记录
     * @return
     */
    @PostMapping("/messages/delete")
    public BaseResponse<String> deleteMessage(@RequestBody MessageDeleteDto dto) {

        List<Long> ids = dto.getIds();
        if (ids == null || ids.isEmpty()) {
            return BaseResponse.error("未选择要删除的消息");
        }

        // 2. 执行删除
        boolean isSuccess = entChatMessageService.deleteByIds(ids);

        // 3. 返回结果（注意失败要用 error/fail）
        if (isSuccess) {
            return BaseResponse.success("删除成功");
        } else {
            return BaseResponse.error("删除失败");
        }
    }


}
