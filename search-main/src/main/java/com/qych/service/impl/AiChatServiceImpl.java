package com.qych.service.impl;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.qych.agent.CompanySummaryAgent;
import com.qych.agent.EnterpriseChatAgent;
import com.qych.agent.TitleGeneratorAgent;
import com.qych.entity.pojos.EntChatMessage;
import com.qych.entity.pojos.EntChatSession;
import com.qych.entity.vo.EntDetailVO;
import com.qych.mapper.EntChatMessageMapper;
import com.qych.mapper.EntChatSessionMapper;
import com.qych.service.AiChatService;
import com.qych.service.IEntBasicInfoService;
import com.qych.utils.SseManager;
import com.qych.utils.UserContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


@Service
@Slf4j
public class AiChatServiceImpl implements AiChatService {
    @Autowired
    private EnterpriseChatAgent chatAgent;

    @Autowired
    private TitleGeneratorAgent titleGeneratorAgent;

    @Autowired
    private EntChatSessionMapper entChatSessionMapper;

    @Autowired
    private EntChatMessageMapper entChatMessageMapper;

    @Autowired
    private IEntBasicInfoService entBasicInfoService;

    @Autowired
    private CompanySummaryAgent companySummaryAgent;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String CACHE_PREFIX = "ai_summary_cache:";


