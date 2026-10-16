package com.space.cloud.assistantlanggraph.workflow.greeting;

import com.space.cloud.assistantlanggraph.workflow.GraphStreams;
import com.space.cloud.assistantlanggraph.workflow.greeting.nodes.GreeterNode;
import com.space.cloud.assistantlanggraph.workflow.greeting.nodes.ResponderNode;
import com.space.cloud.assistantlanggraph.workflow.greeting.state.GreetingState;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.StateGraph;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 简单问候工作流。图在启动时编译一次，供接口重复执行。
 */
@Component
public class GreetingGraphApp {

    private final CompiledGraph<GreetingState> compiledGraph;

    /**
     * 组装并编译 greeter → responder 工作流。
     */
    public GreetingGraphApp() {
        try {
            StateGraph<GreetingState> stateGraph = new StateGraph<>(GreetingState.SCHEMA, GreetingState::new)
                    .addNode("greeter", node_async(new GreeterNode()))
                    .addNode("responder", node_async(new ResponderNode()))
                    .addEdge(START, "greeter")
                    .addEdge("greeter", "responder")
                    .addEdge("responder", END);
            this.compiledGraph = stateGraph.compile();
        } catch (GraphStateException ex) {
            throw new IllegalStateException("编译问候工作流失败", ex);
        }
    }

    /**
     * 用给定初始消息执行工作流，每个节点完成时立即发出一条结果。
     *
     * @param message 写入状态的初始消息
     * @return 按节点顺序推送的执行结果
     */
    public Flux<Map<String, Object>> stream(String message) {
        return GraphStreams.stream(
                compiledGraph,
                Map.of(GreetingState.MESSAGES_KEY, message),
                GreetingGraphApp::toStep
        );
    }

    /**
     * 复制消息列表，避免后续节点追加后改掉已发出的内容。
     *
     * @param item 当前节点输出
     * @return 节点名和该步结束时的消息快照
     */
    private static Map<String, Object> toStep(NodeOutput<GreetingState> item) {
        return Map.of(
                "node", item.node(),
                "messages", List.copyOf(item.state().messages())
        );
    }
}
