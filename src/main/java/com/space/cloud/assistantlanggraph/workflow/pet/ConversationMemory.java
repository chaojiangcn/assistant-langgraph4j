package com.space.cloud.assistantlanggraph.workflow.pet;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进程内会话记忆。按 conversationId 保存近期对话，重启后清空。
 */
@Component
public class ConversationMemory {

    private static final int MAX_TURNS = 20;

    private final Map<String, List<ConversationTurn>> store = new ConcurrentHashMap<>();

    /**
     * 打开已有会话，或在没有标识时新建一个。
     *
     * @param conversationId 请求里的会话标识，可为空
     * @return 实际使用的会话标识
     */
    public String open(String conversationId) {
        if (conversationId != null && !conversationId.isBlank()) {
            store.computeIfAbsent(conversationId.strip(), id -> new ArrayList<>());
            return conversationId.strip();
        }
        String id = UUID.randomUUID().toString();
        store.put(id, new ArrayList<>());
        return id;
    }

    /**
     * @param conversationId 会话标识
     * @return 该会话的近期对话快照
     */
    public List<ConversationTurn> history(String conversationId) {
        List<ConversationTurn> turns = store.get(conversationId);
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
     * @param conversationId 会话标识
     * @param userText       本轮用户原文
     * @param assistantText  本轮最后一条助手回复
     */
    public void append(String conversationId, String runId, String userText, String assistantText) {
        if (conversationId == null || conversationId.isBlank()) {
            return;
        }
        String executionId = runId == null ? "" : runId;
        List<ConversationTurn> turns = store.computeIfAbsent(conversationId, id -> new ArrayList<>());
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
