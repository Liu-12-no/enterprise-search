package com.qych.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.qych.component.CompanyCacheService;
import com.qych.entity.dtos.ChatFileUploadDTO;
import com.qych.service.KnowledgeBaseService;

import com.qych.utils.MinioUtil;
import com.qych.utils.UserContext;
import com.qych.utils.exception.BaseException;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.ahocorasick.trie.Emit;
import org.ahocorasick.trie.Trie;

import org.apache.http.HttpHost;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    //注入本地BGE模型
    @Autowired
    private EmbeddingModel embeddingModel;

    //注入向量数据库连接
    @Autowired
    private EmbeddingStore<TextSegment> embeddingStore;

    @Autowired
    private CompanyCacheService companyCacheService;

    @Value("${spring.ai.elasticsearch.url}")
    private String aiEsUrl;

    @Autowired
    private MinioUtil minioUtil;

    @Value("${spring.minio.endpoint}")
    private String endpoint;

    @Value("${spring.minio.bucketName}")
    private String bucketName;

    private ElasticsearchClient aiElasticsearchClient;

    @Value("${qych.python-parser.url}")
    private String pythonApiUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @PostConstruct
    public void initAiClient() {
        // 1. 设置通信地址
        HttpHost host = HttpHost.create(aiEsUrl);

        // 2. 创建底层网络通信底盘
        RestClient restClient = RestClient.builder(host).build();

        // 3. 绑定 ES 8 的 JSON 翻译官
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());

        // 4. 实例化最终的高级客户端
        this.aiElasticsearchClient = new ElasticsearchClient(transport);

        log.info("【AI 向量库】专属 ES 8.x 清理客户端初始化完毕，直连: {}", aiEsUrl);
    }

    /**
     * 根据不同的后缀名解析文件
     *
     * @param tempFilePath
     * @param suffix  后缀名
     * @param dto
     */
    @Override
    public void processAndImportFile(String tempFilePath, String suffix, ChatFileUploadDTO dto,String fileUrl) {

        Document document = null;
        if (".pdf".equals(suffix)){
            //解析pdf
            document = parsePdfWithPythonService(tempFilePath);
        } else if (".docx".equals(suffix) || ".doc".equals(suffix)) {
            //解析word
            document = parseWordToDocument(tempFilePath);
        } else if (".jpg".equals(suffix) || ".jpeg".equals(suffix) || ".png".equals(suffix)) {
            //解析图片
            document = parseImageToDocument(tempFilePath);
        }else {
            throw new BaseException("不支持该格式:"+suffix);
        }

        //校验一下
        if (document == null) {
            throw new RuntimeException("文件解析结果为空，请检查文件内容");
        }
        //从当前线程获得用户id
        Long userId = UserContext.getUserId();
        //把普通字符串变成path对象
        Path path = Paths.get(tempFilePath);

        //文件原名
        String originalFilename = dto.getFile().getOriginalFilename();
        //把本地文件(pdf、wprd、image)对象，翻译成大模型能读懂的结构化文档对象
        document.metadata().put("scope", "session_private"); // 标记为私有文件
        document.metadata().put("sessionId", dto.getSessionId());
        document.metadata().put("userId", userId);

        if(originalFilename!=null){
            document.metadata().put("fileName", originalFilename);
        }else{
            document.metadata().put("fileName", "未知命名");
        }


        performVectorImport(document,dto,fileUrl);

    }


    /**
     * 将本地pdf文件上传es向量数据库
     * @param document
     * @param dto
     */
    @Override
    public void performVectorImport(Document document,ChatFileUploadDTO dto,String fileUrl) {

        log.info("开始解析 私有会话 文件: {}", dto.getCompanyName());

        //将整篇pdf切割成小块（最大500字，重叠50字，防断句）
        List<TextSegment> rawSegments = DocumentSplitters.recursive(500, 50).split(document);

        List<TextSegment> taggedSegments = new ArrayList<>();

        log.info("切片完成，共 {} 块。开始匹配企业实体...", rawSegments.size());

        //获取Tire
        Trie trie = companyCacheService.getTrie();


        //遍历每一个切片
        for (TextSegment segment : rawSegments) {

            //把所有文字提取出来
            String text = segment.text();

            //复制一份（包含scope、sessionId、userId）
            Metadata segmentMeta = segment.metadata().copy();

            //假设切片中没有提到公司名字，给他打上“通用行业数据”的标签
            String mentionedCompany = "通用行业数据";
            if(trie!= null){
                //扫描文本，瞬间抓住所有的公司名，以Emit对象返回
                Collection<Emit> emits = trie.parseText(text);

                if(!emits.isEmpty()){
                    //将去重后的公司名去重后拼接起来
                    mentionedCompany = emits.stream()
                            //获取刚才抓住的公司名
                            .map(Emit::getKeyword)
                            //去重
                            .distinct()
                            .collect(Collectors.joining(","));
                }
            }
            //将这块切片标记上公司名
            segmentMeta.put("mentionedCompany",mentionedCompany);

            segmentMeta.put("fileId",fileUrl);

            //将切片text和segmentMeta（包含装填各种信息的）捏成一个新切片
            TextSegment textSegment = TextSegment.from(text, segmentMeta);
            //添加进list集合taggedSegments中
            taggedSegments.add(textSegment);
        }
        //向量化入库
        log.info(" 实体识别完毕，正在生成向量并导入 Elasticsearch 数据库...");

        //把每一段中文向量化
        List<Embedding> embeddings = embeddingModel.embedAll(taggedSegments).content();


        //将向量化后的数据还有文本导入es数据库
        embeddingStore.addAll(embeddings,taggedSegments);

        log.info("动态切片入库彻底完成！当前会话 ID: {}", dto.getSessionId());
    }

    // 在 KnowledgeBaseServiceImpl 类中添加
    private Document parseWordToDocument(String tempFilePath) {
        log.info("正在解析 Word 文档: {}", tempFilePath);
        StringBuilder stringBuilder = new StringBuilder();
        // 后续在这里接入 POI 解析 Word

        try {
            //创建文件输入流，准备读取磁盘上的文件
            try(FileInputStream fileInputStream = new FileInputStream(tempFilePath)){

                if(tempFilePath.toLowerCase().endsWith(".docx")){
                    try {
                        //解析.docx格式文件
                        XWPFDocument docx = new XWPFDocument(fileInputStream);

                        //遍历每一个段落
                        for (XWPFParagraph paragraph : docx.getParagraphs()) {
                            stringBuilder.append(paragraph.getText()).append("\n");
                        }
                    } catch (IOException e) {
                        log.error("docx文件解析异常，{}",e);
                        throw new RuntimeException(e);
                    }
                }else{
                    try {
                        // 解析 .doc
                        HWPFDocument hwpfDocument = new HWPFDocument(fileInputStream);

                        // WordExtractor 是旧版本处理 .doc 文件的便捷提取器
                        WordExtractor wordExtractor = new WordExtractor(hwpfDocument);

                        for (String text : wordExtractor.getParagraphText()) {
                            stringBuilder.append(text).append("\n");
                        }

                    } catch (IOException e) {
                        log.error("doc文件解析异常，{}",e);
                        throw new RuntimeException(e);
                    }
                }
                //拼接好的所有文档文本，转化为系统统一的 Document 对象
                return  Document.from(stringBuilder.toString());
            }


        } catch (Exception e) {

            log.error("Word 解析失败: {}", tempFilePath, e);
            throw new RuntimeException(e);
        }

    }

    private Document parseImageToDocument(String tempFilePath) {
        log.info("正在通过 OCR 解析图片: {}", tempFilePath);

        String result;
        try {

            // 后续在这里接入 Tesseract OCR
            Tesseract tesseract = new Tesseract();

            tesseract.setDatapath("E:\\tessdata\\");
            //支持中文识别
            tesseract.setLanguage("chi_sim");
             result = tesseract.doOCR(new File(tempFilePath));
            if(result==null || result.trim().isEmpty()){
                return Document.from("图片中未识别到有效文字");
            }


        } catch (TesseractException e) {
            log.error("OCR 识别异常: ", e);

            return Document.from("图片解析失败，无法提取文字");
        }
        return Document.from(result);
    }

    /**
     * 预览pdf或者图片信息
     *
     * @param fileUrl
     * @param response
     * @throws Exception
     */
    @Override
    public void previewFile(String fileUrl, HttpServletResponse response) throws Exception {
        //url截取逻辑
        String prefix = endpoint + '/' + bucketName + '/';

        String objectName = fileUrl;

        if(objectName.startsWith(prefix)){
            objectName = objectName.substring(prefix.length());
        }

        // 文件名提取与去 UUID
        String fileName = "preview.file";
        int lastSlashIndex = objectName.lastIndexOf("/");
        if(lastSlashIndex!= -1){
            fileName = objectName.substring(lastSlashIndex + 1);
        }
        if(fileName.contains("-")){
            fileName = fileName.substring(fileName.indexOf("_") + 1);
        }

        // 动态判断 Content-Type
        String contentType = "application/octet-stream";
        String lowerFileName = fileName.toLowerCase();
        if (lowerFileName.endsWith(".pdf")) {
            contentType = "application/pdf";
        } else if (lowerFileName.endsWith(".png")) {
            contentType = "image/png";
        } else if (lowerFileName.endsWith(".jpg") || lowerFileName.endsWith(".jpeg")) {
            contentType = "image/jpeg";
        }

        try (InputStream stream = minioUtil.getObjectStream(objectName)) {

            // 设置 HTTP 响应的 Content-Type
            response.setContentType(contentType);

            // 设置 HTTP 响应的 Content-Disposition 头
            // 'inline' 表示浏览器应该尝试在页面内联显示文件（预览）
            // 'filename' 参数指定了用户如果选择下载该文件时，默认保存的文件名
            // java.net.URLEncoder.encode 是为了处理文件名中的中文或特殊字符，防止乱码
            response.setHeader("Content-Disposition", "inline; filename=\"" + java.net.URLEncoder.encode(fileName, "UTF-8") + "\"");

            //将从 MinIO 获取的输入流 (stream) 复制到 HTTP 的输出流中
            StreamUtils.copy(stream, response.getOutputStream());

            // 强制将缓冲区的内容写入客户端，确保数据发送完成
            response.flushBuffer();
        }
    }

    /**
     * 根据messageId删除向量数据库信息
     *
     * @param fileUrl
     */
    @Override
    public void deleteVectorsByFileUrl(String fileUrl) {

        log.info("【清理】准备精准清理向量，fileUrl: {}", fileUrl);
        try {
            // 使用 ES 8.x 的 deleteByQuery API
            aiElasticsearchClient.deleteByQuery(d -> d
                    .index("company_knowledge_vector_index") // 确保这里的索引名与你 Bean 定义的一致
                    .query(q -> q
                            .term(t -> t
                                    .field("metadata.fileId") // 必须匹配你存入时写的 field 名
                                    .value(v -> v.stringValue(fileUrl)) // fileUrl 就是唯一的 fileId
                            )
                    )
            );
            log.info("【清理】成功清理 fileUrl 对应的向量: {}", fileUrl);
        } catch (Exception e) {
            log.error("【清理】ES 执行删除失败，fileUrl: {}", fileUrl, e);
            throw new RuntimeException("向量库文件清理失败", e);
        }
    }

    /**
     * 根据sessionId删除向量数据库信息
     *
     * @param sessionId
     */
    @Override
    public void deleteVectorsBySessionId(Long sessionId) {

        log.info("【清理】正在销毁会话所有向量数据，快递单号: {}", sessionId);
        try {
            aiElasticsearchClient.deleteByQuery(d -> d
                    .index("company_knowledge_vector_index")
                    .query(q -> q
                            .term(t -> t
                                    .field("metadata.sessionId") // 匹配会话
                                    .value(v -> v.longValue(sessionId))
                            )
                    )
            );
            log.info("【清理】会话包裹已全部销毁，sessionId: {}", sessionId);
        } catch (Exception e) {
            log.error("【清理】会话清理失败", e);
        }
    }

    /**
     *让python解析pdf
     * @param tempFilePath
     * @return
     */
    private Document parsePdfWithPythonService(String tempFilePath){

        log.info("正在将 PDF 转发给 Python 微服务进行深度解析: {}", tempFilePath);

        try {
            StringBuilder finalContent = new StringBuilder();

            // 1. 组装请求...
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new FileSystemResource(tempFilePath));

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // 2. 发起请求
            ResponseEntity<String> response = restTemplate.postForEntity(pythonApiUrl, requestEntity, String.class);

            // ==========================================
            // 【层级 1】：判断 HTTP 网络请求是否成功 (200 OK)
            // ==========================================
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {

                JsonNode rootNode = objectMapper.readTree(response.getBody());

                // ==========================================
                // 【层级 2】：判断 Python 业务是否成功 ("success")
                // ==========================================
                if ("success".equals(rootNode.has("status") ? rootNode.get("status").asText() : "")) {

                    // 拿到 JSON 里的 content 数组
                    JsonNode contentArray = rootNode.get("data").get("content");

                    // ==========================================
                    // 【层级 3】：如果有数据，开始遍历拼装
                    // ==========================================
                    if (contentArray != null && contentArray.isArray()) {
                        for (JsonNode pageNode : contentArray) {

                            // 提取当前页的普通纯文本
                            String pageText = pageNode.has("text") ? pageNode.get("text").asText() : "";
                            if (pageText != null && !pageText.trim().isEmpty()) {
                                finalContent.append(pageText).append("\n\n");
                            }

                            // 提取并格式化当前页的表格数据
                            JsonNode tablesArray = pageNode.get("tables");
                            if (tablesArray != null && tablesArray.isArray() && tablesArray.size() > 0) {
                                finalContent.append("--- 以下为表格数据 ---\n");

                                for (JsonNode tableNode : tablesArray) {
                                    for (JsonNode rowNode : tableNode) {
                                        List<String> rowCells = new ArrayList<>();
                                        for (JsonNode cellNode : rowNode) {
                                            String cellText = cellNode.asText().replace("\n", " ").trim();
                                            rowCells.add(cellText);
                                        }
                                        finalContent.append("| ").append(String.join(" | ", rowCells)).append(" |\n");
                                    }
                                    finalContent.append("\n");
                                }
                                finalContent.append("--- 表格数据结束 ---\n\n");
                            }
                        }
                    }

                } else {

                    // Python 明确告诉你：我收到文件了，但是我解析失败了！
                    String errorMsg = rootNode.has("message") ? rootNode.get("message").asText() : "未知业务错误";
                    throw new RuntimeException("Python 微服务解析业务失败: " + errorMsg);
                }

                // --- 此时，HTTP 成功且业务成功，准备返回结果 ---
                String resultText = finalContent.toString().trim();
                if (resultText.isEmpty()) {
                    log.warn("【警告】PDF 深度解析结果为空字符串！文件路径: {}", tempFilePath);
                    return Document.from("文档解析完毕，但未提取到有效文本或表格数据。");
                }

                log.info("✅ PDF 深度解析成功，完美拼装文本与表格，总字符数: {}", resultText.length());
                return Document.from(resultText);

            } else {

                // Python 服务器死机了，或者根本没启动报 404/500！
                throw new RuntimeException("调用 Python 服务网络异常，HTTP 状态码: " + response.getStatusCodeValue());
            }

        } catch (Exception e) {
            log.error("❌ 桥接 Python 解析 PDF 发生全局异常: ", e);
            throw new RuntimeException("PDF 深度解析桥接失败", e);
        }


    }


}
