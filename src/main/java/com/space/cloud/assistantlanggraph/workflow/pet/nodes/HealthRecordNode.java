package com.space.cloud.assistantlanggraph.workflow.pet.nodes;

import com.space.cloud.assistantlanggraph.workflow.pet.state.PetCareAssistantState;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.Map;

/**
 * 宠物健康记录。当前只打印调试文案，健康数据仍在完善中。
 */
public class HealthRecordNode implements NodeAction<PetCareAssistantState> {

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> apply(PetCareAssistantState state) {
        return PetDebugReply.printWithPetContext(
                "[健康记录] 调试占位：已收到健康记录，健康数据仍在完善中，尚未写入。",
                state
        );
    }
}
