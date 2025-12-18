package com.review.agent.service;

import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.agent.hook.modelcalllimit.ModelCallLimitHook;
import com.alibaba.cloud.ai.graph.agent.hook.summarization.SummarizationHook;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.alibaba.cloud.ai.graph.store.stores.MemoryStore;
import com.alibaba.cloud.ai.graph.streaming.StreamingOutput;
import com.review.agent.common.utils.ExceptionUtils;
import com.review.agent.graph.hook.RedisMemoryHook;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.StopWatch;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class ChatService {

    @Resource
    private ChatModel chatModel;
    @Resource
    private PromptService promptService;

    @Resource
    private UserService userService;

    @Resource
    private RedisMemoryHook redisMemoryHook;

    @Resource
    private VectorStoreService vectorStoreService;

    private MemoryStore store = new MemoryStore();

    /**
     * 闲聊 - 添加缓存命中机制
     */
    public Flux<String> chat(Long userId, String request) throws GraphRunnerException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        // 匹配相似问题
        Document matchResult = vectorStoreService.searchOne(request);
        if (matchResult != null) {
            log.info("{} 命中缓存", request);
            String response = matchResult.getMetadata().get("response").toString();
            List<String> fragments = splitContentIntoFragments(response);
            stopWatch.stop();
            log.info("缓存命中耗时: {}s", stopWatch.getTotalTimeSeconds());
            return Flux.fromIterable(fragments)
                    .delayElements(Duration.ofMillis(100));
        }

        // 没有相似问题，调用 LLM
        ReactAgent reactAgent = ReactAgent.builder()
                .name("chat-agent")
                .model(chatModel)
                .systemPrompt("你是一个专业的助手,擅长回答用户的问题")
                .build();
        Flux<NodeOutput> stream = reactAgent.stream(request);

        // 拼接 SSE 响应
        StringBuilder stringBuilder = new StringBuilder();

        return Flux.create(emitter ->
                stream.subscribe(
                        // 处理流式输出
                        output -> {
                            if (output instanceof StreamingOutput<?> streamingOutput) {
                                String chunk = streamingOutput.message().getText();
                                if (chunk != null && !chunk.isEmpty()) {
                                    emitter.next(chunk);
                                    System.out.println(chunk);
                                    stringBuilder.append(chunk);
                                }
                            }
                        },
                        // 处理错误
                        throwable -> ExceptionUtils.throwLLMConnectionFailed(throwable),
                        // sse 完成时添加到向量数据库
                        () -> {
                            Document document = new Document(UUID.randomUUID().toString(), request,
                                    Map.of("response", stringBuilder.toString(),
                                            "userId", userId.toString()));

                            vectorStoreService.addOne(document);
                            log.info("添加文档到向量数据库: {}", request);
                        }
                )
        );
    }

    /**
     * 基于分析结果聊天 - 保存当天的会话作为记忆
     */
    public Flux<String> chatWithAnalysisResult(Long userId, String request) throws GraphRunnerException {

        // 上下文压缩
        SummarizationHook summarizationHook = SummarizationHook.builder()
                .model(chatModel)
                .maxTokensBeforeSummary(4000)
                .messagesToKeep(20)
                .build();
        // 调用限制
        ModelCallLimitHook modelCallLimitHook = ModelCallLimitHook.builder().runLimit(5).build();

        String applicationContext = "chitchat";
        List<String> namespace = List.of(userId.toString(), applicationContext);

        // 在此命名空间内搜索记忆，通过内容等价性过滤，按向量相似度排序
//        List<StoreItem> items = store.searchItems(
//                        StoreSearchRequest.builder()
//                                .namespace(namespace)
//                                .query("my-key")
//                                .build())
//                .getItems();

        String systemPrompt = promptService.getChatPrompt("");
        ReactAgent reactAgent = ReactAgent.builder()
                .name("review-agent")
                .model(chatModel)
                .systemPrompt(systemPrompt)
                .hooks(List.of(redisMemoryHook, summarizationHook, modelCallLimitHook))
                .saver(new MemorySaver())
                .build();

        RunnableConfig config = RunnableConfig.builder()
                .threadId(userId.toString())
                .addMetadata("namespace", namespace)
                .store(store)
                .build();

        Flux<NodeOutput> stream = reactAgent.stream(request, config);

        return Flux.create(emitter ->
                stream.subscribe(
                        // 处理流式输出
                        output -> {
                            if (output instanceof StreamingOutput<?> streamingOutput) {
                                Message message = streamingOutput.message();
                                if (message != null) {
                                    String chunk = message.getText();
                                    if (chunk != null && !chunk.isEmpty()) {
                                        emitter.next(chunk);
                                    }
                                }
                            }
                        },
                        // 处理错误
                        throwable -> log.error("流式输出错误: {}", throwable.getMessage(), throwable),
                        // 完成时添加到记忆中
                        () -> {
                        }
                )
        );
    }

    /**
     * 将缓存内容分解成小片段的方法
     */
    private List<String> splitContentIntoFragments(String content) {
        // 实际应用中需要更复杂的分割逻辑，这里仅做简单示例
        // 比如可以按句号、逗号或固定字符数进行分割
        return Arrays.asList(content.split("(?<=[。！？])|(?<=[.!?])"));
    }
}