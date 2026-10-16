package com.space.cloud.assistantlanggraph.workflow.greeting.nodes;

import com.space.cloud.assistantlanggraph.workflow.greeting.state.GreetingState;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.Map;

/**
 * 看到问候后追加确认。
 */
public class ResponderNode implements NodeAction<GreetingState> {

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> apply(GreetingState state) {
        System.out.println("ResponderNode executing. Current messages: " + state.messages());
        if (state.messages().contains("Hello from GreeterNode!")) {
            return Map.of(GreetingState.MESSAGES_KEY, "Acknowledged greeting!");
        }
        return Map.of(GreetingState.MESSAGES_KEY, "No greeting found.");
    }
}
