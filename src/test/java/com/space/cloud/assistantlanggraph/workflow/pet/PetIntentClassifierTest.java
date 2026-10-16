package com.space.cloud.assistantlanggraph.workflow.pet;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PetIntentClassifierTest {

    private final PetIntentClassifier classifier = new PetIntentClassifier();

    @Test
    void classifiesDailyExpenseHealthChatAndHealthQuery() {
        assertThat(classifier.classify("今天带豆豆遛弯半小时")).isEqualTo(PetIntent.DAILY);
        assertThat(classifier.classify("猫粮花了128元")).isEqualTo(PetIntent.EXPENSE);
        assertThat(classifier.classify("帮我记一下体重5.2公斤")).isEqualTo(PetIntent.HEALTH);
        assertThat(classifier.classify("今天吐了两次，帮我记一下")).isEqualTo(PetIntent.HEALTH);
        assertThat(classifier.classify("你好呀")).isEqualTo(PetIntent.CHAT);
        assertThat(classifier.classify("今天想吐槽一下")).isEqualTo(PetIntent.CHAT);
        assertThat(classifier.classify("狗狗一直吐怎么办")).isEqualTo(PetIntent.HEALTH_QUERY);
        assertThat(classifier.classify("猫能吃巧克力吗")).isEqualTo(PetIntent.HEALTH_QUERY);
        assertThat(classifier.classify("皮皮今天吐了一点白色的液体")).isEqualTo(PetIntent.CLARIFY);
    }

    @Test
    void usesPreviousTurnWhenTheCurrentReplyIsOnlyARecordCue() {
        List<ConversationTurn> history = List.of(
                new ConversationTurn("user", "皮皮今天吐了一点白色的液体"),
                new ConversationTurn("assistant", "[追问] 这是要记到健康记录里，还是想问问要不要去医院看看？")
        );

        assertThat(classifier.classify("帮我记一下", history)).isEqualTo(PetIntent.HEALTH);
        assertThat(classifier.classify("要不要去医院", history)).isEqualTo(PetIntent.HEALTH_QUERY);
    }
}
