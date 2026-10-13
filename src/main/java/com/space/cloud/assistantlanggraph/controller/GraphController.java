package com.space.cloud.assistantlanggraph.controller;

import com.space.cloud.assistantlanggraph.workflow.SimpleGraphApp;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

/**
 * 简单工作流接口。
 */
@RestController
@RequestMapping("/ai/graph")
public class GraphController {

    private final SimpleGraphApp simpleGraphApp;

    /**
     * @param simpleGraphApp 已编译的简单工作流
     */
    public GraphController(SimpleGraphApp simpleGraphApp) {
        this.simpleGraphApp = simpleGraphApp;
    }

    /**
     * 以 SSE 逐节点执行问候工作流。
     *
     * @param message 初始消息，默认 {@code Let's, begin!}
     * @return 每个节点完成时推送的结果
     */
    @GetMapping(value = "/run", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Map<String, Object>> run(@RequestParam(value = "message", defaultValue = "Let's, begin!") String message) {
        return simpleGraphApp.stream(message);
    }
}
