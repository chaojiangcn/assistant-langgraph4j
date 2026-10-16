package com.space.cloud.assistantlanggraph.workflow.pet.nodes;

import com.space.cloud.assistantlanggraph.workflow.pet.PetIntent;
import com.space.cloud.assistantlanggraph.workflow.pet.state.PetCareAssistantState;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.Map;

/**
 * 意图还不明确时继续追问。当前只打印调试文案。
 */
public class FollowUpNode implements NodeAction<PetCareAssistantState> {

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> apply(PetCareAssistantState state) {
        String question = PetIntent.CLARIFY.route().equals(state.intent())
                ? "[追问] 调试占位：这是要记到健康记录里，还是想问问要不要去医院看看？"
                : "[追问] 调试占位：还不太确定你想做什么。是要记录日常、花费或健康情况，还是想咨询健康问题？";
        return PetDebugReply.print(question, state);
    }
}
