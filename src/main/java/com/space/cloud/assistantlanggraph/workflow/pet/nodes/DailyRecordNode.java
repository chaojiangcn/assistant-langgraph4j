package com.space.cloud.assistantlanggraph.workflow.pet.nodes;

import com.space.cloud.assistantlanggraph.workflow.pet.state.PetCareAssistantState;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.Map;

/**
 * 宠物日常记录。当前只打印调试文案，不写入存储。
 */
public class DailyRecordNode implements NodeAction<PetCareAssistantState> {

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> apply(PetCareAssistantState state) {
        return PetDebugReply.print("[日常记录] 调试占位：已收到宠物日常，尚未写入记录。", state);
    }
}
