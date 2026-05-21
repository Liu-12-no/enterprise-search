package com.qych.tool;


import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.ToolMemoryId;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.stream.Collectors;

@Component
@Slf4j
public class KnowledgeBaseTool {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Tool("企业深度信息检索与私有文档分析复合工具。具备两大功能：\n" +
            "1. 企业搜索：用于查询具体财务指标（利润、营收）、业务动态、战略等。当用户询问具体业务数据时，必须调用。\n" +
            "2. 私有文档处理：当用户提到【这篇文档】、【上传的文件】、【详细总结】等字眼时，【绝对不能说你无法查看】，必须调用此工具提取文档内容。\n" +
            "【系统强制指令】：用户提问时通常不会提及‘文档’等字眼，只要你需要具体数据来回答，或用户要求总结，请毫不犹豫地调用此工具，调用前绝对禁止回复废话！\n"+
            "【强制指令】：如果用户的提问中包含了具体的文件名（如【面试.docx】），你必须将该文件名精准提取并传入 fileName 参数中！")
    public String searchPrivateDocument(
            @P("提取出的自然语言搜索关键词，例如：海尔最近在白电领域的利润是多少") String query,
            @P("用户提问中包含的具体文件名（例如：'面试.docx'）。如果用户未指定具体文件名，请传空字符串 \"\"") String fileName,
            @ToolMemoryId Long sessionId
    ){
        log.info("命中私有知识库检索工具，关键词: [{}], 当前 SessionID: [{}]", query, sessionId);
        //过滤掉只属于当前会话的私有数据
        Filter sessionFilter = MetadataFilterBuilder.metadataKey("sessionId").isEqualTo(String.valueOf(sessionId))
                .and(MetadataFilterBuilder.metadataKey("scope").isEqualTo("session_private"));

        //如果大模型传来了文件名，强行追加 Metadata 过滤！
        if (StringUtils.hasText(fileName)) {
            sessionFilter = sessionFilter.and(MetadataFilterBuilder.metadataKey("fileName").isEqualTo(fileName.trim()));
            log.info("已开启精准文件隔离，只检索文件: {}", fileName);
        }

        //将关键词翻译成向量
        Embedding queryEmbedding = embeddingModel.embed(query).content();


        //组装查询请求
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                //查询条件：关键词转换为向量格式
                .queryEmbedding(queryEmbedding)
                //过滤条件
                .filter(sessionFilter)
                //只查找相关度最高的前三条
                .maxResults(3)
                //相似度必须大于0.6
                .minScore(0.6)
                .build();

        //向量数据库查到的返回结果
        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);

        if (searchResult.matches().isEmpty()) {
            return "状态：未命中相关业务细节数据。";
        }

        String searchResulText = searchResult.matches().stream()
                .map(match -> match.embedded().text())
                .collect(Collectors.joining("\n---\n"));


        return searchResulText;
    }
}
