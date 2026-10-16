package com.space.cloud.assistantlanggraph.workflow.pet;

import com.space.cloud.assistantlanggraph.constant.PetCareIntents;

import java.util.Locale;

/**
 * 养宠助手可识别的用户意图。
 */
public enum PetIntent {

    /** 记录吃饭、遛弯等日常。说明见 {@link PetCareIntents}。 */
    DAILY("daily"),

    /** 记录购买、看病等花费。说明见 {@link PetCareIntents}。 */
    EXPENSE("expense"),

    /** 记录体重、疫苗等健康信息。说明见 {@link PetCareIntents}。 */
    HEALTH("health"),

    /** 与宠物相关的闲聊，或还需要追问。说明见 {@link PetCareIntents}。 */
    CHAT("chat"),

    /** 描述了情况，但还不能区分记录和咨询。说明见 {@link PetCareIntents}。 */
    CLARIFY("clarify"),

    /** 询问症状或喂养建议，引导线下就医。说明见 {@link PetCareIntents}。 */
    HEALTH_QUERY("health_query");

    private final String route;

    /**
     * @param route 条件边使用的路由值
     */
    PetIntent(String route) {
        this.route = route;
    }

    /**
     * @return 条件边路由值
     */
    public String route() {
        return route;
    }

    /**
     * @return 配置中的中文意图名称
     */
    public String label() {
        return PetCareIntents.labelOf(route);
    }

    /**
     * 从模型输出里取出第一个意图。{@code health_query} 优先于其中包含的 {@code health}。
     *
     * @param text 模型原文，允许为空
     * @return 匹配到的意图；没有匹配时返回 {@code null}
     */
    public static PetIntent fromModelText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        PetIntent best = null;
        int bestIndex = Integer.MAX_VALUE;
        int bestLength = -1;
        for (PetIntent intent : values()) {
            int routeIndex = lower.indexOf(intent.route());
            if (closer(routeIndex, intent.route().length(), bestIndex, bestLength)) {
                best = intent;
                bestIndex = routeIndex;
                bestLength = intent.route().length();
            }
            int labelIndex = text.indexOf(intent.label());
            if (closer(labelIndex, intent.label().length(), bestIndex, bestLength)) {
                best = intent;
                bestIndex = labelIndex;
                bestLength = intent.label().length();
            }
        }
        return best;
    }

    /**
     * @param index      本次匹配位置，未找到时为负数
     * @param length     本次匹配文本长度
     * @param bestIndex  当前最佳位置
     * @param bestLength 当前最佳长度
     * @return 本次匹配是否更靠前，或同一位置上更长
     */
    private static boolean closer(int index, int length, int bestIndex, int bestLength) {
        if (index < 0) {
            return false;
        }
        if (index < bestIndex) {
            return true;
        }
        return index == bestIndex && length > bestLength;
    }
}
