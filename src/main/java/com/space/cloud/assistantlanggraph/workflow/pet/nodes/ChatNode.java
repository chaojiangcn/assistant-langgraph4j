package com.space.cloud.assistantlanggraph.workflow.pet.nodes;

import com.space.cloud.assistantlanggraph.workflow.pet.state.PetCareAssistantState;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.Map;

/**
 * 闲聊。意图还不明确，当前只打印调试文案，随后进入追问。
 */
public class ChatNode implements NodeAction<PetCareAssistantState> {

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> apply(PetCareAssistantState state) {
        return PetDebugReply.print("[闲聊] 调试占位：这句话还没有明确要记录还是要咨询。", state);
    }
}
