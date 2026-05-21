package com.qych.service;

import com.qych.entity.dtos.ChatFileUploadDTO;
import dev.langchain4j.data.document.Document;

import javax.servlet.http.HttpServletResponse;

public interface KnowledgeBaseService {

    /**
     * 将本地的 文件 研报切片，结合动态词库进行实体抽取，并存入 ES 向量库
     * @param document
     * @param dto
     * @param fileUrl
     */
    public void performVectorImport(Document document, ChatFileUploadDTO dto,String fileUrl);

    /**
     * 根据不同的后缀名解析文件
     * @param tempFilePath
     * @param suffix
     * @param dto
     */
    public void processAndImportFile(String tempFilePath, String suffix, ChatFileUploadDTO dto,String fileUrl);

    /**
     * 根据sessionId删除向量数据库信息
     * @param sessionId
     */
    public void deleteVectorsBySessionId(Long sessionId);

    /**
     * 根据fileUrl删除向量数据库信息
     * @param fileUrl
     */
    public void deleteVectorsByFileUrl(String fileUrl);


    /**
     * 预览pdf或者图片信息
     * @param fileUrl
     * @param response
     * @throws Exception
     */
    public void previewFile(String fileUrl, HttpServletResponse response) throws Exception;
}
