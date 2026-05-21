package com.qych.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class SseManager {
    private static final Map<String, SseEmitter> EMITTER_MAP = new ConcurrentHashMap<>();

    // 🌟 不再单纯依赖 ThreadLocal，因为异步环境下它太不可靠
    private static final InheritableThreadLocal<String> CURRENT_SESSION_ID = new InheritableThreadLocal<>();

    public static void addEmitter(String sessionId, SseEmitter emitter) {
        EMITTER_MAP.put(sessionId, emitter);
    }

    public static void removeEmitter(String sessionId) {
        EMITTER_MAP.remove(sessionId);
    }

    public static void bindCurrentSession(String sessionId) {
        CURRENT_SESSION_ID.set(sessionId);
    }

    public static void clear() {
        CURRENT_SESSION_ID.remove();
    }

    /**
     * 🌟 修复版：手动传入 sessionId 或确保线程安全
     */
    public static void sendToolLog(String logMsg) {
        String sessionId = CURRENT_SESSION_ID.get();
        if (sessionId == null) {
            log.warn("无法获取当前线程的 SessionID，跳过日志发送: {}", logMsg);
            return;
        }

        SseEmitter sseEmitter = EMITTER_MAP.get(sessionId);
        if (sseEmitter != null) {
            // 🌟 核心：必须加锁！保证 SSE 的消息帧是按顺序、完整发送的
            synchronized (sseEmitter) {
                try {
                    sseEmitter.send(SseEmitter.event().data("[TOOL]" + logMsg));
                } catch (IOException e) {
                    EMITTER_MAP.remove(sessionId);
                }
            }
        }
    }
}