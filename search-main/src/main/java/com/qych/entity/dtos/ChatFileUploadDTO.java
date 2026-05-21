package com.qych.entity.dtos;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class ChatFileUploadDTO {
    /**
     * 上传的 PDF 文件
     */
    private MultipartFile file;

    /**
     * 当前会话 ID
     */
    private String sessionId;


    /**
     * 关联的公司名称
     */
    private String companyName;
}
