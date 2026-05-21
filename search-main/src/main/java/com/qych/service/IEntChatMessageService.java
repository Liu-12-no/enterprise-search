package com.qych.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.qych.entity.pojos.EntChatMessage;
import com.qych.entity.vo.EntChatMessageVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IEntChatMessageService extends IService<EntChatMessage> {

    public List<EntChatMessageVO> getHistoryDetail(Long sessionId);

    public boolean deleteByIds(List<Long> ids);

    /**
     * 将pdf上传minio，且将上传地址保存到数据库
     * @param sessionId
     * @param content
     * @param file
     */
    public String saveUserMessageWithFile(Long sessionId, String content, MultipartFile file);



}