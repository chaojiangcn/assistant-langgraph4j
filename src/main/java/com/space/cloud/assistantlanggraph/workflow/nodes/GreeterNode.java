package com.space.cloud.assistantlanggraph.workflow.nodes;

import com.space.cloud.assistantlanggraph.workflow.state.SimpleState;
import org.bsc.langgraph4j.action.NodeAction;
import org.bsc.langgraph4j.state.AgentState;

import java.util.List;
import java.util.Map;

public class GreeterNode  implements NodeAction<SimpleState> {
    @Override
    public Map<String, Object> apply(SimpleState state) throws Exception {
        System.out.println("GreeterNode executing. Current messages: " + state.messages());
        return Map.of(SimpleState.MESSAGES_KEY, "Hello from GreeterNode!");
    }
}