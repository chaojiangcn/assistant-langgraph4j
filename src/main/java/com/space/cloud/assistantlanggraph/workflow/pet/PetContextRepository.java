package com.space.cloud.assistantlanggraph.workflow.pet;

import org.springframework.stereotype.Component;

/**
 * Mock 宠物档案与病例摘要。后续可替换为真实仓储。
 */
@Component
public class PetContextRepository {

    public static final String DEFAULT_PET_ID = "pipi";

    /**
     * @return 默认宠物皮皮
     */
    public PetProfile defaultPet() {
        return new PetProfile(DEFAULT_PET_ID, "皮皮", "猫", "3岁", "皮皮，猫，3岁，已绝育，对鸡肉过敏");
    }

    /**
     * @param petId 宠物标识
     * @return 近期病例摘要；未知宠物返回空字符串
     */
    public String caseSummary(String petId) {
        if (!DEFAULT_PET_ID.equals(petId)) {
            return "";
        }
        return "近期：上周曾呕吐白色泡沫一次；三月前完成狂犬疫苗；体重 4.8 公斤。";
    }

    /**
     * 宠物档案摘要。
     *
     * @param petId   宠物标识
     * @param name    昵称
     * @param species 物种
     * @param age     年龄
     * @param summary 给人看的一句话摘要
     */
    public record PetProfile(String petId, String name, String species, String age, String summary) {
    }
}
