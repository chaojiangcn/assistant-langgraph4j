package com.space.cloud.assistantlanggraph.controller;

import com.space.cloud.assistantlanggraph.workflow.pet.PetCareAssistant;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 养宠助手工作流接口。
 */
@RestController
@RequestMapping("/ai/pet")
public class PetCareAssistantController {

    private final PetCareAssistant petCareAssistant;

    /**
     * @param petCareAssistant 已编译的养宠助手工作流
     */
    public PetCareAssistantController(PetCareAssistant petCareAssistant) {
        this.petCareAssistant = petCareAssistant;
    }

    /**
     * 以 SSE 逐节点执行养宠助手。服务节点目前返回调试文案。
     *
     * @param message   用户原文，默认 {@code 你好}。空白内容会直接拒绝
     * @param sessionId 养宠助手这一段对话的标识；省略时新建，并在每条事件里返回。不是登录态
     * @return 每个节点完成时推送的结果，含 sessionId 与本次 chatId
     */
    @GetMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Map<String, Object>> chat(
            @RequestParam(value = "message", defaultValue = "你好") String message,
            @RequestParam(value = "sessionId", required = false) String sessionId) {
        if (message.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "消息不能为空");
        }
        return petCareAssistant.stream(message, sessionId);
    }

    /**
     * 取消指定的一次 SSE。对话记录与 sessionId 保留。
     *
     * @param chatId 一次 SSE 的标识，来自事件
     * @return 执行标识，以及是否取消了一次正在执行的工作流
     */
    @GetMapping("/cancel")
    public Map<String, Object> cancel(@RequestParam("chatId") String chatId) {
        if (chatId.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "执行标识不能为空");
        }
        String id = chatId.strip();
        return Map.of("chatId", id, "cancelled", petCareAssistant.cancel(id));
    }
}
