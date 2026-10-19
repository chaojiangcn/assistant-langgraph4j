package com.space.cloud.assistantlanggraph.workflow.pet;

import com.space.cloud.assistantlanggraph.constant.PetCareIntents;
import com.space.cloud.assistantlanggraph.workflow.ActiveRuns;
import com.space.cloud.assistantlanggraph.workflow.GraphStreams;
import com.space.cloud.assistantlanggraph.workflow.pet.nodes.ChatNode;
import com.space.cloud.assistantlanggraph.workflow.pet.nodes.ClarifyNode;
import com.space.cloud.assistantlanggraph.workflow.pet.nodes.DailyRecordNode;
import com.space.cloud.assistantlanggraph.workflow.pet.nodes.ExpenseRecordNode;
import com.space.cloud.assistantlanggraph.workflow.pet.nodes.FollowUpNode;
import com.space.cloud.assistantlanggraph.workflow.pet.nodes.HealthQueryNode;
import com.space.cloud.assistantlanggraph.workflow.pet.nodes.HealthRecordNode;
import com.space.cloud.assistantlanggraph.workflow.pet.nodes.IntentNode;
import com.space.cloud.assistantlanggraph.workflow.pet.nodes.LoadContextNode;
import com.space.cloud.assistantlanggraph.workflow.pet.state.PetCareAssistantState;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.NodeAction;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 养宠助手工作流。
 * <p>
 * 先装入会话与档案，再识别意图。日常、花费、健康记录、健康查询做完即结束。
 * 闲聊和意图确认进入追问，确认用户是要记录还是要咨询。
 */
@Component
public class PetCareAssistant {

    private static final String LOAD_CONTEXT = "load_context";

    private static final String INTENT = "intent";

    private static final Map<String, Supplier<NodeAction<PetCareAssistantState>>> SERVICES = Map.of(
            "daily", DailyRecordNode::new,
            "expense", ExpenseRecordNode::new,
            "health", HealthRecordNode::new,
            "health_query", HealthQueryNode::new,
            "clarify", ClarifyNode::new,
            "chat", ChatNode::new
    );

    private final CompiledGraph<PetCareAssistantState> compiledGraph;

    private final ConversationMemory conversationMemory;

    private final ActiveRuns activeRuns = new ActiveRuns();

    public PetCareAssistant(
            ChatModel chatModel,
            ConversationMemory conversationMemory,
            PetContextRepository petContextRepository) {
        this.conversationMemory = conversationMemory;
        this.compiledGraph = compile(chatModel, conversationMemory, petContextRepository);
    }

    /**
     * @param message        用户原文
     * @param sessionId 养宠助手这一段对话的标识，可为空；为空时新建。不是登录态
     * @return 按节点顺序推送的执行结果
     */
    public Flux<Map<String, Object>> stream(String message, String sessionId) {
        String id = conversationMemory.open(sessionId);
        String chatId = UUID.randomUUID().toString();
        Map<String, Object> input = new HashMap<>();
        input.put(PetCareAssistantState.MESSAGES_KEY, message);
        input.put(PetCareAssistantState.SESSION_ID_KEY, id);
        input.put(PetCareAssistantState.CHAT_ID_KEY, chatId);

        AtomicReference<String> lastAssistantReply = new AtomicReference<>("");
        AtomicBoolean cancelled = new AtomicBoolean();
        return GraphStreams.stream(
                        compiledGraph,
                        input,
                        id,
                        cancelled,
                        stop -> activeRuns.register(id, chatId, stop),
                        stop -> activeRuns.clear(id, chatId, stop),
                        PetCareAssistant::toStep)
                .doOnNext(step -> rememberAssistantReply(step, lastAssistantReply))
                .doOnComplete(() -> {
                    if (!cancelled.get()) {
                        conversationMemory.append(id, chatId, message, lastAssistantReply.get());
                    }
                });
    }

    /**
     * 按这一次 SSE 的标识取消。对话记录保留，之后仍用同一 sessionId 继续。
     *
     * @param chatId 一次 SSE 的标识
     * @return 是否取消了一次正在执行的工作流
     */
    public boolean cancel(String chatId) {
        return activeRuns.cancel(chatId);
    }

    private CompiledGraph<PetCareAssistantState> compile(
            ChatModel chatModel,
            ConversationMemory conversationMemory,
            PetContextRepository petContextRepository) {
        try {
            StateGraph<PetCareAssistantState> graph = new StateGraph<>(PetCareAssistantState.SCHEMA, PetCareAssistantState::new)
                    .addNode(LOAD_CONTEXT, node_async(new LoadContextNode(conversationMemory, petContextRepository)))
                    .addNode(INTENT, node_async(new IntentNode(chatModel)))
                    .addNode(PetCareIntents.FOLLOW_UP, node_async(new FollowUpNode()))
                    .addEdge(START, LOAD_CONTEXT)
                    .addEdge(LOAD_CONTEXT, INTENT)
                    .addEdge(PetCareIntents.FOLLOW_UP, END)
                    .addConditionalEdges(INTENT, edge_async(PetCareAssistantState::intent), PetCareIntents.routes());
            for (PetCareIntents.Definition intent : PetCareIntents.ALL) {
                graph.addNode(intent.route(), node_async(service(intent.route())))
                        .addEdge(intent.route(), intent.continues() ? intent.next() : END);
            }
            return graph.compile();
        } catch (GraphStateException ex) {
            throw new IllegalStateException("编译养宠助手工作流失败", ex);
        }
    }

    private static NodeAction<PetCareAssistantState> service(String route) {
        Supplier<NodeAction<PetCareAssistantState>> node = SERVICES.get(route);
        if (node == null) {
            throw new IllegalStateException("没有对应的养宠服务节点：" + route);
        }
        return node.get();
    }

    private static Map<String, Object> toStep(NodeOutput<PetCareAssistantState> item) {
        return Map.of(
                "node", item.node(),
                "intent", item.state().intent(),
                "sessionId", item.state().sessionId(),
                "chatId", item.state().chatId(),
                "messages", List.copyOf(item.state().messages())
        );
    }

    /**
     * 记下本轮最后一条面向用户的助手回复，跳过意图识别调试句。
     */
    private static void rememberAssistantReply(Map<String, Object> step, AtomicReference<String> lastAssistantReply) {
        Object messages = step.get("messages");
        if (!(messages instanceof List<?> list) || list.isEmpty()) {
            return;
        }
        Object last = list.get(list.size() - 1);
        if (last == null) {
            return;
        }
        String text = last.toString();
        if (text.startsWith("[意图识别]")) {
            return;
        }
        lastAssistantReply.set(text);
    }
}
