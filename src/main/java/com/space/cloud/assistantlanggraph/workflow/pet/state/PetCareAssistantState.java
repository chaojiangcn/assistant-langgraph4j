package com.space.cloud.assistantlanggraph.workflow.pet.state;

import com.space.cloud.assistantlanggraph.workflow.pet.ConversationTurn;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 养宠助手的图状态。消息按节点追加，意图与会话上下文整份覆盖。
 */
public class PetCareAssistantState extends AgentState {

    /** 对话消息列表的状态键。 */
    public static final String MESSAGES_KEY = "messages";

    /** 意图路由值的状态键。 */
    public static final String INTENT_KEY = "intent";

    /** 养宠助手这一段对话的标识，不是登录态。 */
    public static final String SESSION_ID_KEY = "sessionId";

    /** 一次 SSE 执行的标识。 */
    public static final String CHAT_ID_KEY = "chatId";

    /** 近期对话历史的状态键。 */
    public static final String HISTORY_KEY = "history";

    /** 宠物档案摘要的状态键。 */
    public static final String PET_PROFILE_KEY = "petProfile";

    /** 病例摘要的状态键。 */
    public static final String CASE_SUMMARY_KEY = "caseSummary";

    /**
     * 状态通道。消息追加，其余字段覆盖。
     */
    public static final Map<String, Channel<?>> SCHEMA;

    static {
        Map<String, Channel<?>> schema = new HashMap<>();
        schema.put(MESSAGES_KEY, Channels.appender(ArrayList::new));
        schema.put(INTENT_KEY, Channels.base(() -> ""));
        schema.put(SESSION_ID_KEY, Channels.base(() -> ""));
        schema.put(CHAT_ID_KEY, Channels.base(() -> ""));
        schema.put(HISTORY_KEY, Channels.base(() -> List.of()));
        schema.put(PET_PROFILE_KEY, Channels.base(() -> ""));
        schema.put(CASE_SUMMARY_KEY, Channels.base(() -> ""));
        SCHEMA = Map.copyOf(schema);
    }

    /**
     * @param initData 初始状态
     */
    public PetCareAssistantState(Map<String, Object> initData) {
        super(initData);
    }

    /**
     * @return 当前已追加的消息，没有消息时返回空列表
     */
    public List<String> messages() {
        return this.<List<String>>value(MESSAGES_KEY).orElse(List.of());
    }

    /**
     * @return 已识别的意图路由值，尚未识别时返回空字符串
     */
    public String intent() {
        return this.<String>value(INTENT_KEY).orElse("");
    }

    /**
     * @return 养宠助手这一段对话的标识，不是登录态
     */
    public String sessionId() {
        return this.<String>value(SESSION_ID_KEY).orElse("");
    }

    /**
     * @return 一次 SSE 执行的标识
     */
    public String chatId() {
        return this.<String>value(CHAT_ID_KEY).orElse("");
    }

    /**
     * @return 近期对话历史
     */
    public List<ConversationTurn> history() {
        return this.<List<ConversationTurn>>value(HISTORY_KEY).orElse(List.of());
    }

    /**
     * @return 宠物档案摘要
     */
    public String petProfile() {
        return this.<String>value(PET_PROFILE_KEY).orElse("");
    }

    /**
     * @return 病例摘要
     */
    public String caseSummary() {
        return this.<String>value(CASE_SUMMARY_KEY).orElse("");
    }
}
