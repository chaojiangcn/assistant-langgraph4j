package com.space.cloud.assistantlanggraph.workflow.pet.nodes;

import com.space.cloud.assistantlanggraph.workflow.pet.state.PetCareAssistantState;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.Map;

/**
 * 健康查询。健康资料尚未齐备，只提示线下就医，不提供诊断或用药建议。
 */
public class HealthQueryNode implements NodeAction<PetCareAssistantState> {

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> apply(PetCareAssistantState state) {
        return PetDebugReply.printWithPetContext(
                "[健康查询] 调试占位：健康数据还在逐步完善中，当前不能给出诊断或用药建议。如果宠物出现不适，请尽快到线下宠物医院就医。",
                state
        );
    }
}
