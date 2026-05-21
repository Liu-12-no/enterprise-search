package com.qych.job;

import com.alibaba.fastjson.JSON;
import com.qych.entity.pojos.EntChatMessage;
import com.qych.mapper.EntChatMessageMapper;
import com.qych.mapper.EntChatSessionMapper;
import com.qych.service.KnowledgeBaseService;
import com.qych.utils.MinioUtil;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ChatDataCleanupJob {

    @Autowired
    private EntChatMessageMapper entChatMessageMapper;

    @Autowired
    private EntChatSessionMapper entChatSessionMapper;

    @Autowired
    private MinioUtil minioUtil;

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @XxlJob("cleanDeletedChatDataJob")
    public void cleanDeletedChatDataJob(){
        log.info("【垃圾回收】启动...开始全链路清理。");
        //ent_chat_session表中标记为删除的id
        List<Long> sessionIds = entChatSessionMapper.selectDeletedSessionIds();
        if(sessionIds!=null && !sessionIds.isEmpty()){
            for (Long sessionId : sessionIds) {
                //删除ES中向量
                knowledgeBaseService.deleteVectorsBySessionId(sessionId);

                //当前会话id对应的历史记录
                List<EntChatMessage> entChatMessages = entChatMessageMapper.selectBySessionId(sessionId);
                for (EntChatMessage entChatMessage : entChatMessages) {
                    //删除minio中对应的上传文件
                    deleteMinioFiles(entChatMessage.getFileInfo());
                }
                //数据库ent_chat_message物理删除
                entChatMessageMapper.physicalDeleteBySessionId(sessionId);
                //数据库ent_chat_session物理删除
                entChatSessionMapper.physicalDeleteById(sessionId);
            }
        }

        //查找已标记为删除的信息
        List<EntChatMessage> deleteChatMessages = entChatMessageMapper.selectDeletedMessages();
        if(deleteChatMessages!=null && !deleteChatMessages.isEmpty()){

            for (EntChatMessage msg : deleteChatMessages) {
                if(msg.getFileInfo()!=null){
                    //删除minion中文件和es中向量
                    deleteFilesAndVectorByJson(msg.getFileInfo());
                }
                //根据id物理删除
                entChatMessageMapper.physicalDeleteById(msg.getId());

            }

        }

    }

    public void deleteFilesAndVectorByJson(String fileInfoJson){
        if (fileInfoJson != null && !fileInfoJson.isEmpty() && !fileInfoJson.equals("[]")) {

            try {
                List<Map> fileList = JSON.parseArray(fileInfoJson, Map.class);
                for (Map fileObj : fileList) {
                    String fileUrl = (String) fileObj.get("url");
                    if (fileUrl != null && !fileUrl.trim().isEmpty()) {
                        //es中删除向量
                        knowledgeBaseService.deleteVectorsByFileUrl(fileUrl);
                        //minio中根据文件名删除文件
                        minioUtil.removeObject(extractObjectNameFromUrl(fileUrl));

                    }
                }
            } catch (Exception e) {
                log.error("同时清理异常: {}", fileInfoJson, e);
            }
        }
    }

    private void deleteMinioFiles(String fileInfoJson) {
        if(fileInfoJson!=null && !fileInfoJson.isEmpty() && !fileInfoJson.equals("[]")){

            try {
                // 将 JSON 字符串解析为 List<Map>
                List<Map> fileList = JSON.parseArray(fileInfoJson, Map.class);

                // 🌟 核心防线：防止 parseArray 真的返回了 null，导致下面的增强 for 循环报空指针
                if (fileList != null) {
                    for (Map fileObj : fileList) {
                        // 获得存储到 minio 的路径
                        String url = (String)fileObj.get("url");
                        if (url != null && !url.trim().isEmpty()){
                            String fileName = extractObjectNameFromUrl(url);
                            // minio中根据文件名删除文件
                            minioUtil.removeObject(fileName);
                            log.info("MinIO清理成功粉碎残留文件: {}", fileName);
                        }
                    }
                }
            } catch (Exception e) {

                log.error("解析文件JSON或清理MinIO异常，跳过此文件。内容: {}", fileInfoJson, e);
            }

        }
    }

    private String extractObjectNameFromUrl(String url) {
        if (url == null) {
            return "";
        }

        
        String bucketName = "knowing-ai/";

        // 找到桶名在整个 URL 字符串里的起始位置
        int index = url.indexOf(bucketName);

        if (index != -1) {
            // 从桶名之后的位置开始截取，拿到最后面的真正路径（门牌号）
            return url.substring(index + bucketName.length());
        }

        // 如果这个 URL 里根本没有桶名，为了防止报错，原样返回
        return url;
    }
}
