package com.review.agent.graph.nodes;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 数据收集节点 收集本地文件
 */
@Slf4j
@Component
public class DataCollectorNode implements NodeAction {

    @Override
    public Map<String, Object> apply(OverAllState state) {
        log.info("======DataCollectorNode apply start======");

        return Map.of();
    }


}
