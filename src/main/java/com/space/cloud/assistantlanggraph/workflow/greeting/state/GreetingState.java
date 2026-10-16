package com.space.cloud.assistantlanggraph.workflow.greeting.state;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 问候工作流的图状态。消息按节点追加。
 */
public class GreetingState extends AgentState {

    /** 对话消息列表的状态键。 */
    public static final String MESSAGES_KEY = "messages";

    /**
     * 状态通道。消息追加。
     */
    public static final Map<String, Channel<?>> SCHEMA = Map.of(
            MESSAGES_KEY, Channels.appender(ArrayList::new)
    );

    /**
     * @param initData 初始状态
     */
    public GreetingState(Map<String, Object> initData) {
        super(initData);
    }

    /**
     * @return 当前已追加的消息，没有消息时返回空列表
     */
    public List<String> messages() {
        return this.<List<String>>value(MESSAGES_KEY).orElse(List.of());
    }
}
