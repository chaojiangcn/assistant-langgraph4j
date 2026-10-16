package com.space.cloud.assistantlanggraph.workflow.greeting.nodes;

import com.space.cloud.assistantlanggraph.workflow.greeting.state.GreetingState;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.Map;

/**
 * 向消息列表追加一条问候。
 */
public class GreeterNode implements NodeAction<GreetingState> {

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> apply(GreetingState state) {
        System.out.println("GreeterNode executing. Current messages: " + state.messages());
        return Map.of(GreetingState.MESSAGES_KEY, "Hello from GreeterNode!");
    }
}
