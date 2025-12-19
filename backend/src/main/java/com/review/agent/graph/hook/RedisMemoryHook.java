package com.review.agent.graph.hook;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.agent.hook.HookPosition;
import com.alibaba.cloud.ai.graph.agent.hook.HookPositions;
import com.alibaba.cloud.ai.graph.agent.hook.ModelHook;
import com.alibaba.cloud.ai.graph.store.Store;
import com.alibaba.cloud.ai.graph.store.StoreItem;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@Component
@HookPositions({HookPosition.BEFORE_MODEL, HookPosition.AFTER_MODEL})
public class RedisMemoryHook extends ModelHook {

    @Override
    public String getName() {
        return "";
    }

    @Override
    public CompletableFuture<Map<String, Object>> beforeModel(OverAllState state, RunnableConfig config) {
        @SuppressWarnings("unchecked")
        List<Message> messages = (List<Message>) state.value("messages").orElseThrow(() -> new IllegalArgumentException("message is empty"));

        List<String> namespace = (List<String>) config.metadata("namespace").get();
        Store store = config.store();

        List<Message> contextList = new ArrayList<>();
        Optional<StoreItem> memory = store.getItem(namespace, "a-memory");
        // 第一轮对话，先初始化
        if (memory.isEmpty()) {
            Map<String, Object> metaMap = new HashMap<>();
            contextList.add(messages.get(0));
            metaMap.put("context", contextList);
            StoreItem item = StoreItem.of(namespace, "a-memory", metaMap);

            store.putItem(item);
        } else {
            // 后续对话，从内存中获取历史对话
            StoreItem storeItem = memory.get();
            Map<String, Object> value = storeItem.getValue();
            contextList = (List<Message>) value.get("context");
            // 暂存当前输入
            Message message = messages.get(0);
            // 清空当前输入，用历史对话替换
            messages.clear();
            messages.addAll(contextList);
            // 加入当前输入
            contextList.add(message);
        }
        return CompletableFuture.completedFuture(Map.of("messages", contextList));
    }

    @Override
    public CompletableFuture<Map<String, Object>> afterModel(OverAllState state, RunnableConfig config) {
        // 存入对话
        Object o = state.value("messages").orElseThrow(() -> new IllegalArgumentException("message is empty"));
        List<Message> messages = (List<Message>) o;
        if (messages.isEmpty()) {
            return CompletableFuture.completedFuture(Map.of());
        }
        AssistantMessage assistantMessage = (AssistantMessage) messages.get(messages.size() - 1);
        Store store = config.store();
        List<String> namespace = (List<String>) config.metadata("namespace").get();
        Optional<StoreItem> item = store.getItem(namespace, "a-memory");
        if (item.isPresent()) {
            StoreItem storeItem = item.get();
            Map<String, Object> value = storeItem.getValue();
            List<Message> contextList = (List<Message>) value.get("context");
            contextList.add(assistantMessage);
        }
        return CompletableFuture.completedFuture(Map.of());
    }
}