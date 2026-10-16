package com.space.cloud.assistantlanggraph.workflow.pet.nodes;

import com.space.cloud.assistantlanggraph.constant.PetCareIntents;
import com.space.cloud.assistantlanggraph.workflow.pet.ConversationTurn;
import com.space.cloud.assistantlanggraph.workflow.pet.PetIntent;
import com.space.cloud.assistantlanggraph.workflow.pet.PetIntentClassifier;
import com.space.cloud.assistantlanggraph.workflow.pet.state.PetCareAssistantState;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;

/**
 * 用大模型识别用户意图。可选意图来自 {@link PetCareIntents}。模型没有给出有效标签时，改用关键词分类。
 */
@Slf4j
public class IntentNode implements NodeAction<PetCareAssistantState> {

    private final ChatModel chatModel;

    private final PetIntentClassifier fallback = new PetIntentClassifier();

    private final String systemPrompt;

    /**
     * @param chatModel 用于意图分类的聊天模型
     */
    public IntentNode(ChatModel chatModel) {
        this.chatModel = chatModel;
        this.systemPrompt = PetCareIntents.systemPrompt();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> apply(PetCareAssistantState state) {
        String userText = state.messages().isEmpty() ? "" : state.messages().get(0);
        PetIntent intent = recognize(userText, state.history(), state.petProfile());
        String reply = "[意图识别] 已识别为" + intent.label();
        log.info("{} 原文：{}", reply, userText);
        return Map.of(
                PetCareAssistantState.INTENT_KEY, intent.route(),
                PetCareAssistantState.MESSAGES_KEY, reply
        );
    }

    /**
     * 模型调用失败或输出无法对应到配置中的标签时，使用关键词分类。
     *
     * @param userText   用户原文
     * @param history    近期对话
     * @param petProfile 宠物档案摘要
     * @return 识别出的意图
     */
    private PetIntent recognize(String userText, List<ConversationTurn> history, String petProfile) {
        try {
            Prompt prompt = new Prompt(buildMessages(userText, history, petProfile));
            ChatResponse response = chatModel.call(prompt);
            String content = response == null || response.getResult() == null || response.getResult().getOutput() == null
                    ? ""
                    : response.getResult().getOutput().getText();
            PetIntent parsed = PetIntent.fromModelText(content);
            if (parsed != null) {
                return parsed;
            }
            log.warn("LLM 意图无法解析，改用关键词。输出：{}", content);
        } catch (RuntimeException ex) {
            if (Thread.currentThread().isInterrupted() || ex instanceof CancellationException) {
                throw ex;
            }
            log.warn("LLM 意图识别失败，改用关键词", ex);
        }
        return fallback.classify(userText, history);
    }

    /**
     * @param userText   本轮用户原文
     * @param history    近期对话
     * @param petProfile 宠物档案摘要
     * @return 发给模型的消息列表
     */
    private List<Message> buildMessages(String userText, List<ConversationTurn> history, String petProfile) {
        List<Message> messages = new ArrayList<>();
        String system = systemPrompt;
        if (petProfile != null && !petProfile.isBlank()) {
            system = system + "\n当前宠物：" + petProfile;
        }
        messages.add(new SystemMessage(system));
        for (ConversationTurn turn : history) {
            if ("assistant".equals(turn.role())) {
                messages.add(new AssistantMessage(turn.text()));
            } else {
                messages.add(new UserMessage(turn.text()));
            }
        }
        messages.add(new UserMessage(userText));
        return messages;
    }
}
