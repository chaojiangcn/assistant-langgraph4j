package com.space.cloud.assistantlanggraph.constant;

import com.space.cloud.assistantlanggraph.workflow.pet.PetIntent;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 养宠助手的意图定义。调整后，意图提示词和工作流路由会一起变化。
 */
public final class PetCareIntents {

    /** 意图含糊时进入的追问节点。 */
    public static final String FOLLOW_UP = "follow_up";

    /** 按提示词中的顺序排列。{@code next} 为空表示该意图的服务做完即结束。 */
    public static final List<Definition> ALL = List.of(
            new Definition("daily", "日常记录", "记录吃饭、遛弯、睡觉、排便等日常。", ""),
            new Definition("expense", "花费记录", "记录购买、账单、花费。", ""),
            new Definition("health", "健康记录", "用户明确要求记录体重、疫苗、驱虫、体温，或明确要求记下已经发生的健康情况。", ""),
            new Definition("health_query", "健康查询", "用户在询问症状、能否喂食、是否生病或是否需要就医。", ""),
            new Definition("clarify", "意图确认", "用户描述了宠物的异常或健康情况，但没有说明是要记录，还是想咨询要不要就医。", FOLLOW_UP),
            new Definition("chat", "闲聊", "闲聊，或还看不出用户想记录还是咨询，需要继续追问。", FOLLOW_UP)
    );

    static {
        Set<String> configured = ALL.stream().map(Definition::route).collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> available = Arrays.stream(PetIntent.values())
                .map(PetIntent::route)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (configured.size() != ALL.size() || !configured.equals(available)) {
            throw new IllegalStateException("宠物意图常量与可用路由不一致：" + configured);
        }
    }

    private PetCareIntents() {
    }

    /**
     * @return 意图路由到同名服务节点
     */
    public static Map<String, String> routes() {
        Map<String, String> routes = new LinkedHashMap<>();
        for (Definition intent : ALL) {
            routes.put(intent.route(), intent.route());
        }
        return Map.copyOf(routes);
    }

    /**
     * @param route 路由值
     * @return 中文名称
     */
    public static String labelOf(String route) {
        for (Definition intent : ALL) {
            if (intent.route().equals(route)) {
                return intent.label();
            }
        }
        throw new IllegalArgumentException("未定义的宠物意图：" + route);
    }

    /**
     * @return 写给模型的意图说明
     */
    public static String systemPrompt() {
        String routes = ALL.stream().map(Definition::route).collect(Collectors.joining("、"));
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是养宠助手的意图分类器。根据用户的一句话，只输出一个标签，不要解释，不要加标点。\n");
        prompt.append("标签只能是：").append(routes).append("。\n");
        for (Definition intent : ALL) {
            prompt.append(intent.route()).append("：").append(intent.description()).append("\n");
        }
        return prompt.toString().strip();
    }

    /**
     * 一条意图定义。
     *
     * @param route       条件边和模型标签
     * @param label       展示名称
     * @param description 写给模型的说明
     * @param next        服务节点之后的下一个节点，空字符串表示结束
     */
    public record Definition(String route, String label, String description, String next) {

        /**
         * @return 该意图完成后是否还要继续追问
         */
        public boolean continues() {
            return next != null && !next.isBlank();
        }
    }
}
