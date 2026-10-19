package com.space.cloud.assistantlanggraph.workflow;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 按 chatId 登记正在执行的工作流。同一 sessionId 再开一次 SSE 时，会先停掉上一次。
 * sessionId 是养宠助手这一段对话，不是登录态。
 */
public final class ActiveRuns {

    private final ConcurrentHashMap<String, Runnable> byChatId = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, String> chatIdBySession = new ConcurrentHashMap<>();

    /**
     * @param sessionId 养宠助手这一段对话的标识
     * @param chatId    一次 SSE 的标识
     * @param stop      停止这一次 SSE
     */
    public void register(String sessionId, String chatId, Runnable stop) {
        String previousChatId = chatIdBySession.put(sessionId, chatId);
        if (previousChatId != null && !previousChatId.equals(chatId)) {
            Runnable previous = byChatId.remove(previousChatId);
            if (previous != null) {
                previous.run();
            }
        }
        Runnable previous = byChatId.put(chatId, stop);
        if (previous != null && previous != stop) {
            previous.run();
        }
    }

    /**
     * @param sessionId 养宠助手这一段对话的标识
     * @param chatId    一次 SSE 的标识
     * @param stop      这一次登记的停止动作
     */
    public void clear(String sessionId, String chatId, Runnable stop) {
        byChatId.remove(chatId, stop);
        chatIdBySession.remove(sessionId, chatId);
    }

    /**
     * 按这一次 SSE 的标识取消。对话记录保留。
     *
     * @param chatId 一次 SSE 的标识
     * @return 是否取消了一次正在执行的工作流
     */
    public boolean cancel(String chatId) {
        if (chatId == null || chatId.isBlank()) {
            return false;
        }
        String id = chatId.strip();
        Runnable stop = byChatId.remove(id);
        if (stop == null) {
            return false;
        }
        chatIdBySession.entrySet().removeIf(entry -> id.equals(entry.getValue()));
        stop.run();
        return true;
    }
}
