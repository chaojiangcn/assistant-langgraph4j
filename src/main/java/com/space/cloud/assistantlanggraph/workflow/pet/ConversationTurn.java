package com.space.cloud.assistantlanggraph.workflow.pet;

import java.io.Serializable;

/**
 * 一轮对话中的一条发言。
 *
 * @param role  角色，{@code user} 或 {@code assistant}
 * @param text  文本内容
 * @param chatId 产生这条发言的一次 SSE 标识；历史回放时可为空
 */
public record ConversationTurn(String role, String text, String chatId) implements Serializable {

    /**
     * @param role 角色
     * @param text 文本内容
     */
    public ConversationTurn(String role, String text) {
        this(role, text, "");
    }
}
