package com.qych.controller;

import com.qych.entity.dtos.ChatFileUploadDTO;
import com.qych.service.IEntChatMessageService;
import com.qych.service.KnowledgeBaseService;
import com.qych.utils.pojo.BaseResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeBaseController {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private IEntChatMessageService entChatMessageService;

    @PostMapping("/upload")
    public BaseResponse<String> uploadPdf(ChatFileUploadDTO dto){

        log.info("收到入库请求，会话：{}",dto.getSessionId());

        if(dto.getFile()==null || dto.getFile().isEmpty()){
            return BaseResponse.error("上传文件不能为空");
        }

        //获取原文件名
        String originalFilename = dto.getFile().getOriginalFilename();
        //获取后缀的小写
        String suffix = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase();

        //落地临时文件
        String tempPath = System.getProperty("java.io.tmpdir") + File.separator + UUID.randomUUID() + suffix;
        File tempFile = new File(tempPath);

        try {

            //将文件上传至minio，且将上传后地址保存至数据库,上传到的minio的地址返回
            String fileUrl = entChatMessageService.saveUserMessageWithFile(Long.parseLong(dto.getSessionId()), "", dto.getFile());

            //把dto里面的文件落地到临时盘
            dto.getFile().transferTo(tempFile);

            knowledgeBaseService.processAndImportFile(tempPath,suffix,dto,fileUrl);

            return BaseResponse.success("知识库pdf文档同步成功！");

        } catch (Exception e) {
            log.error("向量入库异常：", e);
            return BaseResponse.error("入库失败：" + e.getMessage());
        }finally {
            // 清理临时文件
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }



    }

    //
    @GetMapping("/preview")
    public void previewFile(@RequestParam String fileUrl, HttpServletResponse response) {

        try {

            knowledgeBaseService.previewFile(fileUrl, response);
        } catch (Exception e) {
            log.error("代理预览文件失败，URL: {}", fileUrl, e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

}
