package com.qych.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qych.entity.pojos.EntChatMessage;
import com.qych.entity.vo.EntChatMessageVO;
import com.qych.mapper.EntChatMessageMapper;
import com.qych.service.IEntChatMessageService;
import com.qych.utils.MinioUtil;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@Slf4j
@Service
public class EntChatMessageServiceImpl extends ServiceImpl<EntChatMessageMapper, EntChatMessage> implements IEntChatMessageService {

    private final MinioUtil minioUtil;

    public EntChatMessageServiceImpl(MinioUtil minioUtil) {
        this.minioUtil = minioUtil;
    }

    /**
     * 查询sessionId对应的聊天内容
     * @param sessionId
     * @return
     */
    @Override
    public List<EntChatMessageVO> getHistoryDetail(Long sessionId) {

        //构建查询条件
        LambdaQueryWrapper<EntChatMessage> wrapper = new LambdaQueryWrapper<EntChatMessage>().eq(EntChatMessage::getSessionId, sessionId)
                .orderByAsc(EntChatMessage::getCreateTime);

        List<EntChatMessage> entChatMessageList = list(wrapper);

        List<EntChatMessageVO> entChatMessageVOList = new ArrayList<>();

        //转换pojo -> vo
        for (EntChatMessage entChatMessage : entChatMessageList) {
            EntChatMessageVO entChatMessageVO = new EntChatMessageVO();

            BeanUtils.copyProperties(entChatMessage,entChatMessageVO);

            entChatMessageVOList.add(entChatMessageVO);

        }

        return entChatMessageVOList;
    }

    /**
     *根据id删除聊天记录
     * @param ids
     */
    @Override
    public boolean deleteByIds(List<Long> ids) {

        boolean isRemove = removeByIds(ids);
        if(isRemove){
            log.info("成功删除了 {} 条消息，对应的 IDs：{}", ids.size(), ids);
        }

        return isRemove;

    }

    /**
     * 将pdf上传minio，且将上传地址保存到数据库
     * @param sessionId
     * @param content
     * @param file
     */
    @Override
    public String saveUserMessageWithFile(Long sessionId, String content, MultipartFile file) {

        EntChatMessage entChatMessage = new EntChatMessage();
        entChatMessage.setSessionId(sessionId);
        entChatMessage.setRole("user");
        entChatMessage.setContent(content);

        //上传到minio返回的地址
        String fileUrl=null;

        Map<String,String> info =new HashMap<>();
        try {
            if(file!=null && !file.isEmpty()){
                fileUrl = minioUtil.uploadFile(file);

                info.put("name",file.getOriginalFilename());
                info.put("url", fileUrl);
                info.put("size", String.format("%.2f MB", file.getSize() / (1024.0 * 1024.0)));

                //转成list格式
                List<Map<String, String>> fileList = Collections.singletonList(info);
                //转json格式存入
                String jsonString = JSON.toJSONString(fileList);
                entChatMessage.setFileInfo(jsonString);

            }
            save(entChatMessage);

        } catch (Exception e) {
            log.error("上传minio或保存消息失败：{}",e);
            throw new RuntimeException("文件上传或保存失败", e);
        }
        return fileUrl;
    }
}