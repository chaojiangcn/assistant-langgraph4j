package com.space.cloud.assistantlanggraph.workflow.pet.nodes;

import com.space.cloud.assistantlanggraph.workflow.pet.ConversationMemory;
import com.space.cloud.assistantlanggraph.workflow.pet.PetContextRepository;
import com.space.cloud.assistantlanggraph.workflow.pet.PetContextRepository.PetProfile;
import com.space.cloud.assistantlanggraph.workflow.pet.state.PetCareAssistantState;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.Map;

/**
 * 在意图识别前装入会话历史和 mock 宠物档案。
 */
public class LoadContextNode implements NodeAction<PetCareAssistantState> {

    private final ConversationMemory conversationMemory;

    private final PetContextRepository petContextRepository;

    /**
     * @param conversationMemory    进程内会话
     * @param petContextRepository  mock 宠物档案
     */
    public LoadContextNode(ConversationMemory conversationMemory, PetContextRepository petContextRepository) {
        this.conversationMemory = conversationMemory;
        this.petContextRepository = petContextRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> apply(PetCareAssistantState state) {
        PetProfile pet = petContextRepository.defaultPet();
        return Map.of(
                PetCareAssistantState.HISTORY_KEY, conversationMemory.history(state.conversationId()),
                PetCareAssistantState.PET_PROFILE_KEY, pet.summary(),
                PetCareAssistantState.CASE_SUMMARY_KEY, petContextRepository.caseSummary(pet.petId())
        );
    }
}
