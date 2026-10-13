package com.space.cloud.assistantlanggraph.workflow;

import com.space.cloud.assistantlanggraph.workflow.nodes.GreeterNode;
import com.space.cloud.assistantlanggraph.workflow.nodes.ResponderNode;
import com.space.cloud.assistantlanggraph.workflow.state.SimpleState;
import org.bsc.async.AsyncGenerator;
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
public class SimpleGraphApp {

    private final CompiledGraph<SimpleState> compiledGraph;

    /**
     * 组装并编译 greeter → responder 工作流。
     */
    public SimpleGraphApp() {
        try {
            GreeterNode greeterNode = new GreeterNode();
            ResponderNode responderNode = new ResponderNode();

            StateGraph<SimpleState> stateGraph = new StateGraph<>(SimpleState.SCHEMA, SimpleState::new)
                    .addNode("greeter", node_async(greeterNode))
                    .addNode("responder", node_async(responderNode))
                    .addEdge(START, "greeter")
                    .addEdge("greeter", "responder")
                    .addEdge("responder", END);
            this.compiledGraph = stateGraph.compile();
        } catch (GraphStateException ex) {
            throw new IllegalStateException("编译简单工作流失败", ex);
        }
    }

    /**
     * 用给定初始消息执行工作流，每个节点完成时立即发出一条结果。
     *
     * @param message 写入状态的初始消息
     * @return 按节点顺序推送的执行结果
     */
    public Flux<Map<String, Object>> stream(String message) {
        return Flux.create(sink -> {
            AsyncGenerator<NodeOutput<SimpleState>> generator =
                    compiledGraph.stream(Map.of(SimpleState.MESSAGES_KEY, message));
            generator.forEachAsync(item -> sink.next(toStep(item)))
                    .whenComplete((ignored, error) -> {
                        if (error != null) {
                            sink.error(error);
                            return;
                        }
                        sink.complete();
                    });
        });
    }

    /**
     * 把节点输出转成可序列化的一步结果，并复制消息列表，避免后续节点追加后改掉已发出的内容。
     *
     * @param item 当前节点输出
     * @return 节点名和该步结束时的消息快照
     */
    private Map<String, Object> toStep(NodeOutput<SimpleState> item) {
        return Map.of(
                "node", item.node(),
                "messages", List.copyOf(item.state().messages())
        );
    }
}