    /**
     * 创建并处理AI对话流，每次用户发送提问时的总入口
     * @param sessionId
     * @param question
     * @return
     */
    @Override
    public SseEmitter createChatStream(Long sessionId, String question) {

        String sessionStr = String.valueOf(sessionId);

        //异步处理会话创建与标题生成
        handleSessionTitleAsync(sessionId, question);

        //用户的提问存进数据库
        saveChatMessageAsync(sessionId, "user", question);

        // 创建 SSE 发射器
        SseEmitter emitter = new SseEmitter(0L);

        //将SseEmitter新登记到全局 Manager 中
        SseManager.addEmitter(sessionStr, emitter);

        //把 sessionId 绑定到当前 Tomcat 主线程
        SseManager.bindCurrentSession(sessionStr);
        // 🌟 恢复流式监听逻辑

            chatAgent.chat(sessionId, question)
                    .onNext(token -> {
                        synchronized (emitter) {
                            try {
                                Thread.sleep(30);
                                //流式输出给前端
                                emitter.send(SseEmitter.event().data(token));
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                        }
                    })
                    .onComplete(response -> {
                        //流式输出全部结束时，从 response 中拿到 AI 完整的回答，并保存进数据库
                        String aiFullAnswer = response.content().text();
                        //将ai的回答保存进数据库
                        saveChatMessageAsync(sessionId, "assistant", aiFullAnswer);
                        synchronized (emitter) {
                            // 全部发送完毕
                            emitter.complete();
                        }

                        //回答完毕后，安全移除
                        SseManager.removeEmitter(sessionStr);
                    })
                    .onError(error -> {
                        log.error("流式对话异常", error);
                        emitter.completeWithError(error);
                        SseManager.removeEmitter(sessionStr);
                    })
                    //启动
                    .start();



        return emitter;
    }

    /**
     * 生成企业详情的专属 AI 智能速览
     * @param companyId
     * @return
     */
    @Override
    public SseEmitter createCompanySummaryStream(Long companyId) {

        // 创建 SSE 发射器
        SseEmitter emitter = new SseEmitter(0L);

        // 为速览生成一个独一无二的临时标识，防止跟普通聊天冲突
        String sessionStr = "summary_" + companyId + "_" + System.currentTimeMillis();

        String cacheKey = CACHE_PREFIX + companyId;

        SseManager.addEmitter(sessionStr,emitter);
        SseManager.bindCurrentSession(sessionStr);

        CompletableFuture.runAsync(()->{
            try {
                //先从缓存中检查
                String cachedResult = redisTemplate.opsForValue().get(cacheKey);
                if(StringUtils.isNotBlank(cachedResult)){
                    log.info("企业速览缓存命中: {}", companyId);
                    synchronized (emitter){
                        try {
                            char[] chars = cachedResult.toCharArray();
                            for (char a : chars) {
                                synchronized (emitter) {
                                    emitter.send(SseEmitter.event().data(String.valueOf(a)));
                                    // 缓存读取很快，睡 10-15ms
                                    Thread.sleep(15);
                                }
                            }
                            //物理刹车，保证前端打字机效果匀速
                            Thread.sleep(30);
                            emitter.send(SseEmitter.event().data(cachedResult));
                            emitter.complete();
                        } catch (Exception e) {
                            emitter.completeWithError(e);
                        }finally {
                            SseManager.removeEmitter(sessionStr);
                        }
                    }

                    return ;
                }
                //根据companyId查询详情信息
                EntDetailVO detail = entBasicInfoService.getEnterpriseDetailById(companyId);

                if (detail == null) {
                    emitter.send(SseEmitter.event().data("抱歉，未查询到该企业的详细数据，无法进行 AI 诊断。"));
                    emitter.complete();
                    SseManager.removeEmitter(sessionStr);
                    return ;
                }

                //拼接字符串
                StringBuilder dataBuilder = new StringBuilder();
                dataBuilder.append("【基础档案】\n");
                dataBuilder.append("企业名称：").append(detail.getName()).append("\n");
                dataBuilder.append("法定代表人：").append(detail.getLegalPerson() != null ? detail.getLegalPerson() : "未公开").append("\n");
                dataBuilder.append("注册资本：").append(detail.getRegCapital() != null ? detail.getRegCapital() : "未公开").append("\n");
                dataBuilder.append("成立日期：").append(detail.getEstablishDate() != null ? detail.getEstablishDate() : "未公开").append("\n");
                dataBuilder.append("所属行业：").append(detail.getIndustry() != null ? detail.getIndustry() : "未公开").append("\n");
                dataBuilder.append("企业类型：").append(detail.getBusinessType() != null ? detail.getBusinessType() : "未公开").append("\n");
                dataBuilder.append("人员规模：").append(detail.getStaffNumber() != null ? detail.getStaffNumber() : "未公开").append("\n");

                if (detail.getBusinessScope() != null && !detail.getBusinessScope().isEmpty()) {
                    dataBuilder.append("\n【经营范围】：").append(detail.getBusinessScope()).append("\n");
                }
                if (detail.getBusinessOverview() != null && !detail.getBusinessOverview().isEmpty()) {
                    dataBuilder.append("\n【业务概览】：").append(detail.getBusinessOverview()).append("\n");
                }

                // 核心股东提取（限前3名）
                if (detail.getShareholderList() != null && !detail.getShareholderList().isEmpty()) {
                    dataBuilder.append("\n【主要股东】：");
                    int limit = Math.min(detail.getShareholderList().size(), 3);
                    for (int i = 0; i < limit; i++) {
                        dataBuilder.append(detail.getShareholderList().get(i).getShareholderName()).append("、");
                    }
                    dataBuilder.append("等\n");
                }

                // 专利与资质统计
                if (detail.getPatentList() != null && !detail.getPatentList().isEmpty()) {
                    dataBuilder.append("\n【知识产权】：拥有专利 ").append(detail.getPatentList().size()).append(" 项\n");
                }
                if (detail.getQualificationList() != null && !detail.getQualificationList().isEmpty()) {
                    dataBuilder.append("\n【资质荣誉】：拥有资质 ").append(detail.getQualificationList().size()).append(" 项\n");
                }

                String companyData = dataBuilder.toString();

                //定义一个变量用来累加 AI 吐出的所有 Token，最后存 Redis
                StringBuilder fullAiResponse = new StringBuilder();

                companySummaryAgent.summarize(companyData)
                    .onNext(token->{
                        synchronized (emitter){
                            try {
                                //累加内容
                                fullAiResponse.append(token);
                                // 物理刹车，保证前端打字机效果匀速
                                Thread.sleep(30);
                                emitter.send(SseEmitter.event().data(token));
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                        }
                    })
                        .onComplete(response -> {
                            synchronized (emitter) {
                                String finalContent = fullAiResponse.toString();
                                if (!finalContent.isEmpty()) {
                                    // 设置 24 小时过期时间
                                    redisTemplate.opsForValue().set(cacheKey, finalContent, 24, TimeUnit.HOURS);
                                }
                                emitter.complete(); // 生成完毕
                            }
                            SseManager.removeEmitter(sessionStr);
                        })
                        .onError(error -> {
                            log.error("AI企业速览生成异常", error);
                            synchronized (emitter) {
                                emitter.completeWithError(error);
                            }
                            SseManager.removeEmitter(sessionStr);
                        })
                        .start();

            } catch (IOException e) {
                log.error("准备AI速览数据时发生系统异常", e);
                SseManager.removeEmitter(sessionStr);
            }
        });

        return emitter;
    }

    /**
     * 异步提取并保存具体的聊天气泡
     * @param sessionId
     * @param role
     * @param content
     */
    private void saveChatMessageAsync(Long sessionId, String role, String content){

        CompletableFuture.runAsync(() -> {
            try {
                //保存聊天消息至数据库
                saveMessage(sessionId, role, content);
                log.info("成功保存 {} 的消息到数据库", role);
            } catch (Exception e) {
                log.error("保存消息失败", e);
            }

        });
    }

    /**
     * 保存聊天消息至数据库
     * @param sessionId
     * @param role
     * @param content
     */
    private void saveMessage(Long sessionId, String role, String content) {
        //保存聊天消息至数据库
        EntChatMessage chatMessage = new EntChatMessage();
        chatMessage.setContent(content);
        chatMessage.setSessionId(sessionId);
        chatMessage.setRole(role);
        chatMessage.setCreateTime(LocalDateTime.now());

        entChatMessageMapper.insert(chatMessage);
    }


    /**
     * 异步处理会话创建与标题生成
     * @param sessionId
     * @param question
     */
    private void handleSessionTitleAsync(Long sessionId, String question) {

        EntChatSession session = entChatSessionMapper.selectById(sessionId);

        if(session != null){
            log.info("不是第一次对话，跳过");
            return;
        }

        //获取当前登录的用户id
        Long userId = UserContext.getUserId();

        CompletableFuture.runAsync(() -> {

            try {
                log.info("后台线程开始为会话 {} 生成标题...", sessionId);

                String title = titleGeneratorAgent.generateTitle(question);

                log.info("大模型生成的标题是: {}", title);

                //保存聊天会话到数据库
                saveChatSession(sessionId, title, userId);
            } catch (Exception e) {
                log.error("生成标题失败，使用兜底方案",e);

                String fallbackTitle = question.length() > 10 ? question.substring(0, 10) : question;

                //保存聊天会话到数据库
                saveChatSession(sessionId, fallbackTitle, userId);


            }


        });

    }

    /**
     * 保存会话内容
     * @param sessionId
     * @param title
     * @param userId
     */
    private void saveChatSession(Long sessionId, String title, Long userId) {
        //创建实体类
        EntChatSession chatSession = new EntChatSession();

        chatSession.setTitle(title);
        chatSession.setUserId(userId);
        chatSession.setId(sessionId);
        entChatSessionMapper.insert(chatSession);
    }
}
