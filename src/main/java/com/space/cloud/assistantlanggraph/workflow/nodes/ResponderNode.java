package com.space.cloud.assistantlanggraph.workflow.nodes;

import com.space.cloud.assistantlanggraph.workflow.state.SimpleState;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.List;
import java.util.Map;

public class ResponderNode implements NodeAction<SimpleState> {
    @Override
    public Map<String, Object> apply(SimpleState state) {
        System.out.println("ResponderNode executing. Current messages: " + state.messages());
        List<String> currentMessages = state.messages();
        if (currentMessages.contains("Hello from GreeterNode!")) {
            return Map.of(SimpleState.MESSAGES_KEY, "Acknowledged greeting!");
        }
        return Map.of(SimpleState.MESSAGES_KEY, "No greeting found.");
    }
}