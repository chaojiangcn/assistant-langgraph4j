package com.space.cloud.assistantlanggraph.workflow;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 按 runId 登记正在执行的工作流。同一 conversationId 再开新一轮时，会先停掉上一轮。
 */
public final class ActiveRuns {

    private final ConcurrentHashMap<String, Runnable> byRunId = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, String> runIdByConversation = new ConcurrentHashMap<>();

    /**
     * @param conversationId 会话标识
     * @param runId          本轮执行标识
     * @param stop           停止本轮
     */
    public void register(String conversationId, String runId, Runnable stop) {
        String previousRunId = runIdByConversation.put(conversationId, runId);
        if (previousRunId != null && !previousRunId.equals(runId)) {
            Runnable previous = byRunId.remove(previousRunId);
            if (previous != null) {
                previous.run();
            }
        }
        Runnable previous = byRunId.put(runId, stop);
        if (previous != null && previous != stop) {
            previous.run();
        }
    }

    /**
     * @param conversationId 会话标识
     * @param runId          本轮执行标识
     * @param stop           本轮登记的停止动作
     */
    public void clear(String conversationId, String runId, Runnable stop) {
        byRunId.remove(runId, stop);
        runIdByConversation.remove(conversationId, runId);
    }

    /**
     * 按本轮执行标识取消。会话记录保留。
     *
     * @param runId 本轮执行标识
     * @return 是否取消了一轮正在执行的工作流
     */
    public boolean cancel(String runId) {
        if (runId == null || runId.isBlank()) {
            return false;
        }
        String id = runId.strip();
        Runnable stop = byRunId.remove(id);
        if (stop == null) {
            return false;
        }
        runIdByConversation.entrySet().removeIf(entry -> id.equals(entry.getValue()));
        stop.run();
        return true;
    }
}
