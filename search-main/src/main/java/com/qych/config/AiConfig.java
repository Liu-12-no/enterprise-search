package com.qych.config;


import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.qych.agent.CompanySummaryAgent;
import com.qych.agent.EnterpriseChatAgent;
import com.qych.agent.TitleGeneratorAgent;
import com.qych.tool.EnterpriseSearchTool;
import com.qych.tool.KnowledgeBaseTool;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.BgeSmallZhQuantizedEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Slf4j
@Configuration
public class AiConfig {

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.base-url}")
    private String baseUrl;

    @Value("${spring.ai.elasticsearch.url}")
    private String aiEsUrl;



    /**
     * 生成标题的非流式大模型
     * @return
     */
    @Bean
    public ChatLanguageModel titleChatModel() {
        return OpenAiChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .timeout(Duration.ofSeconds(60))
                // 关闭日志，避免和聊天流式日志混淆
                .logRequests(false)
                .logResponses(false)
                .build();
    }




    /**
     * 流式大模型
     * @return
     */

    @Bean
    public StreamingChatLanguageModel streamChatLanguageModel(){
        return OpenAiStreamingChatModel.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .modelName("deepseek-chat")
                .timeout(Duration.ofSeconds(60))
                .logRequests(true)
                .logResponses(true)
                .build();
    }

    /**
     * 注册企业智能分析助手
     */
    @Bean
    public EnterpriseChatAgent enterpriseChatAgent(StreamingChatLanguageModel chatModel, EnterpriseSearchTool searchTool, KnowledgeBaseTool knowledgeBaseTool) {
        return AiServices.builder(EnterpriseChatAgent.class)
                .streamingChatLanguageModel(chatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.withMaxMessages(30))
                .tools(searchTool,knowledgeBaseTool)
                .build();
    }

    /**
     * 注册企业标题智能助手
     * @param titleChatModel
     * @return
     */
    @Bean
    public TitleGeneratorAgent titleGeneratorAgent(ChatLanguageModel titleChatModel){
        return AiServices.builder(TitleGeneratorAgent.class)
                .chatLanguageModel(titleChatModel) // 注入专属的标题大模型
                .build();
    }

    /**
     * 注册企业速览诊断助手
     * @param streamChatLanguageModel
     * @return
     */
    @Bean
    public CompanySummaryAgent companySummaryAgent(StreamingChatLanguageModel streamChatLanguageModel){
        return AiServices.builder(CompanySummaryAgent.class)
                //注入流式大模型
                .streamingChatLanguageModel(streamChatLanguageModel)
                .build();
    }

    /**
     *注册本地向量大模型
     * 将人类语言转化为向量
     * @return
     */

    @Bean
    public EmbeddingModel embeddingModel() {

        return new BgeSmallZhQuantizedEmbeddingModel();
    }

    /**
     * ai专属记忆库
     * @return
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {

        log.info("🚀 [AI配置] 正在初始化 AI 专属 ES 8.x 向量库，连接地址: {}", aiEsUrl);



        return ElasticsearchEmbeddingStore.builder()

                .serverUrl(aiEsUrl)
                .indexName("company_knowledge_vector_index")
                // BgeSmallZhQuantizedEmbeddingModel 的维度是 512
                .dimension(512)
                .build();
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(RestClient restClient) {
        // 创建 RestClientTransport
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        // 返回实例，现在 Spring 容器里就有它了
        return new ElasticsearchClient(transport);
    }


}


