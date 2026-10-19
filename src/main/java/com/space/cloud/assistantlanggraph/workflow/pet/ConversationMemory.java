package com.space.cloud.assistantlanggraph.workflow.pet;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进程内对话记忆。按 sessionId 保存养宠助手这一段对话，不表示登录态。重启后清空。
 */
@Component
public class ConversationMemory {

    private static final int MAX_TURNS = 20;

    private final Map<String, List<ConversationTurn>> store = new ConcurrentHashMap<>();

    /**
     * 打开已有会话，或在没有标识时新建一个。
     *
     * @param sessionId 养宠助手这一段对话的标识，可为空。不是登录态
     * @return 实际使用的 sessionId
     */
    public String open(String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            store.computeIfAbsent(sessionId.strip(), id -> new ArrayList<>());
            return sessionId.strip();
        }
        String id = UUID.randomUUID().toString();
        store.put(id, new ArrayList<>());
        return id;
    }

    /**
     * @param sessionId 养宠助手这一段对话的标识
     * @return 该段对话的近期快照
     */
    public List<ConversationTurn> history(String sessionId) {
        List<ConversationTurn> turns = store.get(sessionId);
        if (turns == null || turns.isEmpty()) {
            return List.of();
        }
        synchronized (turns) {
            return List.copyOf(turns);
        }
    }

    /**
     * 追加本轮用户原文和助手回复。节点中途的调试句由调用方筛过后再传入。
     *
     * @param sessionId     养宠助手这一段对话的标识
     * @param chatId        一次 SSE 的标识
     * @param userText      本轮用户原文
     * @param assistantText 本轮最后一条助手回复
     */
    public void append(String sessionId, String chatId, String userText, String assistantText) {
        if (sessionId == null || sessionId.isBlank()) {
            return;
        }
        String executionId = chatId == null ? "" : chatId;
        List<ConversationTurn> turns = store.computeIfAbsent(sessionId, id -> new ArrayList<>());
        synchronized (turns) {
            if (userText != null && !userText.isBlank()) {
                turns.add(new ConversationTurn("user", userText, executionId));
            }
            if (assistantText != null && !assistantText.isBlank()) {
                turns.add(new ConversationTurn("assistant", assistantText, executionId));
            }
            while (turns.size() > MAX_TURNS) {
                turns.remove(0);
            }
        }
    }
}
