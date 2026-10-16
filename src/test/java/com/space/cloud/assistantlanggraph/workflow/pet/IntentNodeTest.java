package com.space.cloud.assistantlanggraph.workflow.pet;

import com.space.cloud.assistantlanggraph.workflow.pet.nodes.IntentNode;
import com.space.cloud.assistantlanggraph.workflow.pet.state.PetCareAssistantState;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class IntentNodeTest {

    @Test
    void usesTheLabelReturnedByTheModel() {
        IntentNode node = new IntentNode(reply("health_query"));
        Map<String, Object> update = node.apply(stateOf("豆豆今天胃口不太好"));

        assertThat(update.get(PetCareAssistantState.INTENT_KEY)).isEqualTo(PetIntent.HEALTH_QUERY.route());
    }

    @Test
    void prefersHealthQueryWhenTheLabelContainsHealth() {
        assertThat(PetIntent.fromModelText("health_query")).isEqualTo(PetIntent.HEALTH_QUERY);
        assertThat(PetIntent.fromModelText("标签：健康查询")).isEqualTo(PetIntent.HEALTH_QUERY);
    }

    @Test
    void fallsBackToKeywordsWhenTheModelOutputIsUnknown() {
        IntentNode node = new IntentNode(reply("不知道"));
        Map<String, Object> update = node.apply(stateOf("猫粮花了128元"));

        assertThat(update.get(PetCareAssistantState.INTENT_KEY)).isEqualTo(PetIntent.EXPENSE.route());
    }

    @Test
    void fallsBackToKeywordsWhenTheModelCallFails() {
        ChatModel failingModel = prompt -> {
            throw new IllegalStateException("deepseek unavailable");
        };
        IntentNode node = new IntentNode(failingModel);
        Map<String, Object> update = node.apply(stateOf("今天带豆豆遛弯半小时"));

        assertThat(update.get(PetCareAssistantState.INTENT_KEY)).isEqualTo(PetIntent.DAILY.route());
    }

    @Test
    void listsConfiguredIntentsInThePrompt() {
        AtomicReference<String> systemPrompt = new AtomicReference<>();
        ChatModel model = prompt -> {
            systemPrompt.set(prompt.getSystemMessage().getText());
            return new ChatResponse(List.of(new Generation(new AssistantMessage("chat"))));
        };
        new IntentNode(model).apply(stateOf("你好"));

        assertThat(systemPrompt.get())
                .contains("标签只能是：daily、expense、health、health_query、clarify、chat。")
                .contains("clarify：用户描述了宠物的异常或健康情况，但没有说明是要记录，还是想咨询要不要就医。")
                .contains("当前宠物：皮皮，猫，3岁");
    }

    private static ChatModel reply(String content) {
        return (Prompt prompt) -> new ChatResponse(List.of(new Generation(new AssistantMessage(content))));
    }

    private static PetCareAssistantState stateOf(String message) {
        return new PetCareAssistantState(Map.of(
                PetCareAssistantState.MESSAGES_KEY, List.of(message),
                PetCareAssistantState.PET_PROFILE_KEY, "皮皮，猫，3岁"
        ));
    }
}
