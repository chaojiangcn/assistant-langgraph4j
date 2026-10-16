package com.space.cloud.assistantlanggraph.workflow.pet;

import java.util.List;

/**
 * 用关键词识别养宠助手意图，并在大模型不可用时兜底。明确的问诊进入健康查询，明确要求记下的进入健康记录；只描述了异常、却没说要记录还是咨询时，进入意图确认。
 */
public class PetIntentClassifier {

    private static final String[] RECORD_CUES = {"记录", "记一下", "帮我记", "记一笔", "登记"};

    private static final String[] QUESTION_CUES = {
            "吗", "？", "?", "怎么办", "咋办", "为什么", "为啥", "是不是", "能不能", "可不可以",
            "严不严重", "要不要", "查询", "查一下", "咨询", "怎么治"
    };

    private static final String[] EXPENSE_CUES = {"花了", "花费", "费用", "账单", "多少钱", "消费", "价格", "开销"};

    private static final String[] DAILY_CUES = {
            "遛弯", "散步", "吃饭", "吃了", "喂了", "睡觉", "便便", "拉屎", "尿尿", "玩耍", "日常", "喝水", "进食"
    };

    private static final String[] HEALTH_METRIC_CUES = {"体重", "疫苗", "驱虫", "体温", "体检", "打针", "健康记录"};

    private static final String[] SYMPTOM_CUES = {
            "生病", "不舒服", "呕吐", "吐了", "拉稀", "拉肚子", "腹泻", "咳嗽", "发烧", "出血", "抽搐", "没精神", "伤口", "瘙痒", "软便"
    };

    private static final String[] CARE_QUESTION_CUES = {"吃", "喂", "药", "医生", "医院"};

    /**
     * 识别一句话对应的意图。
     *
     * @param message 用户原文，允许为空
     * @return 识别出的意图，空文本返回闲聊
     */
    public PetIntent classify(String message) {
        return classify(message, List.of());
    }

    /**
     * 结合上一轮助手追问识别意图。例如上一轮确认健康记录还是就医后，本轮「帮我记一下」进入健康记录。
     *
     * @param message 用户原文，允许为空
     * @param history 近期对话
     * @return 识别出的意图
     */
    public PetIntent classify(String message, List<ConversationTurn> history) {
        String text = message == null ? "" : message.strip();
        if (text.isEmpty()) {
            return PetIntent.CHAT;
        }

        boolean wantsRecord = containsAny(text, RECORD_CUES);
        boolean asksQuestion = containsAny(text, QUESTION_CUES);
        boolean mentionsSymptom = mentionsSymptom(text);
        boolean mentionsHealthMetric = containsAny(text, HEALTH_METRIC_CUES);

        if (mentionsSymptom && wantsRecord) {
            return PetIntent.HEALTH;
        }
        if (asksQuestion && (mentionsSymptom || mentionsHealthMetric || containsAny(text, CARE_QUESTION_CUES))) {
            return PetIntent.HEALTH_QUERY;
        }
        if (mentionsSymptom) {
            return PetIntent.CLARIFY;
        }
        if (mentionsExpense(text)) {
            return PetIntent.EXPENSE;
        }
        if (mentionsHealthMetric) {
            return PetIntent.HEALTH;
        }
        if (containsAny(text, DAILY_CUES)) {
            return PetIntent.DAILY;
        }

        String lastAssistant = lastText(history, "assistant");
        String previousUser = lastText(history, "user");
        boolean previousSymptom = previousUser != null && mentionsSymptom(previousUser);
        boolean healthFollowUp = lastAssistant != null
                && (lastAssistant.contains("健康记录") || lastAssistant.contains("医院") || lastAssistant.contains("就医"));
        if (previousSymptom || healthFollowUp) {
            if (wantsRecord) {
                return PetIntent.HEALTH;
            }
            if (asksQuestion || text.contains("医院") || text.contains("看看") || text.contains("咨询")) {
                return PetIntent.HEALTH_QUERY;
            }
        }
        return PetIntent.CHAT;
    }

    /**
     * @param history 近期对话
     * @param role    角色
     * @return 该角色最近一条文本，没有则返回 null
     */
    private static String lastText(List<ConversationTurn> history, String role) {
        if (history == null || history.isEmpty()) {
            return null;
        }
        for (int i = history.size() - 1; i >= 0; i--) {
            ConversationTurn turn = history.get(i);
            if (role.equals(turn.role())) {
                return turn.text();
            }
        }
        return null;
    }

    /**
     * 「吐」单独出现也视为症状，「吐槽」除外。
     *
     * @param text 用户原文
     * @return 文本是否在描述症状
     */
    private boolean mentionsSymptom(String text) {
        if (containsAny(text, SYMPTOM_CUES)) {
            return true;
        }
        return text.contains("吐") && !text.contains("吐槽");
    }

    /**
     * @param text 用户原文
     * @return 文本是否在描述一笔花费
     */
    private boolean mentionsExpense(String text) {
        return containsAny(text, EXPENSE_CUES) || text.matches(".*\\d+(?:\\.\\d+)?\\s*(?:元|块钱|块).*");
    }

    /**
     * @param text 用户原文
     * @param cues 待匹配的关键词
     * @return 是否包含任一关键词
     */
    private boolean containsAny(String text, String[] cues) {
        for (String cue : cues) {
            if (text.contains(cue)) {
                return true;
            }
        }
        return false;
    }
}
