package com.space.cloud.assistantlanggraph.workflow.pet.nodes;

import com.space.cloud.assistantlanggraph.workflow.pet.state.PetCareAssistantState;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.Map;

/**
 * 健康记录和健康咨询都说得通，先标出来，再交给追问节点。
 */
public class ClarifyNode implements NodeAction<PetCareAssistantState> {

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> apply(PetCareAssistantState state) {
        return PetDebugReply.printWithPetContext("[意图确认] 调试占位：健康记录和健康咨询都说得通，需要再确认。", state);
    }
}
