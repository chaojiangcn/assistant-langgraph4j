package com.space.cloud.assistantlanggraph.workflow.pet.nodes;

import com.space.cloud.assistantlanggraph.workflow.pet.state.PetCareAssistantState;

import java.util.Map;

/**
 * 把调试文案打印到标准输出，并追加进图状态。
 */
final class PetDebugReply {

    private PetDebugReply() {
    }

    /**
     * @param copy  服务调试文案
     * @param state 当前图状态
     * @return 只追加一条回复的状态更新
     */
    static Map<String, Object> print(String copy, PetCareAssistantState state) {
        String userText = state.messages().isEmpty() ? "" : state.messages().get(0);
        String reply = copy + " 原文：" + userText;
        System.out.println(reply);
        return Map.of(PetCareAssistantState.MESSAGES_KEY, reply);
    }

    /**
     * 健康相关节点附带档案和病例摘要，方便确认上下文已装入。
     *
     * @param copy  服务调试文案
     * @param state 当前图状态
     * @return 只追加一条回复的状态更新
     */
    static Map<String, Object> printWithPetContext(String copy, PetCareAssistantState state) {
        String enriched = copy
                + " 档案：" + state.petProfile()
                + " 病例：" + state.caseSummary();
        return print(enriched, state);
    }
}
