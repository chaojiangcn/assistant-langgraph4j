package com.space.cloud.assistantlanggraph.workflow.pet.nodes;

import com.space.cloud.assistantlanggraph.workflow.pet.state.PetCareAssistantState;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.Map;

/**
 * 宠物花费记录。当前只打印调试文案，不入账。
 */
public class ExpenseRecordNode implements NodeAction<PetCareAssistantState> {

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> apply(PetCareAssistantState state) {
        return PetDebugReply.print("[花费记录] 调试占位：已收到花费信息，尚未入账。", state);
    }
}
