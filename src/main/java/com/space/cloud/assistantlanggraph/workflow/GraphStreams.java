package com.space.cloud.assistantlanggraph.workflow;

import org.bsc.async.AsyncGenerator;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphInput;
import org.bsc.langgraph4j.NodeOutput;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.state.AgentState;
import reactor.core.publisher.Flux;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * 把已编译的图转成按节点推送的 SSE 流。订阅之后才开始执行。
 */
public final class GraphStreams {

    private GraphStreams() {
    }

    /**
     * @param graph  已编译的图
     * @param input  初始状态
     * @param toStep 把节点输出转成可序列化的一步结果
     * @param <S>    图状态类型
     * @return 每个节点完成时推送的结果
     */
    public static <S extends AgentState> Flux<Map<String, Object>> stream(
            CompiledGraph<S> graph,
            Map<String, Object> input,
            Function<NodeOutput<S>, Map<String, Object>> toStep) {
        return stream(graph, input, null, null, null, null, toStep);
    }

    /**
     * @param graph          已编译的图
     * @param input          初始状态
     * @param sessionId 养宠助手这一段对话的标识，写入 {@link RunnableConfig#threadId()}；为空时使用空配置。不是登录态
     * @param cancelled      本轮被取消时置为 true；可为空
     * @param register       登记停止动作；可为空
     * @param clear          本轮结束后摘掉登记；可为空
     * @param toStep         把节点输出转成可序列化的一步结果
     * @param <S>            图状态类型
     * @return 每个节点完成时推送的结果
     */
    public static <S extends AgentState> Flux<Map<String, Object>> stream(
            CompiledGraph<S> graph,
            Map<String, Object> input,
            String sessionId,
            AtomicBoolean cancelled,
            Consumer<Runnable> register,
            Consumer<Runnable> clear,
            Function<NodeOutput<S>, Map<String, Object>> toStep) {
        RunnableConfig config = sessionId == null || sessionId.isBlank()
                ? RunnableConfig.empty()
                : RunnableConfig.builder().threadId(sessionId).build();
        AtomicBoolean cancelledFlag = cancelled == null ? new AtomicBoolean() : cancelled;
        return Flux.create(sink -> {
            AsyncGenerator.Cancellable<NodeOutput<S>> generator = graph.stream(GraphInput.args(input), config);
            Runnable stop = () -> {
                if (cancelledFlag.compareAndSet(false, true)) {
                    generator.cancel(true);
                }
            };
            if (register != null) {
                register.accept(stop);
            }
            sink.onCancel(() -> stop.run());
            sink.onDispose(() -> {
                if (clear != null) {
                    clear.accept(stop);
                }
            });
            generator.forEachAsync(item -> sink.next(toStep.apply(item)))
                    .whenComplete((ignored, error) -> {
                        if (cancelledFlag.get() || generator.isCancelled()) {
                            sink.complete();
                            return;
                        }
                        if (error != null) {
                            sink.error(error);
                            return;
                        }
                        sink.complete();
                    });
        });
    }
}
