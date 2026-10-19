package com.space.cloud.assistantlanggraph.workflow.pet;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class PetCareAssistantTest {

    private final PetIntentClassifier classifier = new PetIntentClassifier();

    private final ConversationMemory conversationMemory = new ConversationMemory();

    private final PetCareAssistant graphApp = new PetCareAssistant(
            prompt -> {
                String route = classifier.classify(prompt.getUserMessage().getText()).route();
                return new ChatResponse(List.of(new Generation(new AssistantMessage(route))));
            },
            conversationMemory,
            new PetContextRepository()
    );

    @Test
    void routesEachIntentToItsServiceNode() {
        assertRoute("今天带豆豆遛弯半小时", "daily", "[日常记录]");
        assertRoute("猫粮花了128元", "expense", "[花费记录]");
        assertRoute("帮我记一下体重5.2公斤", "health", "[健康记录]");
        assertRoute("狗狗一直吐怎么办", "health_query", "[健康查询]");
        assertFollowUp("你好呀", "chat", "[闲聊]", "[追问]");
        assertFollowUp("皮皮今天吐了一点白色的液体", "clarify", "[意图确认]", "记到健康记录");
    }

    @Test
    void remembersConversationAcrossTurns() {
        PetCareAssistant app = new PetCareAssistant(
                prompt -> new ChatResponse(List.of(new Generation(new AssistantMessage("unknown")))),
                new ConversationMemory(),
                new PetContextRepository()
        );

        List<Map<String, Object>> first = app.stream("皮皮今天吐了一点白色的液体", null)
                .collectList()
                .block(Duration.ofSeconds(5));
        assertThat(first).isNotNull();
        String sessionId = String.valueOf(first.get(0).get("sessionId"));
        assertThat(sessionId).isNotBlank();
        assertThat(first).extracting(step -> step.get("node"))
                .containsExactly("__START__", "load_context", "intent", "clarify", "follow_up", "__END__");
        String firstChatId = String.valueOf(first.get(0).get("chatId"));
        assertThat(firstChatId).isNotBlank();

        List<Map<String, Object>> second = app.stream("帮我记一下", sessionId)
                .collectList()
                .block(Duration.ofSeconds(5));
        assertThat(second).isNotNull();
        assertThat(second).extracting(step -> step.get("node"))
                .containsExactly("__START__", "load_context", "intent", "health", "__END__");
        assertThat(String.valueOf(second.get(0).get("sessionId"))).isEqualTo(sessionId);
        assertThat(String.valueOf(second.get(0).get("chatId"))).isNotBlank().isNotEqualTo(firstChatId);
        assertThat(second.get(3).get("messages").toString())
                .contains("[健康记录]")
                .contains("皮皮，猫，3岁");
    }

    @Test
    void cancellingTheCurrentRunKeepsTheConversation() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        AtomicInteger calls = new AtomicInteger();
        PetCareAssistant app = new PetCareAssistant(prompt -> {
            if (calls.incrementAndGet() != 2) {
                return new ChatResponse(List.of(new Generation(new AssistantMessage("unknown"))));
            }
            started.countDown();
            try {
                if (!new CountDownLatch(1).await(3, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("model was not cancelled");
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new CancellationException("cancelled");
            }
            return new ChatResponse(List.of(new Generation(new AssistantMessage("unknown"))));
        }, new ConversationMemory(), new PetContextRepository());

        List<Map<String, Object>> first = app.stream("皮皮今天吐了一点白色的液体", "conv-keep")
                .collectList()
                .block(Duration.ofSeconds(5));
        assertThat(first).isNotNull();

        AtomicReference<String> runningId = new AtomicReference<>();
        Thread runner = new Thread(() -> {
            List<Map<String, Object>> steps = app.stream("等等再看", "conv-keep")
                    .doOnNext(step -> runningId.compareAndSet(null, String.valueOf(step.get("chatId"))))
                    .collectList()
                    .block(Duration.ofSeconds(5));
            assertThat(steps).isNotNull();
        });
        runner.start();
        assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
        awaitRunId(runningId);
        assertThat(app.cancel(runningId.get())).isTrue();
        runner.join(5000);

        List<Map<String, Object>> next = app.stream("帮我记一下", "conv-keep")
                .collectList()
                .block(Duration.ofSeconds(5));
        assertThat(next).isNotNull();
        assertThat(next).extracting(step -> step.get("node"))
                .containsExactly("__START__", "load_context", "intent", "health", "__END__");
        assertThat(String.valueOf(next.get(0).get("sessionId"))).isEqualTo("conv-keep");
        assertThat(String.valueOf(next.get(0).get("chatId"))).isNotBlank();
    }

    private static void awaitRunId(AtomicReference<String> runningId) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (runningId.get() == null && System.nanoTime() < deadline) {
            Thread.sleep(20);
        }
        assertThat(runningId.get()).isNotBlank();
    }

    private void assertFollowUp(String message, String intent, String servicePrefix, String question) {
        List<Map<String, Object>> steps = graphApp.stream(message, null).collectList().block(Duration.ofSeconds(5));
        assertThat(steps).isNotNull();
        assertThat(steps).extracting(step -> step.get("node"))
                .containsExactly("__START__", "load_context", "intent", intent, "follow_up", "__END__");
        assertThat(steps.get(3).get("messages").toString()).contains(servicePrefix);
        assertThat(steps.get(4).get("intent")).isEqualTo(intent);
        assertThat(steps.get(4).get("messages").toString()).contains(question);
        assertThat(String.valueOf(steps.get(0).get("sessionId"))).isNotBlank();
        assertThat(String.valueOf(steps.get(0).get("chatId"))).isNotBlank();
    }

    private void assertRoute(String message, String node, String replyPrefix) {
        List<Map<String, Object>> steps = graphApp.stream(message, null).collectList().block(Duration.ofSeconds(5));
        assertThat(steps).isNotNull();
        assertThat(steps).extracting(step -> step.get("node"))
                .containsExactly("__START__", "load_context", "intent", node, "__END__");

        Map<String, Object> serviceStep = steps.get(3);
        assertThat(serviceStep.get("intent")).isEqualTo(node);
        assertThat(serviceStep.get("messages").toString()).contains(replyPrefix);
        assertThat(String.valueOf(steps.get(0).get("sessionId"))).isNotBlank();
        assertThat(String.valueOf(steps.get(0).get("chatId"))).isNotBlank();
    }
}
